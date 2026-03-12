"""RequestContext extraction from gRPC metadata.

This module re-exports the shared ``RequestContext`` from ``python_base``
and provides a convenience helper for extracting it from gRPC service contexts.
Shared by all atomizer services.
"""

from __future__ import annotations

from typing import TYPE_CHECKING

from python_base import RequestContext, extract_context_from_metadata

if TYPE_CHECKING:
    import grpc

__all__ = [
    "RequestContext",
    "extract_context_from_grpc",
]


def extract_context_from_grpc(grpc_context: grpc.aio.ServicerContext) -> RequestContext:
    """Extract a ``RequestContext`` from a gRPC servicer invocation context.

    Args:
        grpc_context: The gRPC async servicer context carrying invocation metadata.

    Returns:
        A populated ``RequestContext`` instance.
    """
    metadata = grpc_context.invocation_metadata() or []
    return extract_context_from_metadata(metadata)
