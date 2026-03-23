package com.reportplatform.enginedata.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Shared security configuration for engine-data.
 * Stateless sessions, CSRF disabled, permits actuator/dapr/events endpoints.
 * API endpoints protected by @PreAuthorize — roles populated from X-Roles header.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    public SecurityConfig(HeaderAuthenticationFilter headerAuthenticationFilter) {
        this.headerAuthenticationFilter = headerAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/dapr/**").permitAll()
                        .requestMatchers("/events/**").permitAll()
                        // Internal pipeline endpoints (called by orchestrator, no user context)
                        .requestMatchers("/api/v1/map", "/api/v1/store", "/api/v1/store-doc",
                                         "/api/v1/rollback", "/api/v1/rollback-doc").permitAll()
                        .requestMatchers("/getRoutingInfo").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
