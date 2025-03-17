package com.yigit.airflow_spring_rest_controller.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux configuration.
 * 
 * This class configures how JSON encoding/decoding settings are applied for all HTTP responses.
 * It ensures that ZonedDateTime objects are properly serialized in ISO 8601 format.
 */
@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

    private final ObjectMapper objectMapper;
    
    @Autowired
    public WebFluxConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        // Replace the default Jackson encoder/decoder with our custom ObjectMapper
        configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(objectMapper));
        
        configurer.defaultCodecs().jackson2JsonDecoder(
            new Jackson2JsonDecoder(objectMapper));
    }
} 