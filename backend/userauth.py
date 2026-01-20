from fastapi import FastAPI, Depends, Security, HTTPException
from fastapi_azure_auth import SingleTenantAzureAuthorizationCodeBearer
from pydantic import BaseModel, Field
import os
from typing import Optional
from models import User


api_env = os.getenv("API_ENV", "production")
ProdMode=False
if api_env=="production":
    ProdMode=True
azure_scheme = SingleTenantAzureAuthorizationCodeBearer(app_client_id=os.getenv("AZURE_CLIENT_ID"),
    tenant_id=os.getenv("AZURE_TENANT_ID"),scopes={ f"api://{os.getenv('AZURE_CLIENT_ID')}/user_impersonation": "Access API" })


async def get_current_user():
    if ProdMode:
        return await get_current_azure_user()
    else:
        return User(oid="local-dev-user", name="Developer", email="dev@local", roles="AppAdmin")


async def get_current_azure_user(token: dict = Security(azure_scheme)) -> User:
    return User(**token)


async def verify_admin(user: User = Depends(get_current_user)):
    if "AppAdmin" not in user.roles:
        raise HTTPException(status_code=403, detail="Not authorized")
    return user

