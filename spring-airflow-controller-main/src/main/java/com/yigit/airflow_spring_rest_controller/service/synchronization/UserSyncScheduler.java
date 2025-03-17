package com.yigit.airflow_spring_rest_controller.service.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that periodically synchronizes Keycloak users
 * Only active when not in test profile
 */
@Component
@EnableScheduling
@Profile("!test")
public class UserSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(UserSyncScheduler.class);
    
    private final UserSyncService userSyncService;
    
    @Autowired
    public UserSyncScheduler(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }
    
    /**
     * Synchronize users every 15 minutes (can be configured to a different value)
     * Initial delay of 60 seconds to allow application to fully start up
     */
    @Scheduled(fixedRateString = "${keycloak.sync.interval:900000}", initialDelayString = "${keycloak.sync.initial-delay:60000}")
    public void scheduledUserSync() {
        log.info("Starting scheduled Keycloak user synchronization");
        
        userSyncService.syncUsers()
            .subscribe(
                count -> log.info("Scheduled user synchronization completed, synchronized {} users", count),
                error -> log.error("Error during scheduled user synchronization: {}", error.getMessage())
            );
    }
} 