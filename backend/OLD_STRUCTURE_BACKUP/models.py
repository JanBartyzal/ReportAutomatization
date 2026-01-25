"""
Pydantic models for API request/response validation.

This module defines data models for user authentication, file uploads,
reports, and slide data processing.
"""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


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


class UploadFile(BaseModel):
    """
    Uploaded file metadata.
    
    Attributes:
        oid: Owner's Azure AD object ID
        filename: Sanitized filename with MD5 suffix
        md5hash: MD5 hash of file content for deduplication
    """
    oid: str
    filename: str
    md5hash: str


class Report(BaseModel):
    """
    Report metadata.
    
    Attributes:
        oid: Owner's Azure AD object ID
        region: Report region
    """
    oid: str
    region: str


class SlideData(BaseModel):
    """
    Extracted data from a single PowerPoint slide.
    
    Attributes:
        report_oid: Parent report owner's OID (optional)
        slide_index: 1-indexed slide number
        title: Slide title text
        table_data: List of extracted table rows (dicts)
        image_data: List of image metadata and base64 data
        text_content: List of text elements from slide
    """
    report_oid: Optional[str] = None
    slide_index: int
    title: str
    table_data: List[Dict[str, Any]]
    image_data: List[Dict[str, Any]]
    text_content: List[str] = []