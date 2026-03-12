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


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-atm-pptx")

GRPC_PORT: int = _int_env("GRPC_PORT", 50051)

BLOB_STORAGE_URL: str = os.environ.get(
    "BLOB_STORAGE_URL",
    "http://127.0.0.1:10000/devstoreaccount1",
)

AZURE_STORAGE_CONNECTION_STRING: str | None = os.environ.get(
    "AZURE_STORAGE_CONNECTION_STRING",
)

BLOB_CONTAINER: str = os.environ.get("BLOB_CONTAINER", "files")

ARTIFACTS_CONTAINER: str = os.environ.get("ARTIFACTS_CONTAINER", "artifacts")

# LibreOffice binary path override (useful in containers)
LIBREOFFICE_BIN: str = os.environ.get("LIBREOFFICE_BIN", "libreoffice")

# Slide render dimensions
RENDER_WIDTH: int = _int_env("RENDER_WIDTH", 1280)
RENDER_HEIGHT: int = _int_env("RENDER_HEIGHT", 720)

# MetaTable confidence threshold
METATABLE_CONFIDENCE_THRESHOLD: float = float(
    os.environ.get("METATABLE_CONFIDENCE_THRESHOLD", "0.85")
)
