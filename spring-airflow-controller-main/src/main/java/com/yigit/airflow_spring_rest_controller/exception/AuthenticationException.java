package com.yigit.airflow_spring_rest_controller.exception;

/**
 * Exception thrown when authentication fails
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
} 