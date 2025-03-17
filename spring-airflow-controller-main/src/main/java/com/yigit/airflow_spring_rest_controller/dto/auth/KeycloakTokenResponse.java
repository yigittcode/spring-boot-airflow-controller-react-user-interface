package com.yigit.airflow_spring_rest_controller.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO for Keycloak token response
 */
@Data
public class KeycloakTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("expires_in")
    private int expiresIn;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("scope")
    private String scope;

    @Override
    public String toString() {
        return "KeycloakTokenResponse{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresIn=" + expiresIn +
                ", scope='" + scope + '\'' +
                ", accessToken='[PROTECTED]'" +
                ", refreshToken='[PROTECTED]'" +
                '}';
    }
} 