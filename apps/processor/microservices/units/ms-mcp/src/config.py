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


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-mcp")

HTTP_PORT: int = _int_env("HTTP_PORT", 8000)

# Database (read-only via ms_qry role)
DB_HOST: str = os.environ.get("DB_HOST", "localhost")
DB_PORT: int = _int_env("DB_PORT", 5432)
DB_NAME: str = os.environ.get("DB_NAME", "reportplatform")
DB_USER: str = os.environ.get("DB_USER", "ms_qry")
DB_PASSWORD: str = os.environ.get("DB_PASSWORD", "ms_qry_pass")

# Azure Entra ID (OBO flow)
AZURE_TENANT_ID: str = os.environ.get("AZURE_TENANT_ID", "common")
AZURE_CLIENT_ID: str = os.environ.get("AZURE_CLIENT_ID", "")
AZURE_CLIENT_SECRET: str = os.environ.get("AZURE_CLIENT_SECRET", "")

# Dapr
DAPR_HOST: str = os.environ.get("DAPR_HOST", "localhost")
DAPR_HTTP_PORT: int = _int_env("DAPR_HTTP_PORT", 3500)
DAPR_GRPC_PORT: int = _int_env("DAPR_GRPC_PORT", 50001)
DAPR_STATESTORE_NAME: str = os.environ.get("DAPR_STATESTORE_NAME", "reportplatform-statestore")

# Log level
LOG_LEVEL: str = os.environ.get("LOG_LEVEL", "INFO")
