"""Structured JSON logging configuration for microservices."""

from __future__ import annotations

import json
import logging
import sys
from datetime import datetime, timezone
from typing import Any

from python_base.context import get_request_context


class JsonFormatter(logging.Formatter):
    """Structured JSON log formatter.

    Produces one JSON object per log line with fields:
    - timestamp (ISO 8601 UTC)
    - level
    - message
    - service
    - trace_id (from RequestContext if available)
    - correlation_id (from RequestContext if available)
    - logger
    - module
    - function
    - line
    """

    def __init__(self, service_name: str = "unknown") -> None:
        """Initialize the JSON formatter.

        Args:
            service_name: Name of the microservice, included in every log entry.
        """
        super().__init__()
        self._service_name = service_name

    def format(self, record: logging.LogRecord) -> str:
        """Format a log record as a JSON string.

        Args:
            record: The log record to format.

        Returns:
            A single-line JSON string.
        """
        log_entry: dict[str, Any] = {
            "timestamp": datetime.fromtimestamp(record.created, tz=timezone.utc).isoformat(),
            "level": record.levelname,
            "message": record.getMessage(),
            "service": self._service_name,
            "logger": record.name,
            "module": record.module,
            "function": record.funcName,
            "line": record.lineno,
        }

        # Enrich with request context if available
        ctx = get_request_context()
        if ctx is not None:
            log_entry["trace_id"] = ctx.trace_id
            log_entry["correlation_id"] = ctx.correlation_id
            log_entry["user_id"] = ctx.user_id
            log_entry["org_id"] = ctx.org_id

        # Enrich with OTEL span context for trace-log correlation
        try:
            from opentelemetry import trace as otel_trace

            span = otel_trace.get_current_span()
            span_ctx = span.get_span_context()
            if span_ctx and span_ctx.is_valid:
                log_entry.setdefault("trace_id", format(span_ctx.trace_id, "032x"))
                log_entry["span_id"] = format(span_ctx.span_id, "016x")
        except ImportError:
            pass

        # Include exception info if present
        if record.exc_info and record.exc_info[1] is not None:
            log_entry["exception"] = self.formatException(record.exc_info)

        # Include any extra fields added to the log record
        if hasattr(record, "extra_fields") and isinstance(record.extra_fields, dict):
            log_entry["extra"] = record.extra_fields

        return json.dumps(log_entry, default=str, ensure_ascii=False)


def setup_logging(
    service_name: str,
    level: int | str = logging.INFO,
    log_to_console: bool = True,
) -> None:
    """Configure structured JSON logging for a microservice.

    This should be called once at service startup, before any log output.

    Args:
        service_name: Name of the microservice (e.g. "ms-atm-pptx").
        level: Logging level (default INFO).
        log_to_console: Whether to attach a StreamHandler to stdout.
    """
    root_logger = logging.getLogger()

    # Clear existing handlers to avoid duplicate output
    root_logger.handlers.clear()

    root_logger.setLevel(level)

    if log_to_console:
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(JsonFormatter(service_name=service_name))
        root_logger.addHandler(handler)

    # Reduce noise from third-party libraries
    for noisy_logger in ("httpx", "httpcore", "grpc", "opentelemetry"):
        logging.getLogger(noisy_logger).setLevel(logging.WARNING)
