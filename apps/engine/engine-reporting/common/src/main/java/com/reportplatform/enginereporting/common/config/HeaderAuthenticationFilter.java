package com.reportplatform.enginereporting.common.config;

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
 * Authentication filter that reads X-User-Id and X-Roles headers
 * (set by API Gateway / ForwardAuth) and populates Spring SecurityContext.
 */
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities;
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            } else {
                authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_VIEWER"));
            }

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            // Fallback: check X-API-Key header for direct service calls (UAT/dev)
            String apiKey = request.getHeader("X-API-Key");
            if (apiKey != null && !apiKey.isBlank()) {
                List<SimpleGrantedAuthority> authorities;
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                } else {
                    authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_VIEWER"),
                            new SimpleGrantedAuthority("ROLE_EDITOR"));
                }
                var auth = new UsernamePasswordAuthenticationToken(
                        "apikey-user", null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
                return;
            }

            // Fallback: check Authorization Bearer header for direct service calls
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String principalId = extractSubjectFromJwt(authHeader.substring(7));
                List<SimpleGrantedAuthority> authorities;
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    authorities = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(r -> !r.isEmpty())
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .collect(Collectors.toList());
                } else {
                    authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_VIEWER"),
                            new SimpleGrantedAuthority("ROLE_EDITOR"));
                }
                var auth = new UsernamePasswordAuthenticationToken(principalId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Set RLS ThreadLocal context for DataSource wrapper
        String orgIdHeader = request.getHeader("X-Org-Id");
        if (orgIdHeader != null && !orgIdHeader.isBlank()) {
            RlsContext.setOrgId(orgIdHeader);
        }
        if (userId != null && !userId.isBlank()) {
            RlsContext.setUserId(userId);
        }
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            RlsContext.setRole(rolesHeader.replaceAll("[^A-Z_,]", ""));
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            RlsContext.clear();
        }
    }

    private String extractSubjectFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                int subIdx = payload.indexOf("\"sub\"");
                if (subIdx >= 0) {
                    int valueStart = payload.indexOf("\"", subIdx + 5) + 1;
                    int valueEnd = payload.indexOf("\"", valueStart);
                    if (valueStart > 0 && valueEnd > valueStart) {
                        return payload.substring(valueStart, valueEnd);
                    }
                }
            }
        } catch (Exception ignored) {}
        return "bearer-user";
    }
}
