"""Request context propagation for gRPC and async services."""

from __future__ import annotations

import uuid
from contextvars import ContextVar
from dataclasses import dataclass, field
from typing import Any, Sequence

_request_context_var: ContextVar[RequestContext | None] = ContextVar("request_context", default=None)


@dataclass(frozen=True, slots=True)
class RequestContext:
    """Immutable request context propagated through gRPC metadata and async tasks.

    Attributes:
        trace_id: Distributed tracing identifier (OpenTelemetry trace ID).
        user_id: Authenticated user identifier from Azure Entra ID.
        org_id: Organization/tenant identifier for multi-tenancy.
        roles: List of RBAC roles assigned to the user.
        correlation_id: Unique identifier for correlating related operations.
    """

    trace_id: str = ""
    user_id: str = ""
    org_id: str = ""
    roles: list[str] = field(default_factory=list)
    correlation_id: str = field(default_factory=lambda: str(uuid.uuid4()))


def extract_context_from_metadata(metadata: Sequence[tuple[str, Any]]) -> RequestContext:
    """Extract RequestContext from gRPC invocation metadata.

    Metadata keys follow the convention used by Dapr sidecars and
    OpenTelemetry propagation headers.

    Args:
        metadata: Sequence of (key, value) tuples from gRPC metadata.

    Returns:
        A populated RequestContext instance.
    """
    meta_dict: dict[str, str] = {}
    for key, value in metadata:
        meta_dict[key.lower()] = str(value) if not isinstance(value, str) else value

    roles_raw = meta_dict.get("x-roles", "")
    roles = [r.strip() for r in roles_raw.split(",") if r.strip()] if roles_raw else []

    # Extract trace_id from headers
    trace_id = meta_dict.get("x-trace-id", meta_dict.get("traceparent", ""))

    # Bridge W3C traceparent into OpenTelemetry context if available
    traceparent = meta_dict.get("traceparent", "")
    if traceparent:
        _set_otel_context_from_traceparent(meta_dict)

    return RequestContext(
        trace_id=trace_id,
        user_id=meta_dict.get("x-user-id", ""),
        org_id=meta_dict.get("x-org-id", ""),
        roles=roles,
        correlation_id=meta_dict.get("x-correlation-id", str(uuid.uuid4())),
    )


def _set_otel_context_from_traceparent(meta_dict: dict[str, str]) -> None:
    """Bridge W3C traceparent header into OpenTelemetry context.

    This allows traces arriving via Dapr gRPC metadata to be correctly
    linked in the OTEL trace hierarchy.
    """
    try:
        from opentelemetry.propagators.textmap import DefaultGetter
        from opentelemetry.propagate import extract
        from opentelemetry import context as otel_context

        # Extract OTEL context from the W3C traceparent header
        carrier = {k: v for k, v in meta_dict.items() if k in ("traceparent", "tracestate")}
        ctx = extract(carrier)
        otel_context.attach(ctx)
    except ImportError:
        pass  # OpenTelemetry not installed — skip


def get_request_context() -> RequestContext | None:
    """Retrieve the current request context from the async context variable.

    Returns:
        The current RequestContext, or None if not set.
    """
    return _request_context_var.get()


def set_request_context(ctx: RequestContext) -> None:
    """Store a RequestContext in the current async context variable.

    Args:
        ctx: The RequestContext to propagate through async call chains.
    """
    _request_context_var.set(ctx)
