/**
 * OpenTelemetry browser SDK initialization.
 *
 * Configures distributed tracing from the frontend browser to the backend
 * services via the OTEL Collector HTTP endpoint.
 *
 * Traces include:
 * - Automatic fetch/XHR instrumentation (covers all Axios calls)
 * - Page load/navigation timing
 * - Manual spans for key user interactions
 * - Global error handling
 * - Performance metrics collection
 * - Anonymous user session tracking
 *
 * The OTEL Collector is reached via the nginx proxy at /otel/ which
 * forwards to otel-collector:4318 (OTLP HTTP).
 *
 * Part of P5-W4-001: Frontend Error Tracking & Analytics
 */
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { ZoneContextManager } from '@opentelemetry/context-zone';
import { Resource } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { XMLHttpRequestInstrumentation } from '@opentelemetry/instrumentation-xml-http-request';
import { DocumentLoadInstrumentation } from '@opentelemetry/instrumentation-document-load';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { W3CTraceContextPropagator, trace, context, propagation } from '@opentelemetry/api';

let initialized = false;

/**
 * Generate or retrieve anonymous session ID.
 * Stored in sessionStorage - no PII, only for session tracking.
 */
function getSessionId(): string {
    const SESSION_KEY = 'rp_session_id';
    let sessionId = sessionStorage.getItem(SESSION_KEY);

    if (!sessionId) {
        // Generate anonymous UUID
        sessionId = crypto.randomUUID();
        sessionStorage.setItem(SESSION_KEY, sessionId);
    }

    return sessionId;
}

/**
 * Get current session info (anonymous, no PII).
 */
export function getSessionInfo(): { sessionId: string; sessionStart: number } {
    const SESSION_START_KEY = 'rp_session_start';
    let sessionStart = sessionStorage.getItem(SESSION_START_KEY);

    if (!sessionStart) {
        sessionStart = Date.now().toString();
        sessionStorage.setItem(SESSION_START_KEY, sessionStart);
    }

    return {
        sessionId: getSessionId(),
        sessionStart: parseInt(sessionStart, 10),
    };
}

/**
 * Get the current active trace ID.
 * Returns empty string if no active span or tracer not initialized.
 */
export function getTraceId(): string {
    const span = trace.getActiveSpan();
    if (!span) return '';
    return span.spanContext().traceId;
}

/**
 * Report a handled error to OpenTelemetry.
 * Used by ErrorBoundary and manual error reporting.
 */
export function reportError(error: Error, contextData?: Record<string, string>): void {
    const tracer = getTracer();
    const span = tracer.startSpan('error.handled');

    span.setAttribute('error.name', error.name);
    span.setAttribute('error.message', error.message);
    span.setAttribute('error.stack', error.stack || '');

    if (contextData) {
        Object.entries(contextData).forEach(([key, value]) => {
            span.setAttribute(key, value);
        });
    }

    // Add session info (anonymous)
    const sessionInfo = getSessionInfo();
    span.setAttribute('session.id', sessionInfo.sessionId);
    span.setAttribute('location.pathname', window.location.pathname);
    span.setAttribute('location.href', window.location.href);

    span.end();

    // Also log to console in development
    if (import.meta.env.DEV) {
        console.error('[OTEL] Error reported:', error.message, contextData, 'TraceID:', span.spanContext().traceId);
    }
}

/**
 * Initialize OpenTelemetry tracing in the browser.
 * Safe to call multiple times — subsequent calls are no-ops.
 */
export function initTelemetry(): void {
    if (initialized) return;

    const collectorUrl = import.meta.env.VITE_OTEL_COLLECTOR_URL || '/otel';
    const sessionInfo = getSessionInfo();

    const resource = new Resource({
        [ATTR_SERVICE_NAME]: 'ms-fe',
        [ATTR_SERVICE_VERSION]: '0.1.0',
        'deployment.environment': import.meta.env.MODE || 'development',
        'service.namespace': 'reportplatform',
        // Anonymous session tracking - NO PII
        'session.id': sessionInfo.sessionId,
        'session.start': sessionInfo.sessionStart.toString(),
    });

    const exporter = new OTLPTraceExporter({
        url: `${collectorUrl}/v1/traces`,
    });

    const provider = new WebTracerProvider({
        resource,
        spanProcessors: [new BatchSpanProcessor(exporter)],
    });

    // Use Zone.js context manager for async context propagation in browser
    provider.register({
        contextManager: new ZoneContextManager(),
        propagator: new W3CTraceContextPropagator(),
    });

    // Register automatic instrumentations
    registerInstrumentations({
        instrumentations: [
            new FetchInstrumentation({
                // Propagate trace context to API calls (same origin)
                propagateTraceHeaderCorsUrls: [
                    /\/api\//,        // All API calls
                    /\/otel\//,       // OTEL collector
                    /localhost/,      // Local development
                ],
                clearTimingResources: true,
                // Add custom span attributes for API calls
                applyCustomAttributesOnSpan: (span, request) => {
                    span.setAttribute('http.url', request.url);
                    span.setAttribute('http.method', request.method || 'GET');
                },
            }),
            new XMLHttpRequestInstrumentation({
                propagateTraceHeaderCorsUrls: [
                    /\/api\//,
                    /localhost/,
                ],
                applyCustomAttributesOnSpan: (span, xhr) => {
                    const url = xhr.responseURL || '';
                    span.setAttribute('http.url', url);
                    if (xhr.method) {
                        span.setAttribute('http.method', xhr.method);
                    }
                    if (xhr.status) {
                        span.setAttribute('http.status_code', xhr.status.toString());
                    }
                },
            }),
            new DocumentLoadInstrumentation(),
        ],
    });

    // Set up global error handler for uncaught errors
    setupGlobalErrorHandler();

    // Set up performance metrics collection
    setupPerformanceMetrics();

    initialized = true;
    console.info('[OTEL] Browser tracing initialized →', collectorUrl);
}

