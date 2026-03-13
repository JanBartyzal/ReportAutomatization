"""PPTX-specific configuration – re-exports from common config."""

from src.common.config import (
    CHART_DPI,
    CHART_HEIGHT_INCHES,
    CHART_WIDTH_INCHES,
    MAX_BATCH_SIZE,
    GENERATION_TIMEOUT,
)

__all__ = [
    "CHART_DPI",
    "CHART_HEIGHT_INCHES",
    "CHART_WIDTH_INCHES",
    "MAX_BATCH_SIZE",
    "GENERATION_TIMEOUT",
]
