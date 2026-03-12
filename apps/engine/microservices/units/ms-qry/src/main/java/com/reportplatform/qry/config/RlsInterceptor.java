package com.reportplatform.qry.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that sets PostgreSQL session variables for Row-Level Security.
 * Before each query request, sets {@code app.current_org_id}, {@code app.current_user_role},
 * and {@code app.query_scope} on the database connection so that RLS policies filter
 * results correctly based on organization, role, and scope.
 */
@Component
public class RlsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RlsInterceptor.class);
    private static final String ORG_ID_HEADER = "X-Org-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String QUERY_SCOPE_HEADER = "X-Query-Scope";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String orgId = request.getHeader(ORG_ID_HEADER);
        if (orgId != null && !orgId.isBlank()) {
            // Validate UUID format to prevent SQL injection
            try {
                java.util.UUID.fromString(orgId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid org_id format in header: {}", orgId);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }

            entityManager.createNativeQuery("SET LOCAL app.current_org_id = :orgId")
                    .setParameter("orgId", orgId)
                    .executeUpdate();

            log.debug("RLS org_id set to: {}", orgId);
        }

        // Set user role for RLS policies (enables HOLDING_ADMIN cross-org access)
        String userRole = request.getHeader(USER_ROLE_HEADER);
        if (userRole != null && !userRole.isBlank()) {
            // Sanitize: only allow known role values
            String sanitizedRole = userRole.replaceAll("[^A-Z_]", "");
            entityManager.createNativeQuery("SET LOCAL app.current_user_role = :role")
                    .setParameter("role", sanitizedRole)
                    .executeUpdate();

            log.debug("RLS user_role set to: {}", sanitizedRole);
        }

        // Set query scope for scope-aware queries
        String queryScope = request.getHeader(QUERY_SCOPE_HEADER);
        if (queryScope != null && !queryScope.isBlank()) {
            String sanitizedScope = queryScope.replaceAll("[^A-Z_]", "");
            entityManager.createNativeQuery("SET LOCAL app.query_scope = :scope")
                    .setParameter("scope", sanitizedScope)
                    .executeUpdate();

            log.debug("RLS query_scope set to: {}", sanitizedScope);
        }

        return true;
    }
}
