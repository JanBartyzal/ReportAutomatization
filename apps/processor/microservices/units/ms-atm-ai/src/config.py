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


SERVICE_NAME: str = os.environ.get("SERVICE_NAME", "ms-atm-ai")

GRPC_PORT: int = _int_env("GRPC_PORT", 50051)

# LiteLLM proxy URL (OpenAI-compatible)
LITELLM_BASE_URL: str = os.environ.get("LITELLM_BASE_URL", "http://localhost:4000")
LITELLM_API_KEY: str = os.environ.get("LITELLM_API_KEY", "sk-local-dev-key")

# Model selection per operation type
MODEL_SEMANTIC: str = os.environ.get("MODEL_SEMANTIC", "gpt-4o")
MODEL_EMBEDDING: str = os.environ.get("MODEL_EMBEDDING", "text-embedding-3-small")

# Rate limiting: max concurrent AI requests per org
MAX_CONCURRENT_PER_ORG: int = _int_env("MAX_CONCURRENT_PER_ORG", 5)

# Quota: default monthly token limit per org
DEFAULT_MONTHLY_TOKEN_QUOTA: int = _int_env("DEFAULT_MONTHLY_TOKEN_QUOTA", 1_000_000)

# Dapr
DAPR_HOST: str = os.environ.get("DAPR_HOST", "localhost")
DAPR_HTTP_PORT: int = _int_env("DAPR_HTTP_PORT", 3500)
DAPR_STATESTORE_NAME: str = os.environ.get("DAPR_STATESTORE_NAME", "reportplatform-statestore")

# Prompts config path
PROMPTS_CONFIG_PATH: str = os.environ.get("PROMPTS_CONFIG_PATH", "src/prompts/prompts.yaml")
