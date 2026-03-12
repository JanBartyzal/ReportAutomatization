"""Re-export shared context extraction for PDF atomizer."""

from src.common.context import RequestContext, extract_context_from_grpc

__all__ = ["RequestContext", "extract_context_from_grpc"]
