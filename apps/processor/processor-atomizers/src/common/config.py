"""Shared configuration using pydantic-settings.

All settings are loaded from environment variables with sensible defaults
for local development. Atomizer-specific settings extend this base.
"""

from __future__ import annotations

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Unified settings for the processor-atomizers service."""

    # -- Service identity --
    service_name: str = "processor-atomizers"

    # -- Dapr --
    dapr_host: str = "localhost"
    dapr_http_port: int = 3500
    dapr_grpc_port: int = 50001
    dapr_statestore_name: str = "reportplatform-statestore"

    # -- Azure Blob Storage --
    blob_storage_url: str = "http://127.0.0.1:10000/devstoreaccount1"
    azure_storage_connection_string: str | None = None
    blob_container: str = Field(default="file-uploads", validation_alias="AZURE_STORAGE_CONTAINER")
    artifacts_container: str = "artifacts"
    temp_container: str = "temp"

    # -- Server ports --
    grpc_port: int = 50090
    http_port: int = 8088

    # -- Logging & Observability --
    log_level: str = "INFO"
    otel_endpoint: str = "http://localhost:4317"
    otel_service_name: str = "processor-atomizers"

    # -- PPTX-specific --
    libreoffice_bin: str = "libreoffice"
    render_width: int = 1280
    render_height: int = 720
    metatable_confidence_threshold: float = 0.85

    # -- Excel-specific --
    empty_row_threshold: int = 0

    # -- PDF/OCR-specific --
    ocr_language: str = "ces+eng+deu"
    ocr_confidence_threshold: float = 0.8

    # -- CSV-specific --
    max_rows_to_sample: int = 1000

    # -- AI-specific --
    litellm_base_url: str = "http://localhost:4000"
    litellm_api_key: str = "sk-local-dev-key"
    model_semantic: str = "gpt-4o"
    model_embedding: str = "text-embedding-3-small"
    max_concurrent_per_org: int = 5
    default_monthly_token_quota: int = 1_000_000
    prompts_config_path: str = "src/atomizers/ai/prompts/prompts.yaml"

    # -- Cleanup-specific --
    cleanup_interval_hours: int = 6
    cleanup_dry_run: bool = False
    temp_png_age_hours: int = 24
    temp_csv_age_hours: int = 24
    raw_age_days: int = 90
    generator_output_age_hours: int = 24
    files_container: str = "files"
    generator_container: str = "generator-output"
    temp_png_prefix: str = "slides/"
    temp_csv_prefix: str = "exports/"
    raw_prefix: str = "_raw/"
    generator_prefix: str = ""

    class Config:
        env_prefix = ""
        case_sensitive = False
        env_file = ".env"
        extra = "ignore"
