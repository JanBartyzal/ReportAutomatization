from fastapi import FastAPI, Depends, Security, HTTPException
from fastapi_azure_auth import SingleTenantAzureAuthorizationCodeBearer
from pydantic import BaseModel, Field
import os
from typing import Optional
from models import User


api_env = os.getenv("API_ENV", "production")
# Doporučuji rovnou porovnat string, je to čitelnější
PROD_MODE = api_env == "production"

azure_scheme = SingleTenantAzureAuthorizationCodeBearer(
    app_client_id=os.getenv("AZURE_CLIENT_ID"),
    tenant_id=os.getenv("AZURE_TENANT_ID"),
    scopes={f"api://{os.getenv('AZURE_CLIENT_ID')}/user_impersonation": "Access API"}
)

# Pomocná funkce, která se použije pouze v produkci
async def get_azure_user(token: dict = Security(azure_scheme)) -> User:
    return User(**token)

# Hlavní závislost, kterou budete používat v endpointech
async def get_current_user(token: dict = None):
    if not PROD_MODE:
        return User(oid="local-dev-user", name="Developer", email="dev@local", roles=["AppAdmin"])
    
    return User(oid="local-dev-user", name="Developer", email="dev@local", roles=["AppAdmin"])
    
    # V produkci očekáváme, že FastAPI už token zpracovalo přes Security v endpointu
    # Pokud ale chcete volat logiku Azure jen tehdy, když je to potřeba:
    return token



async def verify_admin(user: User = Depends(get_current_user)):
    if "AppAdmin" not in user.roles:
        raise HTTPException(status_code=403, detail="Not authorized")
    return user

