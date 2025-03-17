package com.yigit.airflow_spring_rest_controller.service;

import com.yigit.airflow_spring_rest_controller.entity.AuditLog;
import com.yigit.airflow_spring_rest_controller.entity.AuditLogOperation;
import com.yigit.airflow_spring_rest_controller.repository.AuditLogRepository;
import com.yigit.airflow_spring_rest_controller.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Service for managing audit logs
 */
@Service
public class AuditLogService {
    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Creates an audit log entry for a DAG operation
     * 
     * @param dagId the DAG ID
     * @param operation the operation type as an enum
     * @param details additional details (optional)
     * @return Mono<AuditLog> the created audit log
     */
    public Mono<AuditLog> logOperation(String dagId, AuditLogOperation operation, String details) {
        // Null kontrolleri
        if (dagId == null || dagId.trim().isEmpty()) {
            log.error("Cannot log operation: DAG ID is null or empty");
            return Mono.error(new IllegalArgumentException("DAG ID cannot be null or empty"));
        }
        
        if (operation == null) {
            log.error("Cannot log operation: Operation type is null");
            return Mono.error(new IllegalArgumentException("Operation type cannot be null"));
        }
        
        log.debug("Starting to log operation: {} for DAG: {}", operation, dagId);
        return logOperation(dagId, null, operation, details);
    }
    
    /**
     * Creates an audit log entry for a DAG run operation
     * 
     * @param dagId the DAG ID
     * @param dagRunId the DAG run ID
     * @param operation the operation type as an enum
     * @param details additional details (optional)
     * @return Mono<AuditLog> the created audit log
     */
    public Mono<AuditLog> logOperation(String dagId, String dagRunId, AuditLogOperation operation, String details) {
        // Null kontrolleri
        if (dagId == null || dagId.trim().isEmpty()) {
            log.error("Cannot log operation: DAG ID is null or empty");
            return Mono.error(new IllegalArgumentException("DAG ID cannot be null or empty"));
        }
        
        if (operation == null) {
            log.error("Cannot log operation: Operation type is null");
            return Mono.error(new IllegalArgumentException("Operation type cannot be null"));
        }
        
        log.debug("Starting to log operation: {} for DAG: {}, DAG run: {}", operation, dagId, dagRunId);
        return getCurrentUserFromToken()
            .doOnSuccess(jwt -> log.debug("Successfully retrieved current user JWT"))
            .doOnError(e -> log.error("Failed to retrieve current user JWT: {}", e.getMessage()))
            .flatMap(jwt -> {
                String userId = jwt.getClaimAsString("sub");
                String username = jwt.getClaimAsString("preferred_username");
                
                if (userId == null || userId.trim().isEmpty()) {
                    log.error("Cannot log operation: User ID could not be extracted from JWT");
                    return Mono.error(new IllegalStateException("User ID could not be extracted from JWT"));
                }
                
                log.debug("Extracted user info from JWT: userId={}, username={}", userId, username);
                
                return createAuditLog(userId, username != null ? username : "unknown", dagId, dagRunId, operation, details);
            })
            .doOnSuccess(auditLog -> log.info("Successfully logged operation: {} for DAG: {}, DAG run: {}, log ID: {}", 
                operation, dagId, dagRunId, auditLog.getId()))
            .doOnError(e -> log.error("Failed to log operation: {} for DAG: {}, DAG run: {}, error: {}", 
                operation, dagId, dagRunId, e.getMessage()));
    }
    
    /**
     * Get audit logs with role-based access control:
     * - Admins can see all logs
     * - Users can only see their own logs
     * 
     * @return Flux<AuditLog> filtered audit logs
     */
    public Flux<AuditLog> getAuditLogs() {
        return getCurrentUserFromToken()
            .flatMapMany(jwt -> {
                String userId = jwt.getClaimAsString("sub");
                boolean isAdmin = hasAdminRole(jwt);
                
                if (isAdmin) {
                    log.info("Admin user requesting all audit logs");
                    return auditLogRepository.findAll();
                } else {
                    log.info("Regular user requesting their own audit logs");
                    return auditLogRepository.findByUserId(userId);
                }
            });
    }
    
