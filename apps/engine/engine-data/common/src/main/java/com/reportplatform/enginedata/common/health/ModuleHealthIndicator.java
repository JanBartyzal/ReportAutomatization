package com.reportplatform.enginedata.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Health indicator that reports the status of each engine-data module.
 * Checks database connectivity and reports module availability.
 */
@Component
public class ModuleHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public ModuleHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return Health.up()
                        .withDetail("service", "engine-data")
                        .withDetail("modules", new String[]{
                                "sink-tbl", "sink-doc", "sink-log",
                                "query", "dashboard", "search", "template"
                        })
                        .withDetail("database", "connected")
                        .build();
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("service", "engine-data")
                    .withDetail("database", "disconnected")
                    .withException(e)
                    .build();
        }
        return Health.unknown().build();
    }
}
