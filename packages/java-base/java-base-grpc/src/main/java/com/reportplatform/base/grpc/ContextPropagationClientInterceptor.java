package com.reportplatform.base.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC client interceptor that propagates {@link RequestContextHolder.RequestContext}
 * from the current thread into outgoing gRPC call metadata.
 * <p>
 * This ensures that identity and tracing context is forwarded across service boundaries
 * when making downstream gRPC calls.
 */
public class ContextPropagationClientInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagationClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                RequestContextHolder.RequestContext context = resolveContext();

                if (context != null) {
                    headers.put(ContextPropagationServerInterceptor.TRACE_ID_KEY, context.traceId());
                    headers.put(ContextPropagationServerInterceptor.USER_ID_KEY, context.userId());
                    headers.put(ContextPropagationServerInterceptor.ORG_ID_KEY, context.orgId());
                    headers.put(ContextPropagationServerInterceptor.CORRELATION_ID_KEY, context.correlationId());

                    if (!context.roles().isEmpty()) {
                        headers.put(ContextPropagationServerInterceptor.ROLES_KEY,
                                String.join(",", context.roles()));
                    }

                    log.debug("Propagating RequestContext to outgoing gRPC call {}: traceId={}, userId={}",
                            method.getFullMethodName(), context.traceId(), context.userId());
                } else {
                    log.warn("No RequestContext available for outgoing gRPC call {}. "
                            + "Context propagation skipped.", method.getFullMethodName());
                }

                super.start(responseListener, headers);
            }
        };
    }

    /**
     * Resolves the current RequestContext, first from the gRPC Context, then from thread-local.
     */
    private static RequestContextHolder.RequestContext resolveContext() {
        // Prefer gRPC Context (structured concurrency safe)
        RequestContextHolder.RequestContext fromGrpcContext =
                GrpcContextKeys.REQUEST_CONTEXT_KEY.get();
        if (fromGrpcContext != null) {
            return fromGrpcContext;
        }
        // Fall back to thread-local
        return RequestContextHolder.get();
    }
}