    /**
     * Get audit logs for a specific DAG with role-based access control
     * 
     * @param dagId the DAG ID
     * @return Flux<AuditLog> filtered audit logs
     */
    public Flux<AuditLog> getAuditLogsForDag(String dagId) {
        return getCurrentUserFromToken()
            .flatMapMany(jwt -> {
                String userId = jwt.getClaimAsString("sub");
                boolean isAdmin = hasAdminRole(jwt);
                
                if (isAdmin) {
                    return auditLogRepository.findByDagId(dagId);
                } else {
                    return auditLogRepository.findByUserIdAndDagId(userId, dagId);
                }
            });
    }
    
    /**
     * Check if a user has admin role
     * 
     * @param jwt the JWT token
     * @return boolean true if user has admin role
     */
    private boolean hasAdminRole(org.springframework.security.oauth2.jwt.Jwt jwt) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                return roles != null && roles.contains("airflow-admin");
            }
        } catch (Exception e) {
            log.warn("Error checking admin role: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Get the current user from the security context
     * 
     * @return Mono<Jwt> the JWT token
     */
    private Mono<org.springframework.security.oauth2.jwt.Jwt> getCurrentUserFromToken() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(auth -> {
                if (auth instanceof JwtAuthenticationToken) {
                    return ((JwtAuthenticationToken) auth).getToken();
                }
                throw new IllegalStateException("User not authenticated with JWT token");
            });
    }
    
    /**
     * Create and save an audit log entry
     */
    private Mono<AuditLog> createAuditLog(String userId, String username, String dagId, String dagRunId, 
                                         AuditLogOperation operation, String details) {
        log.debug("Creating audit log entry: user={}, dag={}, dagRun={}, operation={}", 
            username, dagId, dagRunId, operation);
            
        AuditLog auditLog = AuditLog.builder()
            .userId(userId)
            .username(username)
            .dagId(dagId)
            .dagRunId(dagRunId)
            .operation(operation)
            .operationTime(LocalDateTime.now())
            .details(details)
            .build();
            
        return auditLogRepository.save(auditLog)
            .doOnSuccess(saved -> log.info("Created audit log: user={}, dag={}, operation={}, id={}", 
                username, dagId, operation, saved.getId()))
            .doOnError(e -> log.error("Failed to save audit log: user={}, dag={}, operation={}, error={}", 
                username, dagId, operation, e.getMessage()));
    }
    
    /**
     * Get audit logs for a specific operation with role-based access control
     * 
     * @param operation the operation type
     * @return Flux<AuditLog> filtered audit logs
     */
    public Flux<AuditLog> getAuditLogsByOperation(AuditLogOperation operation) {
        log.debug("Getting audit logs for operation: {}", operation);
        return getCurrentUserFromToken()
            .flatMapMany(jwt -> {
                String userId = jwt.getClaimAsString("sub");
                boolean isAdmin = hasAdminRole(jwt);
                
                if (isAdmin) {
                    log.info("Admin user requesting all audit logs for operation: {}", operation);
                    return auditLogRepository.findByOperation(operation);
                } else {
                    log.info("Regular user requesting their own audit logs for operation: {}", operation);
                    return Flux.from(auditLogRepository.findByOperation(operation))
                        .filter(log -> log.getUserId().equals(userId));
                }
            });
    }
    
    /**
     * Get audit logs for a specific operation and DAG with role-based access control
     * 
     * @param operation the operation type
     * @param dagId the DAG ID
     * @return Flux<AuditLog> filtered audit logs
     */
    public Flux<AuditLog> getAuditLogsByOperationAndDagId(AuditLogOperation operation, String dagId) {
        log.debug("Getting audit logs for operation: {} and DAG: {}", operation, dagId);
        return getCurrentUserFromToken()
            .flatMapMany(jwt -> {
                String userId = jwt.getClaimAsString("sub");
                boolean isAdmin = hasAdminRole(jwt);
                
                // Get logs for the specified operation and DAG ID
                Flux<AuditLog> operationLogs = Flux.from(auditLogRepository.findByOperation(operation))
                    .filter(log -> log.getDagId().equals(dagId));
                
                if (isAdmin) {
                    log.info("Admin user requesting all audit logs for operation: {} and DAG: {}", operation, dagId);
                    return operationLogs;
                } else {
                    log.info("Regular user requesting their own audit logs for operation: {} and DAG: {}", operation, dagId);
                    return operationLogs.filter(log -> log.getUserId().equals(userId));
                }
            });
    }
} 