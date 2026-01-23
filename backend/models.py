from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


class User(BaseModel):
    oid: str = Field(..., description="Object ID v Azure AD (unikátní ID uživatele)")
    name: Optional[str] = None
    email: Optional[str] = Field(None, alias="preferred_username")
    roles: list[str] = []
    region: Optional[str] = None

class Regions(BaseModel):
    oid: str
    region: str
   

class UploadFile(BaseModel):
    oid: str
    filename: str
    md5hash: str

class Report(BaseModel):
    oid: str
    region: str

class SlideData(BaseModel):
    report_oid: Optional[str] = None
    slide_index: int
    title: str
    table_data: List[Dict[str, Any]]
    image_data: List[Dict[str, Any]]
    text_content: List[str] = []