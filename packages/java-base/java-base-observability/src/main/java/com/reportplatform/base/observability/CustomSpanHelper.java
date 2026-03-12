package com.reportplatform.base.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * Helper for creating custom OpenTelemetry spans with business-specific attributes.
 * <p>
 * Provides methods to create spans for common operations (file processing,
 * orchestrator steps, database queries) with standardized attributes that enable
 * trace search by {@code file_id}, {@code user_id}, and {@code org_id} in Tempo.
 * <p>
 * Usage example:
 * <pre>{@code
 * @Autowired CustomSpanHelper spans;
 *
 * Span span = spans.startFileProcessingSpan("upload", fileId, orgId, userId);
 * try (var scope = span.makeCurrent()) {
 *     // ... processing logic ...
 * } catch (Exception e) {
 *     spans.recordError(span, e);
 *     throw e;
 * } finally {
 *     span.end();
 * }
 * }</pre>
 */
public class CustomSpanHelper {

    // Business attribute keys for trace search
    public static final AttributeKey<String> ATTR_FILE_ID = AttributeKey.stringKey("file_id");
    public static final AttributeKey<String> ATTR_USER_ID = AttributeKey.stringKey("user_id");
    public static final AttributeKey<String> ATTR_ORG_ID = AttributeKey.stringKey("org_id");
    public static final AttributeKey<String> ATTR_WORKFLOW_ID = AttributeKey.stringKey("workflow_id");
    public static final AttributeKey<String> ATTR_STEP_NAME = AttributeKey.stringKey("step_name");
    public static final AttributeKey<String> ATTR_FILE_TYPE = AttributeKey.stringKey("file_type");
    public static final AttributeKey<Long> ATTR_FILE_SIZE = AttributeKey.longKey("file_size_bytes");

    private final Tracer tracer;

    public CustomSpanHelper(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Start a span for file processing operations (upload, parse, convert).
     */
    public Span startFileProcessingSpan(String operation, String fileId, String orgId, String userId) {
        return tracer.spanBuilder("file." + operation)
                .setAllAttributes(Attributes.of(
                        ATTR_FILE_ID, fileId,
                        ATTR_ORG_ID, orgId,
                        ATTR_USER_ID, userId
                ))
                .startSpan();
    }

    /**
     * Start a span for an orchestrator workflow step.
     */
    public Span startOrchestratorStepSpan(String workflowId, String stepName, String fileId) {
        return tracer.spanBuilder("orchestrator." + stepName)
                .setAllAttributes(Attributes.of(
                        ATTR_WORKFLOW_ID, workflowId,
                        ATTR_STEP_NAME, stepName,
                        ATTR_FILE_ID, fileId
                ))
                .startSpan();
    }

    /**
     * Start a span for a gRPC call to another service via Dapr.
     */
    public Span startDaprInvocationSpan(String targetAppId, String method) {
        return tracer.spanBuilder("dapr.invoke." + targetAppId + "/" + method)
                .setAttribute("dapr.app_id", targetAppId)
                .setAttribute("rpc.method", method)
                .startSpan();
    }

    /**
     * Start a span for database operations.
     */
    public Span startDbSpan(String operation, String table) {
        return tracer.spanBuilder("db." + operation)
                .setAttribute("db.operation", operation)
                .setAttribute("db.sql.table", table)
                .startSpan();
    }

    /**
     * Record an error on a span and set its status to ERROR.
     */
    public void recordError(Span span, Throwable throwable) {
        span.recordException(throwable);
        span.setStatus(StatusCode.ERROR, throwable.getMessage());
    }

    /**
     * Add business attributes to the current active span.
     */
    public void enrichCurrentSpan(String fileId, String orgId, String userId) {
        Span current = Span.current();
        if (current.getSpanContext().isValid()) {
            current.setAttribute(ATTR_FILE_ID, fileId);
            current.setAttribute(ATTR_ORG_ID, orgId);
            current.setAttribute(ATTR_USER_ID, userId);
        }
    }
}
