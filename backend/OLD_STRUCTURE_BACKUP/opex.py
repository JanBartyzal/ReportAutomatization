"""
OPEX (Operational Expenditure) report processing.

This module handles processing of OPEX PowerPoint files including
slide extraction, data normalization, and report generation.
"""

import os
import logging
from typing import List, Dict, Any
from fastapi import HTTPException
from sqlalchemy.orm import Session
from dbmodels import UploadFile as DBUploadFile
from powerpoint import PowerpointManager
from models import SlideData
from pptx import Presentation
from table_data import TableDataProcessor


logger = logging.getLogger(__name__)


class OpexManager:
    """
    Manager for OPEX report processing.
    
    Handles extraction and processing of OPEX data from PowerPoint files.
    """
    
    def __init__(self) -> None:
        """Initialize OPEX manager with required processors."""
        self.powerpoint_manager = PowerpointManager()
        self.table_data_processor = TableDataProcessor()

    def process_opex(self, file_id: str, db: Session) -> None:
        """
        Process OPEX file and extract all slides.
        
        Args:
            file_id: Database ID of the uploaded file
            db: Database session
            
        Raises:
            HTTPException: 404 if file not found
        """
        db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
        if not db_file:
            raise HTTPException(status_code=404, detail="File not found")
        
        file_path = os.path.join("local_data/uploads", db_file.filename)
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="File not found on disk")
        
        presentation = self.powerpoint_manager.load_powerpoint(file_path)
        slides = self.powerpoint_manager.extract_slides(presentation)

        for slide in slides:
            logger.info(f"Processed slide {slide.slide_index}: {slide.title}")

    def get_presetation_header(self, file_id: str, db: Session) -> List[Dict[str, Any]]:
        """
        Get presentation header information (slide summary).
        
        Args:
            file_id: Database ID of the uploaded file
            db: Database session
            
        Returns:
            List of slide summary dictionaries containing:
                - slide_id: Slide index
                - slide_title: Slide title
                - table_data: Number of tables found
                - image_data: Number of images found
                - text_content: Number of text elements
                
        Raises:
            HTTPException: 404 if file not found
        """
        db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
        if not db_file:
            raise HTTPException(status_code=404, detail="File not found")
        
        file_path = os.path.join("local_data/uploads", db_file.filename)
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="File not found on disk")
        
        presentation = self.powerpoint_manager.load_powerpoint(file_path)
        slides = self.powerpoint_manager.extract_slides(presentation)
        headdata = []

        for slide in slides:
            info = {
                "slide_id": slide.slide_index,
                "slide_title": slide.title,
                "table_data": len(slide.table_data),
                "image_data": len(slide.image_data),
                "text_content": len(slide.text_content)
            }
            headdata.append(info)

        return headdata

    def get_slide_data(self, file_id: str, slide_id: int, db: Session) -> List[Dict[str, Any]]:
        """
        Get detailed data for a specific slide.
        
        Args:
            file_id: Database ID of the uploaded file
            slide_id: Slide index to retrieve
            db: Database session
            
        Returns:
            List containing one slide data dictionary with:
                - slide_id: Slide index
                - slide_title: Slide title
                - text_content: Normalized text content
                - table_data: List of extracted tables
                - image_data_count: Number of images
                - text_content_count: Number of text elements
                
        Raises:
            HTTPException: 404 if file not found
        """
        db_file = db.query(DBUploadFile).filter(DBUploadFile.id == file_id).first()
        if not db_file:
            raise HTTPException(status_code=404, detail="File not found")
        
        file_path = os.path.join("local_data/uploads", db_file.filename)
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="File not found on disk")
        
        presentation = self.powerpoint_manager.load_powerpoint(file_path)
        slides = self.powerpoint_manager.extract_slides(presentation)
        headdata = []

        for slide in slides:
            if slide.slide_index == slide_id:
                textslide = self.table_data_processor.normalize_text(slide.text_content)
                tableslide = self.table_data_processor.normalize_table(slide.table_data)
                
                tables_list = []
                # normalize_table returns a DataFrame. Validate it's not empty before converting.
                if tableslide is not None and not tableslide.empty:
                     tables_list.append(tableslide.to_dict(orient='records'))

                info = {
                    "slide_id": slide.slide_index,
                    "slide_title": slide.title,
                    "text_content": textslide,
                    "table_data": tables_list,
                    "image_data_count": len(slide.image_data),
                    "text_content_count": len(slide.text_content)
                }
                headdata.append(info)

        return headdata

        


