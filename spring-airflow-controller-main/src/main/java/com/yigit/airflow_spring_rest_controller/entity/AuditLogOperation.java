package com.yigit.airflow_spring_rest_controller.entity;

/**
 * Defines possible operations for audit logging
 */
public enum AuditLogOperation {
    PAUSE,
    UNPAUSE,
    DELETE,
    TRIGGER,
    CLEAR,
    UPDATE_STATE
} 