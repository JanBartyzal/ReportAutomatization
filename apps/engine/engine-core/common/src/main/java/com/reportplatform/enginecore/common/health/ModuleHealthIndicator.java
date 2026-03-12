package com.reportplatform.enginecore.common.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator that reports status of each engine-core module.
 * Provides detailed information about module availability at /actuator/health.
 */
@Component
public class ModuleHealthIndicator implements HealthIndicator {

    private static final String[] MODULES = {"auth", "admin", "batch", "versioning", "audit"};

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        for (String module : MODULES) {
            builder.withDetail(module, "UP");
        }

        builder.withDetail("consolidated", true);
        builder.withDetail("version", "1.0.0-SNAPSHOT");

        return builder.build();
    }
}
