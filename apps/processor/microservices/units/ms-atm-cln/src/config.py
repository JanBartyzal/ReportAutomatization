"""Configuration loaded from environment variables."""

from __future__ import annotations

import os
from datetime import timedelta


def _int_env(key: str, default: int) -> int:
    raw = os.environ.get(key)
    if raw is None:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-atm-cln")

# Cron schedule (default: every hour)
CRON_SCHEDULE: str = os.environ.get("CRON_SCHEDULE", "0 * * * *")

# Dry run mode - when True, don't actually delete files
DRY_RUN: bool = os.environ.get("DRY_RUN", "false").lower() == "true"

# Azure Storage configuration
AZURE_STORAGE_CONNECTION_STRING: str | None = os.environ.get(
    "AZURE_STORAGE_CONNECTION_STRING",
)

# Cleanup age thresholds (in hours)
TEMP_PNG_AGE_HOURS: int = _int_env("TEMP_PNG_AGE_HOURS", 24)
TEMP_CSV_AGE_HOURS: int = _int_env("TEMP_CSV_AGE_HOURS", 24)
RAW_AGE_DAYS: int = _int_env("RAW_AGE_DAYS", 90)
GENERATOR_OUTPUT_AGE_HOURS: int = _int_env("GENERATOR_OUTPUT_AGE_HOURS", 24)

# Container names
TEMP_CONTAINER: str = os.environ.get("TEMP_CONTAINER", "temp")
FILES_CONTAINER: str = os.environ.get("FILES_CONTAINER", "files")
GENERATOR_CONTAINER: str = os.environ.get("GENERATOR_CONTAINER", "generator-output")

# Prefixes to clean
TEMP_PNG_PREFIX: str = os.environ.get("TEMP_PNG_PREFIX", "slides/")
TEMP_CSV_PREFIX: str = os.environ.get("TEMP_CSV_PREFIX", "exports/")
RAW_PREFIX: str = os.environ.get("RAW_PREFIX", "_raw/")
GENERATOR_PREFIX: str = os.environ.get("GENERATOR_PREFIX", "")
