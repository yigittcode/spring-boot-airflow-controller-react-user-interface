package com.yigit.airflow_spring_rest_controller.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User entity representing Keycloak users in the application database
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users") // table name
public class User {
    
    @Id
    private String id; // Keycloak UUID
    
    @Column("created_timestamp")
    private Long createdTimestamp;
    
    private String username;
    
    private Boolean enabled;
    
    private Boolean totp;
    
    @Column("email_verified")
    private Boolean emailVerified;
    
    @Column("first_name")
    private String firstName;
    
    @Column("last_name")
    private String lastName;
    
    private String email;
} 