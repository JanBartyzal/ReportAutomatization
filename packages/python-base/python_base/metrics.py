"""Prometheus metrics for Python microservices.

Provides custom metrics for gRPC request tracking and file processing,
plus an HTTP server to expose /metrics for Prometheus scraping.
"""

from __future__ import annotations

import logging
import os
import time
from collections.abc import Callable
from contextlib import contextmanager
from typing import Any, Generator

from prometheus_client import (
    CollectorRegistry,
    Counter,
    Histogram,
    Info,
    generate_latest,
    start_http_server,
)

logger = logging.getLogger(__name__)

# Shared registry for all metrics
REGISTRY = CollectorRegistry()

# --- Service info ---
SERVICE_INFO = Info(
    "reportplatform_service",
    "Service metadata",
    registry=REGISTRY,
)

# --- gRPC request metrics ---
GRPC_REQUEST_DURATION = Histogram(
    "reportplatform_grpc_request_duration_seconds",
    "Duration of gRPC requests in seconds",
    ["service", "method", "status"],
    buckets=(0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0),
    registry=REGISTRY,
)

GRPC_REQUESTS_TOTAL = Counter(
    "reportplatform_grpc_requests_total",
    "Total number of gRPC requests",
    ["service", "method", "status"],
    registry=REGISTRY,
)

# --- File processing metrics ---
FILE_PROCESSING_DURATION = Histogram(
    "reportplatform_file_processing_duration_seconds",
    "Duration of file processing (atomizer) in seconds",
    ["service", "file_type", "status"],
    buckets=(0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0),
    registry=REGISTRY,
)

FILE_PROCESSING_TOTAL = Counter(
    "reportplatform_file_processing_total",
    "Total number of files processed",
    ["service", "file_type", "status"],
    registry=REGISTRY,
)

FILE_PROCESSING_BYTES = Histogram(
    "reportplatform_file_processing_bytes",
    "Size of processed files in bytes",
    ["service", "file_type"],
    buckets=(1024, 10240, 102400, 1048576, 10485760, 52428800, 104857600),
    registry=REGISTRY,
)


def init_service_info(service_name: str) -> None:
    """Set service metadata labels."""
    SERVICE_INFO.info({
        "name": service_name,
        "version": os.getenv("SERVICE_VERSION", "0.1.0"),
        "environment": os.getenv("DEPLOYMENT_ENVIRONMENT", "local"),
    })


@contextmanager
def track_grpc_request(
    service: str,
    method: str,
) -> Generator[None, None, None]:
    """Context manager to track gRPC request duration and count.

    Usage::

        with track_grpc_request("ms-atm-pptx", "ProcessFile"):
            result = await process(request)
    """
    start = time.perf_counter()
    status = "ok"
    try:
        yield
    except Exception:
        status = "error"
        raise
    finally:
        duration = time.perf_counter() - start
        GRPC_REQUEST_DURATION.labels(service=service, method=method, status=status).observe(duration)
        GRPC_REQUESTS_TOTAL.labels(service=service, method=method, status=status).inc()


@contextmanager
def track_file_processing(
    service: str,
    file_type: str,
    file_size_bytes: int | None = None,
) -> Generator[None, None, None]:
    """Context manager to track file processing duration and outcome.

    Usage::

        with track_file_processing("ms-atm-pptx", "pptx", file_size_bytes=len(data)):
            atoms = atomize(data)
    """
    if file_size_bytes is not None:
        FILE_PROCESSING_BYTES.labels(service=service, file_type=file_type).observe(file_size_bytes)
    start = time.perf_counter()
    status = "success"
    try:
        yield
    except Exception:
        status = "failure"
        raise
    finally:
        duration = time.perf_counter() - start
        FILE_PROCESSING_DURATION.labels(service=service, file_type=file_type, status=status).observe(duration)
        FILE_PROCESSING_TOTAL.labels(service=service, file_type=file_type, status=status).inc()


def start_metrics_server(port: int = 9090) -> None:
    """Start an HTTP server exposing /metrics for Prometheus scraping.

    Args:
        port: Port to listen on (default 9090).
    """
    start_http_server(port, registry=REGISTRY)
    logger.info("Prometheus metrics server started on port %d", port)
