package com.reportplatform.base.grpc;

import io.grpc.Context;

/**
 * gRPC {@link Context.Key} definitions for request context propagation.
 * <p>
 * These keys allow RequestContext to be propagated through gRPC's structured
 * concurrency model, which is safer than thread-locals for async/virtual thread scenarios.
 */
public final class GrpcContextKeys {

    /**
     * gRPC context key for the {@link RequestContextHolder.RequestContext}.
     */
    public static final Context.Key<RequestContextHolder.RequestContext> REQUEST_CONTEXT_KEY =
            Context.key("reportplatform-request-context");

    private GrpcContextKeys() {
        // Constants class
    }
}
