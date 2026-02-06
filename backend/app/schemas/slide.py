"""
Slide data-related Pydantic schemas.

This module defines data models for PowerPoint slide content.
"""

from pydantic import BaseModel
from typing import Optional, List, Dict, Any


class SlideData(BaseModel):
    """
    Extracted data from a single PowerPoint slide.
    
    Attributes:
        report_id: Parent report owner's id (optional)
        slide_index: 1-indexed slide number
        title: Slide title text
        table_data: List of extracted table rows (dicts)
        image_data: List of image metadata and base64 data
        text_content: List of text elements from slide
    """
    report_id: Optional[str] = None
    slide_index: int
    title: str
    table_data: List[Dict[str, Any]]
    pseudo_tables: List[Dict[str, Any]] = []
    image_data: List[Dict[str, Any]]
    text_content: List[str] = []
    ollama_slide_image: Optional[str] = None # Base64
    ollama_prompt: Optional[str] = None
