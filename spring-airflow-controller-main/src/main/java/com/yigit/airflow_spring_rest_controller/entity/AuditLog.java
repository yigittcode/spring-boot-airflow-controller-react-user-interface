package com.yigit.airflow_spring_rest_controller.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking DAG operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_logs")
public class AuditLog {
    
    @Id
    private Long id;
    
    @Column("user_id")
    private String userId; // Foreign key to User entity
    
    @Column("username")
    private String username;
    
    @Column("dag_id")
    private String dagId;
    
    @Column("dag_run_id")
    private String dagRunId; // Can be null for operations that don't involve specific runs
    
    @Column("operation")
    private AuditLogOperation operation; // Enum: PAUSE, UNPAUSE, DELETE, TRIGGER, CLEAR, UPDATE_STATE
    
    @Column("operation_time")
    private LocalDateTime operationTime;
    
    @Column("details")
    private String details; // Additional information about the operation if needed
} 