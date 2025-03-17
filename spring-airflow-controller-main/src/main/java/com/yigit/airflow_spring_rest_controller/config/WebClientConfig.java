package com.yigit.airflow_spring_rest_controller.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.Base64;

@Configuration
public class WebClientConfig {

    private static final String API_VERSION = "/api/v1";
    private static final int MAX_MEMORY_SIZE = 16 * 1024 * 1024; // 16MB

    @Value("${airflow.api.base-url}")
    private String baseUrl;

    @Value("${airflow.api.username}")
    private String username;

    @Value("${airflow.api.password}")
    private String password;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public WebClient airflowWebClient() {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        // Özel JSON kodlama/çözümleme stratejileri
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(MAX_MEMORY_SIZE);
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                })
                .build();

        // HTTP istemcisini özelleştir
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .wiretap(true);

        return WebClient.builder()
                .baseUrl(baseUrl + API_VERSION)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .exchangeStrategies(exchangeStrategies)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
} 