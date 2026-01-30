"""
RBAC Decorators for FastAPI Route Protection

Provides role-based access control decorators that can be used as dependencies in FastAPI routes.
Supports: Admin, Editor, Viewer roles with organization-aware validation.
"""

from functools import wraps
from fastapi import Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.identity.auth import get_current_user
from app.core.models import Users
from typing import List
import logging

logger = logging.getLogger(__name__)

# Role hierarchy: Admin > Editor > Viewer
ROLE_HIERARCHY = {
    "admin": 3,
    "editor": 2,
   "viewer": 1
}

def has_role(user: Users, required_role: str) -> bool:
    """
    Check if user has the required role or higher in hierarchy.
    
    Args:
        user: User object from database
        required_role: Minimum required role (viewer, editor, admin)
    
    Returns:
        Boolean indicating if user has sufficient privileges
    """
    user_role = getattr(user, 'role', 'viewer').lower()
    required_level = ROLE_HIERARCHY.get(required_role.lower(), 0)
    user_level = ROLE_HIERARCHY.get(user_role, 0)
    
    return user_level >= required_level

def require_role(required_role: str):
    """
    Dependency factory that creates a role-checking dependency.
    
    Usage:
        @app.get("/admin-only")
        def admin_endpoint(user: Users = Depends(require_role("admin"))):
            ...
    
    Args:
        required_role: Minimum required role (viewer, editor, admin)
    
    Returns:
        Dependency function that validates user role
    
    Raises:
        HTTPException: 403 if user doesn't have required role
    """
    async def role_checker(
        current_user: Users = Depends(get_current_user),
        db: Session = Depends(get_db)
    ) -> Users:
        if not has_role(current_user, required_role):
            logger.warning(
                f"Access denied: User {current_user.id} (role: {getattr(current_user, 'role', 'viewer')}) "
                f"attempted to access {required_role} endpoint"
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Insufficient permissions. Required role: {required_role}"
            )
        
        return current_user
    
    return role_checker

# Convenience decorators for common roles
require_admin = require_role("admin")
require_editor = require_role("editor")
require_viewer = require_role("viewer")  # Effectively just requires authentication

def require_any_role(roles: List[str]):
    """
    Dependency that allows access if user has ANY of the specified roles.
    
    Usage:
        @app.get("/flexible")
        def flexible_endpoint(user: Users = Depends(require_any_role(["admin", "editor"]))):
            ...
    
    Args:
        roles: List of acceptable roles
    
    Returns:
        Dependency function that validates user has at least one of the roles
    """
    async def any_role_checker(
        current_user: Users = Depends(get_current_user),
        db: Session = Depends(get_db)
    ) -> Users:
        if not any(has_role(current_user, role) for role in roles):
            logger.warning(
                f"Access denied: User {current_user.id} (role: {getattr(current_user, 'role', 'viewer')}) "
                f"attempted to access endpoint requiring one of: {roles}"
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Insufficient permissions. Required one of: {', '.join(roles)}"
            )
        
        return current_user
    
    return any_role_checker

def require_same_organization(resource_org_id: int):
    """
    Validate that the current user belongs to the same organization as the resource.
    
    Usage:
        plan = db.query(InfraPlan).get(plan_id)
        user = Depends(require_same_organization(plan.organization_id))
    
    Args:
        resource_org_id: Organization ID of the resource being accessed
    
    Raises:
        HTTPException: 403 if user's organization doesn't match
    """
    async def org_checker(current_user: Users = Depends(get_current_user)) -> Users:
        if current_user.organization_id != resource_org_id:
            logger.warning(
                f"Cross-tenant access attempt: User {current_user.id} (org: {current_user.organization_id}) "
                f"attempted to access resource in org: {resource_org_id}"
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Access denied: Resource belongs to different organization"
            )
        
        return current_user
    
    return org_checker
