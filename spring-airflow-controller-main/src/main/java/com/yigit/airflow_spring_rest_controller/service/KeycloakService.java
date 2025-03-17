package com.yigit.airflow_spring_rest_controller.service;

import com.yigit.airflow_spring_rest_controller.dto.auth.KeycloakTokenResponse;
import com.yigit.airflow_spring_rest_controller.dto.auth.TokenResponse;
import com.yigit.airflow_spring_rest_controller.exception.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Keycloak authentication service.
 * This service handles user authentication and token refresh operations.
 */
@Service
public class KeycloakService {
    private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);
    private static final String TOKEN_ENDPOINT_PATH = "/realms/%s/protocol/openid-connect/token";

    private final WebClient webClient;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    /**
     * Configures the Keycloak service
     */
    public KeycloakService(
            @Value("${keycloak.auth-server-url}") String authServerUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.resource}") String clientId,
            @Value("${keycloak.credentials.secret}") String clientSecret) {
        
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        
        this.webClient = WebClient.builder()
                .baseUrl(authServerUrl)
                .build();
        
        logger.info("KeycloakService initialized with realm: {}, clientId: {}", realm, clientId);
    }

    /**
     * Authenticates a user with username and password
     */
    public Mono<TokenResponse> login(String username, String password) {
        logger.info("Attempting login for user: {}", username);
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", username);
        formData.add("password", password);

        String tokenUri = String.format(TOKEN_ENDPOINT_PATH, realm);
        
        return requestToken(tokenUri, formData, "login")
                .doOnSuccess(token -> logger.info("Successfully received token for user: {}", username));
    }

    /**
     * Refreshes token using a refresh token
     */
    public Mono<TokenResponse> refreshToken(String refreshToken) {
        logger.info("Attempting to refresh token");
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        String tokenUri = String.format(TOKEN_ENDPOINT_PATH, realm);
        
        return requestToken(tokenUri, formData, "token refresh")
                .doOnSuccess(response -> logger.info("Successfully refreshed token"));
    }
    
    /**
     * Sends a token request to Keycloak API
     */
    private Mono<TokenResponse> requestToken(String tokenUri, MultiValueMap<String, String> formData, String operation) {
        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> {
                    // Minimal logging for authentication errors
                    if (response.statusCode().equals(HttpStatus.UNAUTHORIZED)) {
                        logger.debug("{} failed: unauthorized", operation);
                    } else {
                        logger.warn("{} failed with status: {}", operation, response.statusCode());
                    }
                    
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                // Debug level logging for error response details
                                logger.debug("Error details: {}", body);
                                
                                // Suitable error message
                                String errorMessage = operation + " failed: " + response.statusCode();
                                
                                // If error message is in JSON format and contains error_description
                                if (body.contains("error_description")) {
                                    try {
                                        // Simple extraction of error_description value
                                        int startIndex = body.indexOf("error_description") + "error_description".length() + 3;
                                        int endIndex = body.indexOf("\"", startIndex);
                                        if (startIndex > 0 && endIndex > startIndex) {
                                            String description = body.substring(startIndex, endIndex);
                                            
                                            // More user-friendly error messages
                                            if (description.contains("Invalid user credentials")) {
                                                errorMessage = "Invalid username or password";
                                            } else if (description.contains("Invalid client")) {
                                                errorMessage = "Authentication system error: Invalid client";
                                            } else {
                                                errorMessage = description;
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.debug("Error parsing error_description from response");
                                    }
                                }
                                
                                return Mono.error(new AuthenticationException(errorMessage));
                            });
                })
                .bodyToMono(KeycloakTokenResponse.class)
                .map(this::convertToTokenResponse)
                .onErrorResume(ex -> {
                    if (ex instanceof WebClientResponseException) {
                        // No need to log details for WebClientResponseException
                        // Already handled above
                    } else if (!(ex instanceof AuthenticationException)) {
                        // Only log non-authentication errors
                        logger.warn("Error during {}: {}", operation, ex.getMessage());
                    }
                    return Mono.error(ex);
                });
    }

    /**
     * Converts Keycloak token response to client TokenResponse
     */
    private TokenResponse convertToTokenResponse(KeycloakTokenResponse keycloakTokenResponse) {
        return new TokenResponse(
                keycloakTokenResponse.getAccessToken(),
                keycloakTokenResponse.getRefreshToken(),
                keycloakTokenResponse.getExpiresIn(),
                keycloakTokenResponse.getTokenType()
        );
    }
} 