/**
 * Set up global error handler for uncaught errors.
 * Reports JavaScript errors to OpenTelemetry.
 */
function setupGlobalErrorHandler(): void {
    // Handle uncaught JavaScript errors
    window.addEventListener('error', (event) => {
        const tracer = getTracer();
        const span = tracer.startSpan('js.error.uncaught');

        span.setAttribute('error.name', 'UncaughtError');
        span.setAttribute('error.message', event.message || 'Unknown error');
        span.setAttribute('location.filename', event.filename || '');
        span.setAttribute('location.lineno', event.lineno?.toString() || '');
        span.setAttribute('location.colno', event.colno?.toString() || '');

        if (event.error?.stack) {
            span.setAttribute('error.stack', event.error.stack);
        }

        // Add session info (anonymous)
        const sessionInfo = getSessionInfo();
        span.setAttribute('session.id', sessionInfo.sessionId);

        span.end();
    });

    // Handle unhandled promise rejections
    window.addEventListener('unhandledrejection', (event) => {
        const tracer = getTracer();
        const span = tracer.startSpan('js.error.promise-rejection');

        span.setAttribute('error.name', 'UnhandledPromiseRejection');
        span.setAttribute('error.message', event.reason?.message || String(event.reason) || 'Unhandled rejection');

        if (event.reason?.stack) {
            span.setAttribute('error.stack', event.reason.stack);
        }

        // Add session info (anonymous)
        const sessionInfo = getSessionInfo();
        span.setAttribute('session.id', sessionInfo.sessionId);

        span.end();

        // Prevent default browser handling
        event.preventDefault();
    });
}

/**
 * Set up performance metrics collection.
 * Collects Core Web Vitals and reports them as spans.
 */
function setupPerformanceMetrics(): void {
    // Wait for page to fully load
    if (document.readyState === 'complete') {
        reportPerformanceMetrics();
    } else {
        window.addEventListener('load', reportPerformanceMetrics);
    }
}

/**
 * Report Core Web Vitals as OpenTelemetry spans.
 */
function reportPerformanceMetrics(): void {
    // Use setTimeout to ensure all metrics are available
    setTimeout(() => {
        const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
        const paint = performance.getEntriesByType('paint');

        const tracer = getTracer();

        // Report page load metrics
        if (navigation) {
            const span = tracer.startSpan('page.load');

            span.setAttribute('page.load.time_to_first_byte', navigation.responseStart.toString());
            span.setAttribute('page.load.dom_content_loaded', navigation.domContentLoadedEventEnd.toString());
            span.setAttribute('page.load.dom_complete', navigation.domComplete.toString());
            span.setAttribute('page.load.load_event_end', navigation.loadEventEnd.toString());
            span.setAttribute('page.load.total', (navigation.loadEventEnd - navigation.startTime).toString());

            // Calculate First Contentful Paint
            const fcp = paint.find(entry => entry.name === 'first-contentful-paint');
            if (fcp) {
                span.setAttribute('page.fcp', fcp.startTime.toString());
            }

            span.end();
        }

        // Report resource timing for API calls
        const resources = performance.getEntriesByType('resource');
        const apiResources = resources.filter(resource =>
            resource.name.includes('/api/') || resource.name.includes('localhost')
        );

        apiResources.forEach(resource => {
            const span = tracer.startSpan('resource.load');

            span.setAttribute('resource.name', resource.name);
            span.setAttribute('resource.duration', resource.duration.toString());
            span.setAttribute('resource.size', (resource as PerformanceResourceTiming).transferSize?.toString() || '0');

            span.end();
        });

    }, 0);
}

/**
 * Get the global tracer for creating manual spans.
 *
 * @example
 * ```ts
 * const span = getTracer().startSpan('upload-file');
 * try {
 *   await uploadFile(file);
 * } finally {
 *   span.end();
 * }
 * ```
 */
export function getTracer() {
    return trace.getTracer('ms-fe', '0.1.0');
}

/**
 * Get the current trace context for propagation.
 * Useful for adding traceparent to custom headers.
 */
export function getTraceHeaders(): Record<string, string> {
    const headers: Record<string, string> = {};
    propagation.inject(context.active(), headers);
    return headers;
}

/**
 * Start a custom span for tracking specific operations.
 * Automatically adds session info.
 *
 * @example
 * ```ts
 * const span = startCustomSpan('user-upload');
 * try {
 *   await uploadFile(file);
 * } finally {
 *   span.end();
 * }
 * ```
 */
export function startCustomSpan(name: string, attributes?: Record<string, string>): ReturnType<typeof getTracer>['startSpan'] {
    const tracer = getTracer();
    const span = tracer.startSpan(name);

    // Add session info (anonymous - no PII)
    const sessionInfo = getSessionInfo();
    span.setAttribute('session.id', sessionInfo.sessionId);

    // Add custom attributes
    if (attributes) {
        Object.entries(attributes).forEach(([key, value]) => {
            span.setAttribute(key, value);
        });
    }

    return span;
}
