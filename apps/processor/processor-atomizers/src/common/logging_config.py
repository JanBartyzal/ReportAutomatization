"""Structured JSON logging setup."""

from __future__ import annotations

import logging
import sys
from typing import TYPE_CHECKING

from pythonjsonlogger import jsonlogger

if TYPE_CHECKING:
    from src.common.config import Settings


def setup_logging(settings: "Settings") -> None:
    """Configure structured JSON logging for the application.

    Args:
        settings: Application settings with log level.
    """
    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)

    handler = logging.StreamHandler(sys.stdout)
    formatter = jsonlogger.JsonFormatter(
        fmt="%(asctime)s %(name)s %(levelname)s %(message)s",
        rename_fields={"asctime": "timestamp", "levelname": "level"},
    )
    handler.setFormatter(formatter)

    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)
    # Remove existing handlers to avoid duplicate output
    root_logger.handlers.clear()
    root_logger.addHandler(handler)

    # Reduce noise from third-party libraries
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)
    logging.getLogger("grpc").setLevel(logging.WARNING)
    logging.getLogger("azure").setLevel(logging.WARNING)
    logging.getLogger("httpx").setLevel(logging.WARNING)

    logging.info("Logging configured: level=%s service=%s", settings.log_level, settings.service_name)
