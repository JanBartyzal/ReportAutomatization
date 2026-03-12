"""Structured logging configuration for processor-generators."""

from __future__ import annotations

import logging
import sys

from src.common.config import LOG_LEVEL, SERVICE_NAME


def setup_logging() -> None:
    """Configure structured logging for the service."""
    log_format = (
        "%(asctime)s | %(levelname)-8s | %(name)s | %(message)s"
    )

    logging.basicConfig(
        level=getattr(logging, LOG_LEVEL.upper(), logging.INFO),
        format=log_format,
        stream=sys.stdout,
        force=True,
    )

    # Reduce noise from noisy libraries
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("matplotlib").setLevel(logging.WARNING)

    logger = logging.getLogger(__name__)
    logger.info("Logging configured: service=%s level=%s", SERVICE_NAME, LOG_LEVEL)
