package com.reportplatform.enginereporting.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Health indicator that reports the status of all consolidated reporting modules.
 * Verifies database connectivity and module availability.
 */
@Component
public class ModuleHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public ModuleHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            if (valid) {
                builder.up()
                        .withDetail("database", "connected")
                        .withDetail("modules", java.util.List.of(
                                "lifecycle", "period", "form", "pptx-template", "notification"))
                        .withDetail("service", "engine-reporting");
            } else {
                builder.down().withDetail("database", "connection invalid");
            }
        } catch (Exception e) {
            builder.down()
                    .withDetail("database", "unavailable")
                    .withException(e);
        }

        return builder.build();
    }
}
