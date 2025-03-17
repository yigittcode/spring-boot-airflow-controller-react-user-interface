package com.yigit.airflow_spring_rest_controller.service.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Startup runner that synchronizes Keycloak users when the application starts
 * Only active when not in test profile
 */
@Component
@Profile("!test")
public class UserSyncStartupRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(UserSyncStartupRunner.class);
    
    private final UserSyncService userSyncService;
    
    @Autowired
    public UserSyncStartupRunner(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }
    
    @Override
    public void run(String... args) {
        log.info("Triggering initial Keycloak user synchronization on startup");
        
        userSyncService.syncUsers()
            .onErrorResume(error -> {
                log.error("Initial user synchronization failed: {}", error.getMessage());
                return Mono.just(0);
            })
            .subscribe(
                count -> log.info("Initial user synchronization completed, synchronized {} users", count),
                error -> log.error("Unexpected error during initial user synchronization", error)
            );
    }
} 