package com.reportplatform.base.observability;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Configures structured JSON logging for production environments.
 * <p>
 * When enabled (via
 * {@code reportplatform.observability.structured-logging.enabled=true}),
 * replaces the default Logback console appender with a JSON encoder that
 * outputs
 * structured log entries compatible with cloud logging systems (GCP Cloud
 * Logging,
 * Azure Monitor, etc.).
 * <p>
 * JSON output includes:
 * <ul>
 * <li>Standard fields: timestamp, level, logger, message, thread</li>
 * <li>MDC fields: traceId, spanId, userId, orgId (when set via MDC)</li>
 * <li>Exception stack traces as structured JSON</li>
 * <li>Service name and version metadata</li>
 * </ul>
 */
public class StructuredLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingConfig.class);

    private final String applicationName;
    private final String applicationVersion;
    private final boolean enabled;

    public StructuredLoggingConfig(
            @Value("${spring.application.name:unknown}") String applicationName,
            @Value("${reportplatform.observability.app-version:1.0.0}") String applicationVersion,
            @Value("${reportplatform.observability.structured-logging.enabled:true}") boolean enabled) {
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void configureStructuredLogging() {
        if (!enabled) {
            log.info("Structured JSON logging is disabled");
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext
                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);

        // Detach existing console appenders
        rootLogger.detachAppender("console");
        rootLogger.detachAppender("CONSOLE");

        // Create structured JSON appender
        ConsoleAppender<ILoggingEvent> jsonAppender = new ConsoleAppender<>();
        jsonAppender.setContext(loggerContext);
        jsonAppender.setName("JSON_CONSOLE");

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(loggerContext);
        // Include all MDC keys (empty list = include all)

        // Configure field names for cloud logging compatibility
        LogstashFieldNames fieldNames = new LogstashFieldNames();
        fieldNames.setTimestamp("timestamp");
        fieldNames.setMessage("message");
        fieldNames.setLogger("logger");
        fieldNames.setThread("thread");
        fieldNames.setStackTrace("stack_trace");
        encoder.setFieldNames(fieldNames);

        // Add custom fields
        encoder.setCustomFields(String.format(
                "{\"service\":\"%s\",\"version\":\"%s\"}",
                applicationName, applicationVersion));

        encoder.start();
        jsonAppender.setEncoder(encoder);
        jsonAppender.start();

        rootLogger.addAppender(jsonAppender);

        log.info("Structured JSON logging configured for service: {} v{}",
                applicationName, applicationVersion);
    }
}
