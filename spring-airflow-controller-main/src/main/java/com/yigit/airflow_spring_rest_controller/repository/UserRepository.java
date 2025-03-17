package com.yigit.airflow_spring_rest_controller.repository;

import com.yigit.airflow_spring_rest_controller.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for User entity
 * Uses Spring Data R2DBC to provide reactive database operations
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    
    /**
     * Find a user by username
     * @param username the username to search for
     * @return a Mono containing the user, or an empty Mono if not found
     */
    Mono<User> findByUsername(String username);
    
    /**
     * Find a user by email
     * @param email the email to search for
     * @return a Mono containing the user, or an empty Mono if not found
     */
    Mono<User> findByEmail(String email);
    
    /**
     * Check if a user with the given ID exists
     * @param id the ID to check
     * @return a Mono of Boolean - true if exists, false otherwise
     */
    Mono<Boolean> existsById(String id);
    
    /**
     * Find users by first name and last name
     * @param firstName the first name to search for
     * @param lastName the last name to search for
     * @return a Flux of users with matching first and last names
     */
    Flux<User> findByFirstNameAndLastName(String firstName, String lastName);
    
    /**
     * Find enabled users
     * @return a Flux of all enabled users
     */
    Flux<User> findByEnabledTrue();
    
    /**
     * Custom query example - find users created after a certain timestamp
     * @param timestamp the timestamp to compare against
     * @return a Flux of users created after the given timestamp
     */
    @Query("SELECT * FROM users WHERE created_timestamp > :timestamp")
    Flux<User> findUsersCreatedAfter(Long timestamp);
} 