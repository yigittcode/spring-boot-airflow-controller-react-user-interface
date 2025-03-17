package com.yigit.airflow_spring_rest_controller.controller;

import com.yigit.airflow_spring_rest_controller.entity.AuditLog;
import com.yigit.airflow_spring_rest_controller.entity.AuditLogOperation;
import com.yigit.airflow_spring_rest_controller.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Controller for Audit Log operations
 * Provides endpoints to view audit logs with role-based access control
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Operations for viewing audit logs")
public class AuditLogController {
    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);
    
    private final AuditLogService auditLogService;
    
    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
    
    /**
     * Get all audit logs (role-based)
     * - Admin users can see all logs
     * - Regular users can only see their own logs
     */
    @Operation(
        summary = "Get audit logs",
        description = "Retrieves audit logs with role-based filtering. Admin users can see all logs, while regular users can only see their own logs."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires authentication")
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<AuditLog> getAuditLogs() {
        log.info("Request received to get audit logs");
        return auditLogService.getAuditLogs();
    }
    
    /**
     * Get audit logs for a specific DAG (role-based)
     */
    @Operation(
        summary = "Get audit logs for a DAG",
        description = "Retrieves audit logs for a specific DAG with role-based filtering."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires authentication")
    })
    @GetMapping("/dag/{dagId}")
    @ResponseStatus(HttpStatus.OK)
    public Flux<AuditLog> getAuditLogsForDag(@PathVariable String dagId) {
        log.info("Request received to get audit logs for DAG: {}", dagId);
        return auditLogService.getAuditLogsForDag(dagId);
    }
    
    /**
     * Get all DELETE operation audit logs (role-based)
     */
    @Operation(
        summary = "Get DELETE operation audit logs",
        description = "Retrieves audit logs specifically for DELETE operations with role-based filtering."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved delete audit logs"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires authentication")
    })
    @GetMapping("/operations/delete")
    @ResponseStatus(HttpStatus.OK)
    public Flux<AuditLog> getDeleteAuditLogs() {
        log.info("Request received to get DELETE operation audit logs");
        return auditLogService.getAuditLogsByOperation(AuditLogOperation.DELETE);
    }
    
    /**
     * Get all DELETE operation audit logs for a specific DAG (role-based)
     */
    @Operation(
        summary = "Get DELETE operation audit logs for a DAG",
        description = "Retrieves audit logs specifically for DELETE operations on a specific DAG with role-based filtering."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved delete audit logs for a DAG"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - requires authentication")
    })
    @GetMapping("/operations/delete/dag/{dagId}")
    @ResponseStatus(HttpStatus.OK)
    public Flux<AuditLog> getDeleteAuditLogsForDag(@PathVariable String dagId) {
        log.info("Request received to get DELETE operation audit logs for DAG: {}", dagId);
        return auditLogService.getAuditLogsByOperationAndDagId(AuditLogOperation.DELETE, dagId);
    }
} 