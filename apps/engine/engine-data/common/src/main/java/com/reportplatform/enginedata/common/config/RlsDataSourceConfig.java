package com.reportplatform.enginedata.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Post-processes the auto-configured DataSource to wrap it with RLS context setting.
 * Sets PostgreSQL session variables on every connection checkout from the pool.
 */
@Configuration
public class RlsDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RlsDataSourceConfig.class);

    @Bean
    public static BeanPostProcessor rlsDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource ds && !"rlsWrappedDataSource".equals(beanName)
                        && !(bean instanceof DelegatingDataSource)) {
                    log.info("Wrapping DataSource '{}' with RLS context interceptor", beanName);
                    return new RlsDataSourceWrapper(ds);
                }
                return bean;
            }
        };
    }

    private static class RlsDataSourceWrapper extends DelegatingDataSource {

        RlsDataSourceWrapper(DataSource delegate) {
            super(delegate);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = super.getConnection();
            applyRlsContext(conn);
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection conn = super.getConnection(username, password);
            applyRlsContext(conn);
            return conn;
        }

        private void applyRlsContext(Connection conn) {
            String orgId = RlsContext.getOrgId();
            String userId = RlsContext.getUserId();
            String role = RlsContext.getRole();

            try {
                if (orgId != null && !orgId.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT set_config('app.current_org_id', ?, false)")) {
                        ps.setString(1, orgId);
                        ps.execute();
                    }
                }
                if (userId != null && !userId.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT set_config('app.current_user_id', ?, false)")) {
                        ps.setString(1, userId);
                        ps.execute();
                    }
                }
                if (role != null && !role.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT set_config('app.current_user_role', ?, false)")) {
                        ps.setString(1, role);
                        ps.execute();
                    }
                }
            } catch (SQLException e) {
                log.debug("Failed to set RLS context: {}", e.getMessage());
            }
        }
    }
}
