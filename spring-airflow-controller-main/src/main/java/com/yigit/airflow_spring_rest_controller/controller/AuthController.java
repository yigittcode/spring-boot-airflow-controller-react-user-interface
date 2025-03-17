package com.yigit.airflow_spring_rest_controller.controller;

import com.yigit.airflow_spring_rest_controller.dto.auth.AuthResponse;
import com.yigit.airflow_spring_rest_controller.dto.auth.LoginRequest;
import com.yigit.airflow_spring_rest_controller.dto.auth.TokenResponse;
import com.yigit.airflow_spring_rest_controller.exception.AuthenticationException;
import com.yigit.airflow_spring_rest_controller.service.KeycloakService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller providing authentication API endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Operations related to user authentication")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final KeycloakService keycloakService;

    @Autowired
    public AuthController(KeycloakService keycloakService) {
        this.keycloakService = keycloakService;
    }

    /**
     * Kullanıcı giriş işlemi yapar ve token döndürür
     *
     * @param loginRequest Kullanıcı adı ve şifre bilgilerini içeren istek
     * @return Access ve refresh token'ları içeren yanıt
     */
    @Operation(
        summary = "Login to Application",
        description = "Authenticates a user with Keycloak and returns access and refresh tokens"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        return keycloakService.login(loginRequest.getUsername(), loginRequest.getPassword())
            .doOnSuccess(token -> logger.info("Login successful for user: {}", loginRequest.getUsername()))
            .doOnError(error -> {
                // Simple error message without stack trace
                if (error instanceof AuthenticationException) {
                    logger.warn("Login failed: Invalid credentials");
                } else {
                    logger.warn("Login failed: {}", error.getMessage());
                }
            });
    }

    /**
     * Refresh token kullanarak yeni access token alır
     *
     * @param refreshToken Yenileme token'ı
     * @return Yeni access ve refresh token'ları içeren yanıt
     */
    @Operation(
        summary = "Refresh Token",
        description = "Refreshes an expired access token using a refresh token"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token refresh successful"),
        @ApiResponse(responseCode = "401", description = "Invalid refresh token")
    })
    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public Mono<TokenResponse> refreshToken(@RequestBody String refreshToken) {
        logger.info("Token refresh attempt");
        return keycloakService.refreshToken(refreshToken)
            .doOnSuccess(token -> logger.info("Token refresh successful"))
            .doOnError(error -> logger.error("Token refresh failed", error));
    }

    /**
     * Kullanıcı kimlik doğrulamasını kontrol eder
     *
     * @param jwt JWT token
     * @return Doğrulama sonucu
     */
    @Operation(
        summary = "Verify User Credentials",
        description = "Endpoint to verify user authentication. If the request reaches this endpoint, it implies successful authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication successful"),
        @ApiResponse(responseCode = "401", description = "Authentication failed (handled by Spring Security)")
    })
    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public Mono<AuthResponse> verifyCredentials(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        logger.info("Authentication verified for user: {}", username);
        return Mono.just(new AuthResponse("Authentication successful for " + username));
    }
} 