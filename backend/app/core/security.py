"""
User authentication and authorization using Azure Entra ID.

This module provides FastAPI dependencies for user authentication and role-based
access control (RBAC). In development mode, it returns a mock user for testing.
In production mode, it validates JWT tokens from Azure Entra ID.
"""

from fastapi import Depends, Security, HTTPException
from fastapi_azure_auth import SingleTenantAzureAuthorizationCodeBearer
from typing import Optional
from app.schemas.user import User
from app.core.config import settings


# Azure AD authentication scheme
azure_scheme = SingleTenantAzureAuthorizationCodeBearer(
    app_client_id=settings.azure_client_id,
    tenant_id=settings.azure_tenant_id,
    scopes={f"api://{settings.azure_client_id}/user_impersonation": "Access API"}
)


async def get_azure_user(token: dict = Security(azure_scheme)) -> User:
    """
    Extract user information from validated Azure AD JWT token.
    
    This function is only called in production mode after the token has been
    validated by the azure_scheme Security dependency.
    
    Args:
        token: Validated JWT token claims from Azure AD
        
    Returns:
        User: User object with OID, name, email, and roles
    """
    return User(**token)


async def get_current_user(
    azure_user: Optional[User] = Security(azure_scheme)
) -> User:
    """
    Get the current authenticated user.
    
    In development mode, returns a mock admin user for testing.
    In production mode, validates the Azure AD token and returns the authenticated user.
    
    This is the main dependency to use in route handlers for authentication.
    
    Args:
        azure_user: User extracted from Azure AD token (only in production)
        
    Returns:
        User: Current authenticated user
        
    Raises:
        HTTPException: 401 if authentication fails in production
        
    Example:
        @app.get("/protected")
        async def protected_route(user: User = Depends(get_current_user)):
            return {"message": f"Hello {user.name}"}
    """
    # Development mode: return mock user for testing
    if not settings.is_production:
        return User(
            oid="local-dev-user",
            name="Developer",
            email="dev@local",
            roles=["AppAdmin"]
        )
    
    # Production mode: return validated Azure user
    if azure_user is None:
        raise HTTPException(
            status_code=401,
            detail="Authentication required"
        )
    
    return azure_user


async def verify_admin(user: User = Depends(get_current_user)) -> User:
    """
    Verify that the current user has admin role.
    
    Use this dependency to restrict endpoints to admin users only.
    
    Args:
        user: Current authenticated user
        
    Returns:
        User: Current user if they have admin role
        
    Raises:
        HTTPException: 403 if user does not have AppAdmin role
        
    Example:
        @app.get("/admin/stats")
        async def admin_stats(user: User = Depends(verify_admin)):
            return {"message": "Admin access granted"}
    """
    if "AppAdmin" not in user.roles:
        raise HTTPException(
            status_code=403,
            detail="Admin privileges required"
        )
    return user
