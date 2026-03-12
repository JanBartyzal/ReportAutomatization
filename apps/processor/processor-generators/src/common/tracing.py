"""OpenTelemetry tracing setup for processor-generators."""

from __future__ import annotations

import logging

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

from src.common.config import OTEL_EXPORTER_OTLP_ENDPOINT, SERVICE_NAME

logger = logging.getLogger(__name__)


def setup_tracing() -> None:
    """Initialize OpenTelemetry tracing with OTLP exporter."""
    resource = Resource.create({"service.name": SERVICE_NAME})
    provider = TracerProvider(resource=resource)

    exporter = OTLPSpanExporter(endpoint=OTEL_EXPORTER_OTLP_ENDPOINT)
    provider.add_span_processor(BatchSpanProcessor(exporter))

    trace.set_tracer_provider(provider)
    logger.info(
        "Tracing initialized: service=%s endpoint=%s",
        SERVICE_NAME,
        OTEL_EXPORTER_OTLP_ENDPOINT,
    )


def get_tracer(name: str | None = None) -> trace.Tracer:
    """Get a tracer instance."""
    return trace.get_tracer(name or SERVICE_NAME)
