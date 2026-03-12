"""AI atomizer configuration."""

from src.common.config import Settings

_settings = Settings()

SERVICE_NAME: str = _settings.service_name
GRPC_PORT: int = _settings.grpc_port
LITELLM_BASE_URL: str = _settings.litellm_base_url
LITELLM_API_KEY: str = _settings.litellm_api_key
MODEL_SEMANTIC: str = _settings.model_semantic
MODEL_EMBEDDING: str = _settings.model_embedding
MAX_CONCURRENT_PER_ORG: int = _settings.max_concurrent_per_org
DEFAULT_MONTHLY_TOKEN_QUOTA: int = _settings.default_monthly_token_quota
DAPR_HOST: str = _settings.dapr_host
DAPR_HTTP_PORT: int = _settings.dapr_http_port
DAPR_STATESTORE_NAME: str = _settings.dapr_statestore_name
PROMPTS_CONFIG_PATH: str = _settings.prompts_config_path
