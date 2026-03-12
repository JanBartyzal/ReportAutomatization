package com.reportplatform.base.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Extracts {@link RequestContext} from incoming HTTP request headers.
 * <p>
 * Expected headers (typically injected by API Gateway or service mesh):
 * <ul>
 *     <li>{@code X-User-Id} - Authenticated user identifier (Azure Entra ID oid)</li>
 *     <li>{@code X-Org-Id} - Organization/tenant identifier</li>
 *     <li>{@code X-Roles} - Comma-separated list of user roles</li>
 *     <li>{@code X-Trace-Id} - Distributed trace identifier (correlation ID)</li>
 * </ul>
 */
public final class RequestContextExtractor {

    private static final Logger log = LoggerFactory.getLogger(RequestContextExtractor.class);

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    private RequestContextExtractor() {
        // Utility class
    }

    /**
     * Extracts a {@link RequestContext} from the given HTTP request headers.
     * If X-Trace-Id is not present, a new UUID is generated.
     *
     * @param request the incoming HTTP servlet request
     * @return the extracted request context
     */
    public static RequestContext extract(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String userId = request.getHeader(HEADER_USER_ID);
        String orgId = request.getHeader(HEADER_ORG_ID);
        String rolesHeader = request.getHeader(HEADER_ROLES);
        String traceId = Optional.ofNullable(request.getHeader(HEADER_TRACE_ID))
                .orElseGet(() -> UUID.randomUUID().toString());
        String correlationId = Optional.ofNullable(request.getHeader(HEADER_CORRELATION_ID))
                .orElse(traceId);

        List<String> roles = parseRoles(rolesHeader);

        if (userId == null || userId.isBlank()) {
            log.warn("Missing {} header in request to {}", HEADER_USER_ID, request.getRequestURI());
        }
        if (orgId == null || orgId.isBlank()) {
            log.warn("Missing {} header in request to {}", HEADER_ORG_ID, request.getRequestURI());
        }

        return new RequestContext(
                traceId,
                userId != null ? userId : "",
                orgId != null ? orgId : "",
                roles,
                correlationId
        );
    }

    private static List<String> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Immutable request context carrying identity and tracing information
     * through the service call chain.
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
            Objects.requireNonNull(traceId, "traceId must not be null");
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(orgId, "orgId must not be null");
            roles = roles != null ? List.copyOf(roles) : List.of();
            Objects.requireNonNull(correlationId, "correlationId must not be null");
        }
    }
}
