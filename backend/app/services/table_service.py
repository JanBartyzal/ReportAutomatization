"""
Table data processing utilities.

This module provides utilities for processing table data including
caching, normalization, and conversion between formats.
"""

import re
import hashlib
import json
import logging
from typing import List, Dict, Any, Optional
import pandas as pd
from app.db.cache import cache
from app.services.ocr_service import TableImageData
from app.core.config import settings


logger = logging.getLogger(__name__)


class TableDataProcessor:
    """
    Processor for table data extraction and caching.
    
    Handles image hashing, Redis caching, and data normalization.
    """
    
    def __init__(self) -> None:
        """Initialize table data processor with image extractor."""
        self.image_data = TableImageData()

    def sanitize_table_name(self, title: str) -> str:
        """
        Sanitize table title for use as database table name.
        
        Args:
            title: Original table title
            
        Returns:
            Sanitized table name (lowercase, alphanumeric + underscores)
        """
        clean = re.sub(r'[^a-zA-Z0-9]', '_', title).lower()
        return f"report_{clean}"

    def save_to_specific_table(
        self,
        df: pd.DataFrame,
        detected_title: str,
        engine: Any
    ) -> str:
        """
        Save DataFrame to specific database table.
        
        Args:
            df: DataFrame to save
            detected_title: Table title to derive table name from
            engine: SQLAlchemy engine
            
        Returns:
            Name of the database table created
        """
        table_name = self.sanitize_table_name(detected_title)
        df.to_sql(table_name, engine, if_exists='append', index=False)
        return table_name

    def get_image_hash(self, image_bytes: bytes) -> str:
        """
        Calculate SHA-256 hash of image bytes for deduplication.
        
        Args:
            image_bytes: Raw image bytes
            
        Returns:
            Hexadecimal SHA-256 hash string
        """
        return hashlib.sha256(image_bytes).hexdigest()
    
    async def extract_data(self, image_bytes: bytes) -> Dict[str, Any]:
        """
        Extract table data from image with caching.
        
        Checks Redis cache first using SHA-256 hash of image.
        If not cached, performs extraction and stores result.
        
        Args:
            image_bytes: Raw image bytes
            
        Returns:
            Extraction result dictionary with keys:
                - title: Detected table title
                - method: Extraction method used
                - data: Extracted table data
                - confidence: OCR confidence score
        """
        img_hash = self.get_image_hash(image_bytes)
        cache_key = f"img_extract:{img_hash}"
    
        cached_result = await cache.get(cache_key)
        if cached_result:
            logger.info(f"⚡ CACHE HIT: {img_hash[:8]}... (loading from Redis)")
            return cached_result

        logger.info(f"🐢 CACHE MISS: {img_hash[:8]}... (extracting)")
        result = self.image_data.smart_extract(image_bytes)
        await cache.set(cache_key, result)
    
        return result

    def normalize_text(self, text_content: List[str]) -> str:
        """
        Normalize text content to numbered lines.
        
        Takes a list of strings (text lines) and returns a single string
        with sequential line numbering.
        
        Args:
            text_content: List of text lines or single string
            
        Returns:
            Numbered text lines separated by newlines
        """
        combined_text_lines = []
        line_counter = 1
        
        if isinstance(text_content, list):
            for line in text_content:
                if line and str(line).strip():
                    combined_text_lines.append(f"{line_counter}. {str(line).strip()}")
                    line_counter += 1
        elif isinstance(text_content, str) and text_content.strip():
             combined_text_lines.append(f"1. {text_content.strip()}")
             
        return "\n".join(combined_text_lines)

    def normalize_table(self, table_data: List[Dict[str, Any]]) -> pd.DataFrame:
        """
        Convert table data to pandas DataFrame.
        
        Takes a list of dictionaries (rows) and converts it to a pandas DataFrame.
        
        Args:
            table_data: List of row dictionaries
            
        Returns:
            DataFrame if conversion succeeds, empty DataFrame otherwise
        """
        if table_data and isinstance(table_data, list):
             try:
                 df = pd.DataFrame(table_data)
                 if not df.empty:
                     return df
             except Exception as e:
                 logger.error(f"Error converting table data to DataFrame: {e}")
        return pd.DataFrame()