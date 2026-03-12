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


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-atm-pdf")

GRPC_PORT: int = _int_env("GRPC_PORT", 50051)

BLOB_STORAGE_URL: str = os.environ.get(
    "BLOB_STORAGE_URL",
    "http://127.0.0.1:10000/devstoreaccount1",
)

AZURE_STORAGE_CONNECTION_STRING: str | None = os.environ.get(
    "AZURE_STORAGE_CONNECTION_STRING",
)

BLOB_CONTAINER: str = os.environ.get("BLOB_CONTAINER", "files")

# OCR configuration
OCR_LANGUAGE: str = os.environ.get("OCR_LANGUAGE", "ces+eng+deu")
OCR_CONFIDENCE_THRESHOLD: float = float(os.environ.get("OCR_CONFIDENCE_THRESHOLD", "0.8"))

# Temporary output container for rendered images
TEMP_CONTAINER: str = os.environ.get("TEMP_CONTAINER", "temp")
