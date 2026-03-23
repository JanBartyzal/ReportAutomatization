package com.reportplatform.enginedata.common.config;

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
 *
 * Internal services (engine-data, engine-reporting) trust these headers
 * because they are only reachable from within the service mesh (Dapr)
 * or via the API Gateway which validates tokens first.
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

        // Debug logging
        org.slf4j.LoggerFactory.getLogger(HeaderAuthenticationFilter.class)
            .info("HeaderAuthFilter: path={}, userId={}, roles={}",
                  request.getRequestURI(), userId, rolesHeader);

        if (userId != null && !userId.isBlank()) {
            List<SimpleGrantedAuthority> authorities;
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            } else {
                // Default: VIEWER role if no roles header
                authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_VIEWER"));
            }

            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            org.slf4j.LoggerFactory.getLogger(HeaderAuthenticationFilter.class)
                .info("HeaderAuthFilter: set auth with {} authorities: {}",
                      authorities.size(), authorities);
        }

        filterChain.doFilter(request, response);
    }
}
