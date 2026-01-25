"""
User-related Pydantic schemas.

This module defines data models for user authentication and region assignments.
"""

from pydantic import BaseModel, Field
from typing import Optional, List


class User(BaseModel):
    """
    User information from Azure AD JWT token.
    
    Attributes:
        oid: Object ID in Azure AD (unique user identifier)
        name: User's display name
        email: User's email address (from preferred_username claim)
        roles: List of assigned roles (e.g., ["AppAdmin"])
        region: User's assigned region (optional)
    """
    oid: str = Field(..., description="Object ID in Azure AD (unique user identifier)")
    name: Optional[str] = None
    email: Optional[str] = Field(None, alias="preferred_username")
    roles: List[str] = []
    region: Optional[str] = None


class Regions(BaseModel):
    """
    User region assignment.
    
    Attributes:
        oid: User's Azure AD object ID
        region: Assigned region name
    """
    oid: str
    region: str
