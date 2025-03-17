package com.yigit.airflow_spring_rest_controller.repository;

import com.yigit.airflow_spring_rest_controller.entity.AuditLog;
import com.yigit.airflow_spring_rest_controller.entity.AuditLogOperation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive repository for AuditLog entity
 */
@Repository
public interface AuditLogRepository extends ReactiveCrudRepository<AuditLog, Long> {
    
    /**
     * Find all audit logs by user ID
     * @param userId the user ID to search for
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByUserId(String userId);
    
    /**
     * Find all audit logs by username
     * @param username the username to search for
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByUsername(String username);
    
    /**
     * Find all audit logs for a specific DAG
     * @param dagId the DAG ID to search for
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByDagId(String dagId);
    
    /**
     * Find all audit logs for a specific DAG run
     * @param dagRunId the DAG run ID to search for
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByDagRunId(String dagRunId);
    
    /**
     * Find all audit logs for a specific operation
     * @param operation the operation to search for
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByOperation(AuditLogOperation operation);
    
    /**
     * Find all audit logs between two dates
     * @param startDate the start date
     * @param endDate the end date
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByOperationTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find all audit logs for a specific user and DAG
     * @param userId the user ID
     * @param dagId the DAG ID
     * @return a Flux of audit logs
     */
    Flux<AuditLog> findByUserIdAndDagId(String userId, String dagId);
    
    /**
     * Count number of operations by type for a specific user
     * @param userId the user ID
     * @param operation the operation type
     * @return a Mono with the count
     */
    Mono<Long> countByUserIdAndOperation(String userId, AuditLogOperation operation);
    
    /**
     * Find all DELETE operations for a specific DAG
     * @param dagId the DAG ID
     * @return a Flux of audit logs
     */
    @Query("SELECT * FROM audit_logs WHERE operation = 'DELETE' AND dag_id = :dagId ORDER BY operation_time DESC")
    Flux<AuditLog> findDeleteOperationsForDag(String dagId);
    
    /**
     * Count number of operations by type
     * @param operation the operation type
     * @return a Mono with the count
     */
    Mono<Long> countByOperation(AuditLogOperation operation);
} 