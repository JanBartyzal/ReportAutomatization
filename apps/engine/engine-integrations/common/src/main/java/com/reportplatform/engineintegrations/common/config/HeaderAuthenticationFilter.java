package com.reportplatform.engineintegrations.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication filter for engine-integrations.
 * Reads X-User-Id / X-Roles headers or falls back to Bearer token.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            // Check X-API-Key for direct service calls (UAT/dev)
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
                if (authorities.isEmpty()) {
                    authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_VIEWER"),
                            new SimpleGrantedAuthority("ROLE_EDITOR"));
                }
                var auth = new UsernamePasswordAuthenticationToken("apikey-user", null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
                if (authorities.isEmpty() || authorities.size() == 1) {
                    authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_VIEWER"),
                            new SimpleGrantedAuthority("ROLE_EDITOR"));
                }
                var auth = new UsernamePasswordAuthenticationToken("bearer-user", null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            return Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_VIEWER"));
    }
}
