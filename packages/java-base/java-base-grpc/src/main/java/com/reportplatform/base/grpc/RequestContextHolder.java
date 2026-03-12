package com.reportplatform.base.grpc;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thread-local holder for {@link RequestContext} during gRPC call processing.
 * <p>
 * Uses a scoped value pattern with explicit set/clear lifecycle.
 * For virtual threads (Java 21), this uses an inheritable thread-local
 * to ensure context propagation across virtual thread boundaries.
 * <p>
 * Usage pattern:
 * <pre>{@code
 * RequestContextHolder.set(context);
 * try {
 *     // process request
 * } finally {
 *     RequestContextHolder.clear();
 * }
 * }</pre>
 */
public final class RequestContextHolder {

    private static final InheritableThreadLocal<RequestContext> CONTEXT =
            new InheritableThreadLocal<>();

    private RequestContextHolder() {
        // Utility class
    }

    /**
     * Sets the request context for the current thread.
     *
     * @param context the request context to set
     * @throws NullPointerException if context is null
     */
    public static void set(RequestContext context) {
        Objects.requireNonNull(context, "RequestContext must not be null");
        CONTEXT.set(context);
    }

    /**
     * Returns the request context for the current thread, or null if not set.
     */
    public static RequestContext get() {
        return CONTEXT.get();
    }

    /**
     * Returns the request context for the current thread.
     *
     * @throws IllegalStateException if no context has been set
     */
    public static RequestContext require() {
        RequestContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException(
                    "No RequestContext available. Ensure the gRPC interceptor is configured.");
        }
        return context;
    }

    /**
     * Clears the request context for the current thread.
     * Must be called in a finally block after processing is complete.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Immutable request context carrying identity and tracing information
     * through the gRPC call chain.
     *
     * Mirrors the proto definition at {@code com.reportplatform.proto.common.v1.RequestContext}.
     *
     * @param traceId       distributed trace identifier
     * @param userId        authenticated user identifier
     * @param orgId         organization/tenant identifier
     * @param roles         list of user roles
     * @param correlationId correlation identifier for business-level tracing
     */
    public record RequestContext(
            String traceId,
            String userId,
            String orgId,
            List<String> roles,
            String correlationId
    ) {
        public RequestContext {
            traceId = traceId != null ? traceId : "";
            userId = userId != null ? userId : "";
            orgId = orgId != null ? orgId : "";
            roles = roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
            correlationId = correlationId != null ? correlationId : "";
        }
    }
}
