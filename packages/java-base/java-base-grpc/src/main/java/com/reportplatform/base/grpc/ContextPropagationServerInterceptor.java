package com.reportplatform.base.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * gRPC server interceptor that extracts {@link RequestContextHolder.RequestContext}
 * from incoming gRPC metadata headers and sets it in the thread-local
 * {@link RequestContextHolder}.
 * <p>
 * Expected metadata keys:
 * <ul>
 *     <li>{@code x-trace-id} - Distributed trace identifier</li>
 *     <li>{@code x-user-id} - Authenticated user identifier</li>
 *     <li>{@code x-org-id} - Organization/tenant identifier</li>
 *     <li>{@code x-roles} - Comma-separated list of user roles</li>
 *     <li>{@code x-correlation-id} - Business correlation identifier</li>
 * </ul>
 */
public class ContextPropagationServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ContextPropagationServerInterceptor.class);

    static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> USER_ID_KEY =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> ORG_ID_KEY =
            Metadata.Key.of("x-org-id", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> ROLES_KEY =
            Metadata.Key.of("x-roles", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> CORRELATION_ID_KEY =
            Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String traceId = getMetadataValue(headers, TRACE_ID_KEY, "");
        String userId = getMetadataValue(headers, USER_ID_KEY, "");
        String orgId = getMetadataValue(headers, ORG_ID_KEY, "");
        String rolesStr = getMetadataValue(headers, ROLES_KEY, "");
        String correlationId = getMetadataValue(headers, CORRELATION_ID_KEY, traceId);

        List<String> roles = parseRoles(rolesStr);

        var requestContext = new RequestContextHolder.RequestContext(
                traceId, userId, orgId, roles, correlationId
        );

        log.debug("Extracted gRPC RequestContext: traceId={}, userId={}, orgId={}, roles={}",
                traceId, userId, orgId, roles);

        // Set in thread-local for the duration of the call
        RequestContextHolder.set(requestContext);

        // Also propagate via gRPC Context for structured concurrency
        Context grpcContext = Context.current().withValue(
                GrpcContextKeys.REQUEST_CONTEXT_KEY, requestContext);

        return Contexts.interceptCall(grpcContext, call, headers,
                new ServerCallHandler<>() {
                    @Override
                    public ServerCall.Listener<ReqT> startCall(
                            ServerCall<ReqT, RespT> call,
                            Metadata headers) {
                        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
                        return new CleanupListener<>(listener);
                    }
                });
    }

    private static String getMetadataValue(Metadata headers, Metadata.Key<String> key, String defaultValue) {
        String value = headers.get(key);
        return value != null ? value : defaultValue;
    }

    private static List<String> parseRoles(String rolesStr) {
        if (rolesStr == null || rolesStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Listener wrapper that cleans up the thread-local context when the call completes.
     */
    private static class CleanupListener<ReqT>
            extends io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {

        CleanupListener(ServerCall.Listener<ReqT> delegate) {
            super(delegate);
        }

        @Override
        public void onComplete() {
            try {
                super.onComplete();
            } finally {
                RequestContextHolder.clear();
            }
        }

        @Override
        public void onCancel() {
            try {
                super.onCancel();
            } finally {
                RequestContextHolder.clear();
            }
        }
    }
}
