"""
Report-related Pydantic schemas.

This module defines data models for report metadata.
"""

from pydantic import BaseModel
from typing import Any, Optional


class Report(BaseModel):
    """
    Report metadata.
    
    Attributes:
        oid: Owner's Azure AD object ID
        region: Report region
    """
    oid: str
    region: str


class AppendixTable(BaseModel):
    """
    Represents a single table extracted from an Excel sheet.
    """
    id: str
    name: str
    rows: list[dict[str, Any]]


class AppendixSheet(BaseModel):
    """
    Represents a sheet in the Excel appendix.
    """
    sheet_name: str
    tables: list[AppendixTable]


class Appendix(BaseModel):
    """
    Excel data appendix structure.
    """
    source_file: str
    uploaded_at: str
    sheets: list[AppendixSheet]


class ReportUpdate(BaseModel):
    """
    Schema for updating a report.
    """
    appendix: Optional[Appendix] = None

