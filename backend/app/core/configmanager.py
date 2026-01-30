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