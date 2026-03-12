"""PPTX atomizer configuration.

All PPTX-specific settings are accessed via the shared Settings object.
This module provides backward-compatible access for PPTX service internals.
"""

from __future__ import annotations

from src.common.config import Settings

# Default settings instance (can be overridden at service init)
_settings = Settings()

SERVICE_NAME: str = _settings.service_name
GRPC_PORT: int = _settings.grpc_port
BLOB_STORAGE_URL: str = _settings.blob_storage_url
AZURE_STORAGE_CONNECTION_STRING: str | None = _settings.azure_storage_connection_string
BLOB_CONTAINER: str = _settings.blob_container
ARTIFACTS_CONTAINER: str = _settings.artifacts_container
LIBREOFFICE_BIN: str = _settings.libreoffice_bin
RENDER_WIDTH: int = _settings.render_width
RENDER_HEIGHT: int = _settings.render_height
METATABLE_CONFIDENCE_THRESHOLD: float = _settings.metatable_confidence_threshold
