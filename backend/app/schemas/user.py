"""
User-related Pydantic schemas.

This module defines data models for user authentication and region assignments.
"""

from pydantic import BaseModel, Field
from typing import Optional, List

class Regions(BaseModel):
    """
    User region assignment.
    
    Attributes:
        aad_object_id: User's Azure AD object ID
        region: Assigned region name
    """
    aad_object_id: str
    region: str


class User(BaseModel):
    id: str
    email: Optional[str] = None
    name: Optional[str] = None
    tenant_id: Optional[str] = None # Entra ID Directory ID
    roles: List[str] = []
    region: Optional[str] = None
    
    # Custom claim for app-specific tenant (Free/Premium DB)
    app_tenant_id: Optional[str] = None

class UserPermissions(BaseModel):
    can_sync_prices: bool
    can_edit_plans: bool
    can_view_reports: bool

class UserMeResponse(BaseModel):
    id: str
    email: Optional[str] = None
    roles: List[str]
    permissions: UserPermissions
