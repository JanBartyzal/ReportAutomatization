"""RequestContext dataclass extracted from gRPC metadata.

Re-exports the shared ``RequestContext`` from ``python_base``
and provides a convenience helper for extracting it from gRPC service contexts.
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
    """Extract a ``RequestContext`` from a gRPC servicer invocation context."""
    metadata = grpc_context.invocation_metadata() or []
    return extract_context_from_metadata(metadata)
