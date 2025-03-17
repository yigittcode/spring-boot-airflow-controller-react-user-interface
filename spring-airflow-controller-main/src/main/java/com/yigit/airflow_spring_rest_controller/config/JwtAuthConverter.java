package com.yigit.airflow_spring_rest_controller.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converter that extracts roles from JWT token and transforms them into Spring Security authorities
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Converts JWT token to AbstractAuthenticationToken for Spring Security
     */
    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        
        ReactiveJwtAuthenticationConverterAdapter adapter = 
            new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
        return adapter.convert(jwt);
    }
    
    /**
     * Extracts role information from JWT and converts it to a list of GrantedAuthority
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = extractRoles(jwt);
        
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
            .collect(Collectors.toList());
    }
    
    /**
     * Extracts role information from JWT token
     */
    private List<String> extractRoles(Jwt jwt) {
        try {
            // First try to get roles from realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
            if (realmAccess != null && realmAccess.containsKey(ROLES_CLAIM)) {
                return (List<String>) realmAccess.get(ROLES_CLAIM);
            }
        } catch (Exception e) {
            // If realm_access claim is not found, continue
        }
        
        // Alternatively try to get roles directly from roles claim
        List<String> directRoles = jwt.getClaimAsStringList(ROLES_CLAIM);
        return directRoles != null ? directRoles : Collections.emptyList();
    }
}