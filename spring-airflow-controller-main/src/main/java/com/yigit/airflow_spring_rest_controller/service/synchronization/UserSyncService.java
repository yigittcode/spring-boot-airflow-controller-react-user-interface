package com.yigit.airflow_spring_rest_controller.service.synchronization;

import com.yigit.airflow_spring_rest_controller.entity.User;
import com.yigit.airflow_spring_rest_controller.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for synchronizing Keycloak users to the local database
 */
@Service
public class UserSyncService {
    private static final Logger log = LoggerFactory.getLogger(UserSyncService.class);
    
    private final String tokenEndpoint;
    private final String usersEndpoint;
    private final String adminUsername;
    private final String adminPassword;
    
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final R2dbcEntityTemplate entityTemplate;
    
    @Autowired
    public UserSyncService(
            UserRepository userRepository, 
            R2dbcEntityTemplate entityTemplate,
            @Value("${keycloak.auth-server-url}") String keycloakBaseUrl,
            @Value("${keycloak.sync.endpoints.token}") String tokenEndpoint,
            @Value("${keycloak.sync.endpoints.users}") String usersEndpoint,
            @Value("${keycloak.sync.admin.username}") String adminUsername,
            @Value("${keycloak.sync.admin.password}") String adminPassword) {
        
        this.userRepository = userRepository;
        this.entityTemplate = entityTemplate;
        this.tokenEndpoint = tokenEndpoint;
        this.usersEndpoint = usersEndpoint;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        
        this.webClient = WebClient.builder()
                .baseUrl(keycloakBaseUrl)
                .build();
    }
    
    /**
     * Synchronizes users from Keycloak to the local database
     * 
     * @return Mono<Integer> the number of users synchronized
     */
    public Mono<Integer> syncUsers() {
        log.info("Starting Keycloak user synchronization");
        
        // Track counts of new and skipped users
        AtomicInteger newUsersCount = new AtomicInteger(0);
        AtomicInteger skippedUsersCount = new AtomicInteger(0);
        
        return getAdminToken()
                .flatMapMany(this::getUsersFromKeycloak)
                .flatMap(user -> saveUserToDatabase(user, newUsersCount, skippedUsersCount))
                .collectList()
                .map(List::size)
                .doOnSuccess(count -> log.info("Successfully synchronized {} users from Keycloak (New: {}, Skipped: {})", 
                        count, newUsersCount.get(), skippedUsersCount.get()))
                .doOnError(error -> log.error("Error synchronizing users from Keycloak", error));
    }
    
    /**
     * Gets an admin token from Keycloak
     * 
     * @return Mono<String> the admin token
     */
    private Mono<String> getAdminToken() {
        log.debug("Getting admin token from Keycloak");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", "admin-cli");
        formData.add("username", adminUsername);
        formData.add("password", adminPassword);
        
        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("access_token"))
                .doOnSuccess(token -> log.debug("Successfully obtained admin token"))
                .doOnError(error -> log.error("Error getting admin token", error));
    }
    
    /**
     * Gets users from Keycloak using the admin token
     * 
     * @param adminToken the admin token
     * @return Flux<User> the users from Keycloak
     */
    private Flux<User> getUsersFromKeycloak(String adminToken) {
        log.debug("Getting users from Keycloak");
        
        return webClient.get()
                .uri(usersEndpoint)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToFlux(Map.class)
                .map(this::mapToUser)
                .doOnComplete(() -> log.debug("Successfully retrieved users from Keycloak"))
                .doOnError(error -> log.error("Error getting users from Keycloak", error));
    }
    
    /**
     * Maps a Keycloak user JSON to a User entity
     * 
     * @param userMap the Keycloak user JSON as a Map
     * @return User the User entity
     */
    private User mapToUser(Map<String, Object> userMap) {
        return User.builder()
                .id((String) userMap.get("id"))
                .createdTimestamp(Long.valueOf(userMap.get("createdTimestamp").toString()))
                .username((String) userMap.get("username"))
                .enabled((Boolean) userMap.get("enabled"))
                .totp((Boolean) userMap.get("totp"))
                .emailVerified((Boolean) userMap.get("emailVerified"))
                .firstName((String) userMap.get("firstName"))
                .lastName((String) userMap.get("lastName"))
                .email((String) userMap.get("email"))
                .build();
    }
    
    /**
     * Saves a user to the database only if it doesn't already exist
     * No updates are performed on existing users
     * 
     * @param user the user to save
     * @param newUsersCount counter for new users
     * @param skippedUsersCount counter for skipped users
     * @return Mono<User> the saved user or existing user
     */
    private Mono<User> saveUserToDatabase(User user, AtomicInteger newUsersCount, AtomicInteger skippedUsersCount) {
        log.debug("Processing user for database: {}", user.getUsername());
        
        // First check if the user already exists
        return userRepository.findById(user.getId())
            .flatMap(existingUser -> {
                // User already exists, do nothing
                log.info("SKIP: User '{}' (id: {}) already exists, skipping", user.getUsername(), user.getId());
                skippedUsersCount.incrementAndGet();
                return Mono.just(user);
            })
            .switchIfEmpty(Mono.defer(() -> {
                // Insert new user with entityTemplate
                log.info("INSERT: Adding new user '{}' (id: {})", user.getUsername(), user.getId());
                return userRepository.save(user)
                    .doOnSuccess(savedUser -> {
                        log.info("SUCCESS: Added user '{}' to database", savedUser.getUsername());
                        newUsersCount.incrementAndGet();
                    })
                    .doOnError(error -> log.error("ERROR: Failed to insert user {}: {}", user.getUsername(), error.getMessage()));
            }));
    }
} 