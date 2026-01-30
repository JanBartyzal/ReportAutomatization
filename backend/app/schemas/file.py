"""
File upload-related Pydantic schemas.

This module defines data models for file upload metadata.
"""

from pydantic import BaseModel


class UploadFile(BaseModel):
    """
    Uploaded file metadata.
    
    Attributes:
        id: Owner's Azure AD object ID
        filename: Sanitized filename with MD5 suffix
        md5hash: MD5 hash of file content for deduplication
    """
    id: str
    filename: str
    md5hash: str
