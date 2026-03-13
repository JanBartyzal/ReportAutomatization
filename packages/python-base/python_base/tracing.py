"""Centralized OpenTelemetry tracing setup for Python microservices."""

from __future__ import annotations

import logging
import os
from contextlib import contextmanager
from typing import Any, Generator

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

logger = logging.getLogger(__name__)

_initialized = False


def setup_tracing(
    service_name: str,
    otlp_endpoint: str | None = None,
) -> TracerProvider:
    """Initialize OpenTelemetry tracing for a Python microservice.

    Configures a TracerProvider with OTLP gRPC exporter pointing to the
    OpenTelemetry Collector. Safe to call multiple times — subsequent
    calls are no-ops.

    Args:
        service_name: Name of the microservice (e.g. "ms-atm-pptx").
        otlp_endpoint: OTLP gRPC endpoint. Defaults to OTEL_EXPORTER_OTLP_ENDPOINT
                       env var, then "http://otel-collector:4317".

    Returns:
        The configured TracerProvider.
    """
    global _initialized
    if _initialized:
        provider = trace.get_tracer_provider()
        if isinstance(provider, TracerProvider):
            return provider
        return TracerProvider()

    endpoint = otlp_endpoint or os.getenv(
        "OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector:4317"
    )

    resource_attrs = {
        "service.name": service_name,
        "service.namespace": "reportplatform",
        "deployment.environment": os.getenv("DEPLOYMENT_ENV", "local"),
    }
    # Merge with OTEL_RESOURCE_ATTRIBUTES if set
    otel_attrs = os.getenv("OTEL_RESOURCE_ATTRIBUTES", "")
    if otel_attrs:
        for pair in otel_attrs.split(","):
            if "=" in pair:
                k, v = pair.split("=", 1)
                resource_attrs[k.strip()] = v.strip()

    resource = Resource.create(resource_attrs)

    provider = TracerProvider(resource=resource)
    exporter = OTLPSpanExporter(endpoint=endpoint, insecure=True)
    provider.add_span_processor(BatchSpanProcessor(exporter))

    trace.set_tracer_provider(provider)
    _initialized = True

    logger.info(
        "OpenTelemetry tracing initialized for '%s' → %s",
        service_name,
        endpoint,
    )
    return provider


def get_tracer(name: str = __name__) -> trace.Tracer:
    """Get a tracer from the global TracerProvider.

    Args:
        name: Tracer name, typically the module name.

    Returns:
        An OpenTelemetry Tracer instance.
    """
    return trace.get_tracer(name)


@contextmanager
def create_span(
    name: str,
    attributes: dict[str, Any] | None = None,
) -> Generator[trace.Span, None, None]:
    """Context manager to create and manage an OpenTelemetry span.

    Usage::

        with create_span("process_file", {"file_id": file_id}) as span:
            # ... processing logic ...
            span.set_attribute("file_size", size)

    Args:
        name: Span name (e.g. "process_file", "orchestrator.parse").
        attributes: Optional dict of span attributes.

    Yields:
        The active Span.
    """
    tracer = get_tracer()
    with tracer.start_as_current_span(name, attributes=attributes) as span:
        yield span


def file_processing_span(
    operation: str,
    file_id: str,
    org_id: str = "",
    user_id: str = "",
) -> Any:
    """Create a span for file processing operations.

    Args:
        operation: Operation name (e.g. "parse", "convert", "upload").
        file_id: File identifier for trace search.
        org_id: Organization/tenant ID.
        user_id: User identifier.

    Returns:
        Context manager yielding the span.
    """
    return create_span(
        f"file.{operation}",
        attributes={
            "file_id": file_id,
            "org_id": org_id,
            "user_id": user_id,
        },
    )


def shutdown_tracing() -> None:
    """Flush pending spans and shut down the TracerProvider."""
    global _initialized
    provider = trace.get_tracer_provider()
    if isinstance(provider, TracerProvider):
        provider.shutdown()
        _initialized = False
        logger.info("OpenTelemetry tracing shut down.")
