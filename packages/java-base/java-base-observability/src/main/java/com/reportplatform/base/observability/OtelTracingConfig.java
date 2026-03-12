package com.reportplatform.base.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenTelemetry tracing for Spring Boot services.
 * <p>
 * Uses Spring Boot's auto-configured {@link OpenTelemetry} instance (from Micrometer bridge)
 * and exposes a {@link Tracer} bean for creating custom spans.
 * <p>
 * The OTLP exporter endpoint is configured via:
 * <ul>
 *   <li>{@code OTEL_EXPORTER_OTLP_ENDPOINT} env var (standard OTEL convention)</li>
 *   <li>{@code management.otlp.tracing.endpoint} property (Spring Boot convention)</li>
 * </ul>
 * <p>
 * Spring Boot 3.x auto-configures the OTLP exporter when {@code micrometer-tracing-bridge-otel}
 * and {@code opentelemetry-exporter-otlp} are on the classpath, so we don't need to manually
 * create the exporter or tracer provider.
 */
@Configuration
@ConditionalOnClass(OpenTelemetry.class)
@ConditionalOnProperty(
        prefix = "reportplatform.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OtelTracingConfig {

    private static final Logger log = LoggerFactory.getLogger(OtelTracingConfig.class);

    @Bean
    @ConditionalOnMissingBean(Tracer.class)
    public Tracer tracer(
            OpenTelemetry openTelemetry,
            @Value("${spring.application.name:unknown}") String serviceName) {
        log.info("Creating OpenTelemetry Tracer for service: {}", serviceName);
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }

    @Bean
    @ConditionalOnMissingBean(CustomSpanHelper.class)
    public CustomSpanHelper customSpanHelper(Tracer tracer) {
        return new CustomSpanHelper(tracer);
    }
}
