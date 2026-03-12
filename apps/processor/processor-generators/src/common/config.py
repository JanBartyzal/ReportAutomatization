"""Configuration loaded from environment variables.

Consolidates settings for PPTX generator, Excel generator, and MCP server.
"""

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


# ---------------------------------------------------------------------------
# Service identity
# ---------------------------------------------------------------------------
SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "processor-generators")

# ---------------------------------------------------------------------------
# Ports
# ---------------------------------------------------------------------------
GRPC_PORT: int = _int_env("GRPC_PORT", 50091)
HTTP_PORT: int = _int_env("HTTP_PORT", 8111)

# ---------------------------------------------------------------------------
# Blob Storage (shared by PPTX and Excel generators)
# ---------------------------------------------------------------------------
BLOB_STORAGE_URL: str = os.environ.get(
    "BLOB_STORAGE_URL",
    "http://127.0.0.1:10000/devstoreaccount1",
)

AZURE_STORAGE_CONNECTION_STRING: str | None = os.environ.get(
    "AZURE_STORAGE_CONNECTION_STRING",
)

TEMPLATES_CONTAINER: str = os.environ.get("TEMPLATES_CONTAINER", "templates")

GENERATED_CONTAINER: str = os.environ.get("GENERATED_CONTAINER", "generated-reports")

# ---------------------------------------------------------------------------
# Chart rendering (PPTX)
# ---------------------------------------------------------------------------
CHART_DPI: int = _int_env("CHART_DPI", 150)
CHART_WIDTH_INCHES: float = float(os.environ.get("CHART_WIDTH_INCHES", "8.0"))
CHART_HEIGHT_INCHES: float = float(os.environ.get("CHART_HEIGHT_INCHES", "5.0"))

# ---------------------------------------------------------------------------
# Batch limits
# ---------------------------------------------------------------------------
MAX_BATCH_SIZE: int = _int_env("MAX_BATCH_SIZE", 50)

# ---------------------------------------------------------------------------
# Database (MCP – read-only via ms_qry role)
# ---------------------------------------------------------------------------
DB_HOST: str = os.environ.get("DB_HOST", "localhost")
DB_PORT: int = _int_env("DB_PORT", 5432)
DB_NAME: str = os.environ.get("DB_NAME", "reportplatform")
DB_USER: str = os.environ.get("DB_USER", "ms_qry")
DB_PASSWORD: str = os.environ.get("DB_PASSWORD", "ms_qry_pass")

# ---------------------------------------------------------------------------
# Azure Entra ID (OBO flow – MCP auth)
# ---------------------------------------------------------------------------
AZURE_TENANT_ID: str = os.environ.get("AZURE_TENANT_ID", "common")
AZURE_CLIENT_ID: str = os.environ.get("AZURE_CLIENT_ID", "")
AZURE_CLIENT_SECRET: str = os.environ.get("AZURE_CLIENT_SECRET", "")

# ---------------------------------------------------------------------------
# Dapr
# ---------------------------------------------------------------------------
DAPR_HOST: str = os.environ.get("DAPR_HOST", "localhost")
DAPR_HTTP_PORT: int = _int_env("DAPR_HTTP_PORT", 3500)
DAPR_GRPC_PORT: int = _int_env("DAPR_GRPC_PORT", 50001)
DAPR_STATESTORE_NAME: str = os.environ.get("DAPR_STATESTORE_NAME", "reportplatform-statestore")

# ---------------------------------------------------------------------------
# Observability
# ---------------------------------------------------------------------------
OTEL_EXPORTER_OTLP_ENDPOINT: str = os.environ.get(
    "OTEL_EXPORTER_OTLP_ENDPOINT",
    "http://localhost:4317",
)

LOG_LEVEL: str = os.environ.get("LOG_LEVEL", "INFO")
