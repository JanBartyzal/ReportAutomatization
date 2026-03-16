package com.reportplatform.dash.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Sets the PostgreSQL session variable {@code app.current_org_id} before each request
 * so that Row-Level Security policies can filter data by organization.
 */
@Component("dashboardRlsInterceptor")
public class RlsInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RlsInterceptor.class);
    private static final String ORG_ID_HEADER = "X-Org-Id";

    private final DataSource dataSource;

    public RlsInterceptor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        String orgId = request.getHeader(ORG_ID_HEADER);
        if (orgId == null || orgId.isBlank()) {
            // Actuator and health endpoints may not have org headers
            return true;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT set_config('app.current_org_id', ?, true)")) {
            stmt.setString(1, orgId);
            stmt.execute();
        } catch (SQLException e) {
            log.error("Failed to set RLS org_id for org={}", orgId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }

        return true;
    }
}
