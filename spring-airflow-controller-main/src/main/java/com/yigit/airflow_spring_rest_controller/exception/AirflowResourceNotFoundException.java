package com.yigit.airflow_spring_rest_controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested Airflow resource is not found
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AirflowResourceNotFoundException extends RuntimeException {

    public AirflowResourceNotFoundException(String message) {
        super(message);
    }

    public AirflowResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AirflowResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with ID '%s' not found", resourceType, resourceId));
    }
} 