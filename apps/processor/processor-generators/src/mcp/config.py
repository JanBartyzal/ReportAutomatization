"""MCP-specific configuration -- re-exports from common config."""

from src.common.config import (
    AZURE_CLIENT_ID,
    AZURE_CLIENT_SECRET,
    AZURE_TENANT_ID,
    DAPR_HOST,
    DAPR_HTTP_PORT,
    DAPR_STATESTORE_NAME,
    DB_HOST,
    DB_NAME,
    DB_PASSWORD,
    DB_PORT,
    DB_USER,
)

__all__ = [
    "AZURE_CLIENT_ID",
    "AZURE_CLIENT_SECRET",
    "AZURE_TENANT_ID",
    "DAPR_HOST",
    "DAPR_HTTP_PORT",
    "DAPR_STATESTORE_NAME",
    "DB_HOST",
    "DB_NAME",
    "DB_PASSWORD",
    "DB_PORT",
    "DB_USER",
]
