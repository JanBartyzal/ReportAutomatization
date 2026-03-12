"""CSV atomizer configuration."""

from src.common.config import Settings

_settings = Settings()

SERVICE_NAME: str = _settings.service_name
GRPC_PORT: int = _settings.grpc_port
BLOB_STORAGE_URL: str = _settings.blob_storage_url
AZURE_STORAGE_CONNECTION_STRING: str | None = _settings.azure_storage_connection_string
BLOB_CONTAINER: str = _settings.blob_container
MAX_ROWS_TO_SAMPLE: int = _settings.max_rows_to_sample
