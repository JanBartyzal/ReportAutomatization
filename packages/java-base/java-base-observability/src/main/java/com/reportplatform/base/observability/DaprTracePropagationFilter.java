package com.reportplatform.base.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that bridges OpenTelemetry trace context into SLF4J MDC.
 * <p>
 * Ensures that every log entry produced during a request carries the active
 * {@code traceId} and {@code spanId} from the OTEL context. This enables
 * correlation between structured logs (in Loki) and traces (in Tempo).
 * <p>
 * Also reads business context headers propagated by Dapr or the API gateway:
 * <ul>
 *   <li>{@code X-User-Id} → MDC {@code userId}</li>
 *   <li>{@code X-Org-Id} → MDC {@code orgId}</li>
 *   <li>{@code X-Correlation-Id} → MDC {@code correlationId}</li>
 * </ul>
 * <p>
 * The filter runs very early (Ordered.HIGHEST_PRECEDENCE + 10) so that all
 * downstream filters and controllers benefit from the enriched MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnClass(Span.class)
public class DaprTracePropagationFilter extends OncePerRequestFilter {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ORG_ID = "orgId";
    private static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract OTEL trace context from current span
            SpanContext spanContext = Span.current().getSpanContext();
            if (spanContext.isValid()) {
                MDC.put(MDC_TRACE_ID, spanContext.getTraceId());
                MDC.put(MDC_SPAN_ID, spanContext.getSpanId());
            }

            // Extract business context headers (from Dapr/API Gateway)
            setMdcFromHeader(request, "X-User-Id", MDC_USER_ID);
            setMdcFromHeader(request, "X-Org-Id", MDC_ORG_ID);
            setMdcFromHeader(request, "X-Correlation-Id", MDC_CORRELATION_ID);

            // Propagate trace context in response headers (for downstream correlation)
            if (spanContext.isValid()) {
                response.setHeader("X-Trace-Id", spanContext.getTraceId());
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_SPAN_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ORG_ID);
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    private void setMdcFromHeader(HttpServletRequest request, String headerName, String mdcKey) {
        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            MDC.put(mdcKey, value);
        }
    }
}
