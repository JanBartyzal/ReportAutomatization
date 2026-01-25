"""
Report-related Pydantic schemas.

This module defines data models for report metadata.
"""

from pydantic import BaseModel


class Report(BaseModel):
    """
    Report metadata.
    
    Attributes:
        oid: Owner's Azure AD object ID
        region: Report region
    """
    oid: str
    region: str
