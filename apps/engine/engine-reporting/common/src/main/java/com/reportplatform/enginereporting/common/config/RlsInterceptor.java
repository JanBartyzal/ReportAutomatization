package com.reportplatform.enginereporting.common.config;

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
 * Sets PostgreSQL session variables for Row-Level Security before each request.
 * Uses EntityManager (same JDBC connection as JPA) so the settings are visible
 * to subsequent Hibernate queries within the same request.
 *
 * Uses set_config() function instead of SET LOCAL to avoid reserved keyword conflicts
 * (e.g. "current_role" is a PostgreSQL built-in).
 */
@Component("reportingRlsInterceptor")
public class RlsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RlsInterceptor.class);
    private static final String ORG_ID_HEADER = "X-Org-Id";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-Roles";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String orgId = request.getHeader(ORG_ID_HEADER);
        if (orgId != null && !orgId.isBlank()) {
            try {
                java.util.UUID.fromString(orgId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid org_id format in header: {}", orgId);
                return true;
            }
            entityManager.createNativeQuery("SELECT set_config('app.current_org_id', '" + orgId + "', false)")
                    .getSingleResult();
            log.debug("RLS org_id set to: {}", orgId);
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            String sanitized = userId.replaceAll("[^a-zA-Z0-9:\\-]", "");
            if (!sanitized.isEmpty()) {
                entityManager.createNativeQuery("SELECT set_config('app.current_user_id', '" + sanitized + "', false)")
                        .getSingleResult();
            }
        }

        String userRole = request.getHeader(USER_ROLE_HEADER);
        if (userRole != null && !userRole.isBlank()) {
            String sanitizedRole = userRole.replaceAll("[^A-Z_,]", "");
            if (!sanitizedRole.isEmpty()) {
                entityManager.createNativeQuery("SELECT set_config('app.current_user_role', '" + sanitizedRole + "', false)")
                        .getSingleResult();
                String primaryRole = sanitizedRole.contains(",") ? sanitizedRole.split(",")[0] : sanitizedRole;
                entityManager.createNativeQuery("SELECT set_config('app.current_role', '" + primaryRole + "', false)")
                        .getSingleResult();
            }
        }

        return true;
    }
}
