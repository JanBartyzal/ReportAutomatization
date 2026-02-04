import os
from typing import List
from pydantic_settings import BaseSettings
from pydantic import Field, PostgresDsn, RedisDsn, validator
import os
import logging
from typing import Optional
from azure.identity import DefaultAzureCredential
from azure.keyvault.secrets import SecretClient
from app.core.models import SystemConfig
from app.core.database import SessionLocal

logger = logging.getLogger(__name__)


class Settings(BaseSettings):
    """Application configuration loaded from environment variables.
    
    All configuration values must be provided via environment variables.
    No default credentials or secrets are allowed.
    
    Attributes:
        database_url: PostgreSQL connection string (required)
        redis_url: Redis connection string (required)
        azure_client_id: Azure AD application client ID
        azure_tenant_id: Azure AD tenant ID
        api_env: Environment mode ('development' or 'production')
        cors_origins: Comma-separated list of allowed CORS origins
        model_name: LLM model name for embeddings and completions
        ocr_confidence_threshold: Minimum OCR confidence score (0-100)
        cache_ttl_days: Redis cache TTL in days
        upload_dir: Directory for file uploads
    """
    
    # Database Configuration (NO DEFAULTS - MUST BE SET)
    database_url: PostgresDsn = Field(
        ...,
        description="PostgreSQL connection URL",
        validation_alias="DATABASE_URL"
    )
    
    # Redis Configuration (NO DEFAULTS - MUST BE SET)
    redis_url: RedisDsn = Field(
        ...,
        description="Redis connection URL",
        validation_alias="REDIS_URL"
    )
    
    # Azure Authentication
    azure_client_id: str = Field(
        ...,
        description="Azure AD application client ID",
        validation_alias="AZURE_CLIENT_ID"
    )
    
    azure_tenant_id: str = Field(
        ...,
        description="Azure AD tenant ID",
        validation_alias="AZURE_TENANT_ID"
    )
    
    # Application Settings
    api_env: str = Field(
        default="production",
        description="Environment mode: 'development' or 'production'",
        validation_alias="API_ENV"
    )
    
    cors_origins_str: str = Field(
        default="http://localhost:5173,http://127.0.0.1:5173",
        description="Comma-separated CORS origins (default includes Vite port 5173)",
        validation_alias="CORS_ORIGINS"
    )
    
    # AI/LLM Configuration
    model_name: str = Field(
        default="text-embedding-ada-002",
        description="LiteLLM model name for embeddings",
        validation_alias="MODEL_NAME"
    )
    
    # OCR Configuration
    ocr_confidence_threshold: int = Field(
        default=85,
        ge=0,
        le=100,
        description="Minimum OCR confidence score (0-100)",
        validation_alias="OCR_CONFIDENCE_THRESHOLD"
    )
    
    # Ollama Configuration
    ollama_base_url: str = Field(
        default="http://ollama:11434",
        description="Ollama service base URL",
        validation_alias="OLLAMA_BASE_URL"
    )
    
    ollama_ocr_model: str = Field(
        default="deepseek-ocr:latest",
        description="Ollama model for OCR and table extraction",
        validation_alias="OLLAMA_OCR_MODEL"
    )
    
    ollama_timeout: int = Field(
        default=120,
        ge=10,
        description="Ollama API timeout in seconds",
        validation_alias="OLLAMA_TIMEOUT"
    )
    
    # Cache Configuration
    cache_ttl_days: int = Field(
        default=7,
        ge=1,
        description="Redis cache TTL in days",
        validation_alias="CACHE_TTL_DAYS"
    )
    
    # File Storage
    upload_dir: str = Field(
        default="local_data/uploads",
        description="Directory for file uploads",
        validation_alias="UPLOAD_DIR"
    )

    # Security / JWT
    secret_key: str = Field(
        default="insecure-secret-key-for-dev", 
        description="Secret key for JWT encoding",
        validation_alias="SECRET_KEY"
    )
    algorithm: str = Field(
        default="HS256", 
        description="Algorithm for JWT encoding",
        validation_alias="ALGORITHM"
    )
    access_token_expire_minutes: int = Field(
        default=60, 
        description="Access token expiration in minutes",
        validation_alias="ACCESS_TOKEN_EXPIRE_MINUTES"
    )

    
    @property
    def cors_origins(self) -> List[str]:
        """Parse CORS origins from comma-separated string."""
        return [origin.strip() for origin in self.cors_origins_str.split(",")]
    
    @property
    def is_production(self) -> bool:
        """Check if running in production mode."""
        return self.api_env.lower() == "production"
    
    @property
    def cache_ttl_seconds(self) -> int:
        """Convert cache TTL from days to seconds."""
        return self.cache_ttl_days * 24 * 60 * 60
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False

# Global settings instance
settings = Settings()
