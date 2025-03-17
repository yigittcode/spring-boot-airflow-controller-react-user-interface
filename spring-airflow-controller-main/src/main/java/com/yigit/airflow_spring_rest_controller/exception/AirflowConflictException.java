package com.yigit.airflow_spring_rest_controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a conflict occurs with Airflow resources
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AirflowConflictException extends RuntimeException {

    public AirflowConflictException(String message) {
        super(message);
    }

    public AirflowConflictException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AirflowConflictException(String resourceType, String resourceId, String action) {
        super(String.format("Cannot %s %s with ID '%s' due to a conflict", action, resourceType, resourceId));
    }
} 