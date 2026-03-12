"""Re-export shared context extraction for PPTX atomizer."""

from src.common.context import RequestContext, extract_context_from_grpc

__all__ = [
    "RequestContext",
    "extract_context_from_grpc",
]
