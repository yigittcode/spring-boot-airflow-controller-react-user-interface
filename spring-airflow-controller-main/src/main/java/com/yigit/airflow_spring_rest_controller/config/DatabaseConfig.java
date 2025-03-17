package com.yigit.airflow_spring_rest_controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import io.r2dbc.spi.ConnectionFactory;

/**
 * R2DBC configuration for PostgreSQL
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "com.yigit.airflow_spring_rest_controller.repository")
@EnableR2dbcAuditing
@EnableTransactionManagement
public class DatabaseConfig {

    /**
     * Configure the reactive transaction manager
     * 
     * @param connectionFactory the R2DBC connection factory
     * @return a reactive transaction manager
     */
    @Bean
    ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
} 