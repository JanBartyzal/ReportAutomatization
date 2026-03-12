"""OpenTelemetry TracerProvider setup with OTLP exporter."""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from opentelemetry import trace
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


def setup_tracing(settings: "Settings") -> None:
    """Configure the global OpenTelemetry TracerProvider.

    Args:
        settings: Application settings with OTEL endpoint and service name.
    """
    resource = Resource.create(
        {
            "service.name": settings.otel_service_name,
            "service.version": "0.1.0",
        }
    )

    provider = TracerProvider(resource=resource)

    try:
        exporter = OTLPSpanExporter(endpoint=settings.otel_endpoint, insecure=True)
        processor = BatchSpanProcessor(exporter)
        provider.add_span_processor(processor)
        logger.info("OTLP trace exporter configured: %s", settings.otel_endpoint)
    except Exception:
        logger.warning(
            "Failed to configure OTLP exporter at %s; tracing is disabled",
            settings.otel_endpoint,
            exc_info=True,
        )

    trace.set_tracer_provider(provider)
