package com.yigit.airflow_spring_rest_controller.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import com.yigit.airflow_spring_rest_controller.dto.error.ErrorResponse;
import com.yigit.airflow_spring_rest_controller.exception.AuthenticationException;

/**
 * Global exception handler for the application
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AirflowResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<ErrorResponse> handleResourceNotFoundException(AirflowResourceNotFoundException ex) {
        return Mono.just(new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource Not Found",
            ex.getMessage()
        ));
    }

    @ExceptionHandler(AirflowConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Mono<ErrorResponse> handleConflictException(AirflowConflictException ex) {
        return Mono.just(new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            ex.getMessage()
        ));
    }

    /**
     * Handles WebClient exceptions, primarily used for Keycloak authentication errors
     */
    @ExceptionHandler(WebClientResponseException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ErrorResponse> handleWebClientResponseException(WebClientResponseException ex) {
        // Minimal logging for common authentication errors
        logger.debug("Authentication client error: {}", ex.getStatusCode());
        
        String errorMessage = "Authentication Failed";
        String errorDetail = "Please check your username and password";
        
        // Parse the error if it's from Keycloak
        if (ex.getResponseBodyAsString().contains("invalid_grant")) {
            errorDetail = "Invalid username or password";
        } else if (ex.getResponseBodyAsString().contains("unauthorized_client")) {
            errorDetail = "Invalid client credentials";
            // Log configuration issues at a higher level
            logger.warn("Keycloak client configuration issue detected");
        }
        
        return Mono.just(new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            errorMessage,
            errorDetail
        ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        logger.error("Response status exception: {}", ex.getMessage());
        
        return Mono.just(new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getReason(),
            "Operation could not be completed"
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Mono<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        logger.error("Access denied: {}", ex.getMessage());
        
        return Mono.just(new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Access Denied",
            "You don't have permission to access this resource"
        ));
    }

    /**
     * Handles custom authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Mono<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        // Only log a brief message without the stack trace
        if (ex.getMessage().contains("Invalid username or password")) {
            logger.debug("Authentication error: Invalid credentials");
        } else {
            logger.debug("Authentication error: {}", ex.getMessage());
        }
        
        return Mono.just(new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication Error",
            ex.getMessage()
        ));
    }

    /**
     * Handles RuntimeExceptions and customizes error messages for common cases
     */
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRuntimeException(RuntimeException ex) {
        // Only log a detailed message for non-auth errors
        if (ex.getMessage() != null && 
           (ex.getMessage().contains("login failed") || 
            ex.getMessage().contains("token refresh failed") ||
            ex instanceof AuthenticationException)) {
            // For auth errors, just debug level
            logger.debug("Auth error: {}", ex.getMessage());
        } else {
            // For other errors, info level without stack trace
            logger.info("Runtime error: {}", ex.getMessage());
        }
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Server Error";
        String detail = "Your request could not be processed";
        
        // Customize error messages for known error patterns
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("login failed") || ex.getMessage().contains("token refresh failed")) {
                status = HttpStatus.UNAUTHORIZED;
                message = "Authentication Error";
                detail = "Login failed. Please check your credentials.";
            } else if (ex.getMessage().contains("No connection to")) {
                detail = "Service connection failed. Please try again later.";
            }
        }
        
        ErrorResponse error = new ErrorResponse(
            status.value(),
            message,
            detail
        );
        
        return Mono.just(ResponseEntity.status(status).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleException(Exception ex) {
        // Log the full stack trace for debugging
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Unexpected Error",
            "Your request could not be processed. Please try again later."
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
} 