"""Configuration loaded from environment variables."""

from __future__ import annotations

import os


def _int_env(key: str, default: int) -> int:
    raw = os.environ.get(key)
    if raw is None:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-gen-pptx")

GRPC_PORT: int = _int_env("GRPC_PORT", 50051)

BLOB_STORAGE_URL: str = os.environ.get(
    "BLOB_STORAGE_URL",
    "http://127.0.0.1:10000/devstoreaccount1",
)

AZURE_STORAGE_CONNECTION_STRING: str | None = os.environ.get(
    "AZURE_STORAGE_CONNECTION_STRING",
)

TEMPLATES_CONTAINER: str = os.environ.get("TEMPLATES_CONTAINER", "templates")

GENERATED_CONTAINER: str = os.environ.get("GENERATED_CONTAINER", "generated-reports")

# Chart rendering
CHART_DPI: int = _int_env("CHART_DPI", 150)
CHART_WIDTH_INCHES: float = float(os.environ.get("CHART_WIDTH_INCHES", "8.0"))
CHART_HEIGHT_INCHES: float = float(os.environ.get("CHART_HEIGHT_INCHES", "5.0"))

# Batch limits
MAX_BATCH_SIZE: int = _int_env("MAX_BATCH_SIZE", 50)
