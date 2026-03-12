package com.reportplatform.base.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration for observability components.
 * <p>
 * Registers structured logging configuration and metrics/tracing beans.
 * Can be disabled with {@code reportplatform.observability.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "reportplatform.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ObservabilityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public StructuredLoggingConfig structuredLoggingConfig(
            @Value("${spring.application.name:unknown}") String applicationName,
            @Value("${reportplatform.observability.app-version:1.0.0}") String applicationVersion,
            @Value("${reportplatform.observability.structured-logging.enabled:true}") boolean enabled) {
        log.info("Registering StructuredLoggingConfig for service: {}", applicationName);
        return new StructuredLoggingConfig(applicationName, applicationVersion, enabled);
    }
}
