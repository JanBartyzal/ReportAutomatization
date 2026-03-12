package com.reportplatform.auth.config;

import com.reportplatform.auth.service.ApiKeyService;
import com.reportplatform.auth.service.TokenValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final TokenValidationService tokenValidationService;
    private final ApiKeyService apiKeyService;

    @Value("${auth.api-key.header-name:X-API-Key}")
    private String apiKeyHeaderName;

    public SecurityConfig(TokenValidationService tokenValidationService, ApiKeyService apiKeyService) {
        this.tokenValidationService = tokenValidationService;
        this.apiKeyService = apiKeyService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/verify").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                // Skip for permitted endpoints
                String path = request.getRequestURI();
                if (path.equals("/api/auth/verify")
                        || path.startsWith("/actuator/health")
                        || path.startsWith("/actuator/info")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Try API key authentication first
                String apiKey = request.getHeader(apiKeyHeaderName);
                if (apiKey != null && !apiKey.isBlank()) {
                    var apiKeyEntity = apiKeyService.validateApiKey(apiKey);
                    if (apiKeyEntity.isPresent()) {
                        var entity = apiKeyEntity.get();
                        var auth = new UsernamePasswordAuthenticationToken(
                                "apikey:" + entity.getId(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + entity.getRole().name()))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                // Try Bearer token authentication
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    var claims = tokenValidationService.validateToken(token);
                    if (claims.isPresent()) {
                        var validated = claims.get();
                        var authorities = validated.roles().stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .toList();

                        var auth = new UsernamePasswordAuthenticationToken(
                                validated.oid(),
                                validated,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                // No valid authentication found - let Spring Security handle 401
                filterChain.doFilter(request, response);
            }
        };
    }
}
