import os
from typing import List
from pydantic_settings import BaseSettings
from pydantic import Field, PostgresDsn, RedisDsn, validator
import os
import logging
from typing import Optional
from azure.identity import DefaultAzureCredential
from azure.keyvault.secrets import SecretClient
from .models import SystemConfig
from .database import SessionLocal

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
    
    @property
    def cors_origins(self) -> List[str]:
        """Parse CORS origins from comma-separated string."""
        return [origin.strip() for origin in self.cors_origins_str.split(",")]
    
    @property
    def is_production(self) -> bool:
        """Check if running in production mode."""
        return self.api_env == "production"
    
    @property
    def cache_ttl_seconds(self) -> int:
        """Convert cache TTL from days to seconds."""
        return self.cache_ttl_days * 24 * 60 * 60
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False




class ConfigManager:
    _secret_client = None
    
    @classmethod
    def get_secret_client(cls):
        if cls._secret_client:
            return cls._secret_client
            
        kv_url = os.getenv("AZURE_KEYVAULT_URL")
        if kv_url:
            try:
                credential = DefaultAzureCredential()
                cls._secret_client = SecretClient(vault_url=kv_url, credential=credential)
                logger.info(f"Initialized KeyVault client for {kv_url}")
            except Exception as e:
                logger.error(f"Failed to initialize KeyVault client: {e}")
        return cls._secret_client

    @staticmethod
    def get_key(key_name: str, default: str = None) -> str:
        """
        Retrieves a configuration value/secret.
        Priority:
        1. Environment Variable
        2. Database SystemConfig
        3. Azure Key Vault
        4. Default value
        """
        # 1. Environment Variable
        val = os.getenv(key_name)
        if val is not None:
            return val
            
        # 2. Database SystemConfig
        # Note: This creates a DB connection per call if not cached. 
        # Ideally we should cache this or pass session. 
        # For simple usage, we do a quick check.
        try:
            with SessionLocal() as db:
                config_item = db.query(SystemConfig).filter(SystemConfig.key == key_name).first()
                if config_item and config_item.value:
                    return config_item.value
        except Exception as e:
            # Table might not exist yet if migration didn't run
            # logger.debug(f"Failed to read from SystemConfig DB: {e}")
            pass

        # 3. Azure Key Vault
        client = ConfigManager.get_secret_client()
        if client:
            try:
                # KeyVault keys cannot have underscores usually, or handled differently.
                # Standard conversion: MY_SECRET -> MY-SECRET ?
                # We try exact match first.
                secret = client.get_secret(key_name)
                if secret.value:
                    return secret.value
            except Exception:
                # Try replacing underscores with dashes (common KV pattern)
                try:
                    kv_key = key_name.replace("_", "-")
                    secret = client.get_secret(kv_key)
                    if secret.value:
                        return secret.value
                except Exception:
                    pass
        
        return default

# Helper function alias
def Get_Key(key_name: str, default: str = None) -> str:
    return ConfigManager.get_key(key_name, default)


# Global settings instance
settings = Settings()
