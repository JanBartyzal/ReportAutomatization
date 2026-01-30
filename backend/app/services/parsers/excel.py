"""
Excel file processing and table extraction.

This module handles reading Excel (.xlsx, .xls) files, extracting data from sheets,
and converting them to the standardized SlideData structure.
"""

import logging
from typing import List, Dict, Any
import pandas as pd
from app.schemas.slide import SlideData

logger = logging.getLogger(__name__)

class ExcelProcessor:
    """
    Processor for Excel file data extraction.
    
    Converts Excel sheets into a structure compatible with PPTX slide processing.
    """

    def parse(self, file_path: str) -> List[SlideData]:
        """
        Parse Excel workbook and convert sheets to SlideData.
        
        Args:
            file_path: Path to the Excel file
            
        Returns:
            List of SlideData objects, one per sheet
        """
        logger.info(f"Parsing Excel file: {file_path}")
        slides = []
        
        try:
            # Load all sheets. engine='openpyxl' is used for .xlsx
            # We use data_only=True via openpyxl if we were using it directly, 
            # but pandas handles values by default.
            with pd.ExcelFile(file_path) as xls:
                for i, sheet_name in enumerate(xls.sheet_names):
                    df = pd.read_excel(xls, sheet_name=sheet_name)
                    
                    if df.empty:
                        logger.info(f"Skipping empty sheet: {sheet_name}")
                        continue
                        
                    # Basic cleaning
                    df = df.fillna("")
                    
                    # Convert to records
                    table_data = df.to_dict(orient='records')
                    
                    # Map to SlideData
                    slide = SlideData(
                        slide_index=i + 1,
                        title=f"Sheet: {sheet_name}",
                        table_data=table_data,
                        pseudo_tables=[],
                        image_data=[],
                        text_content=[f"Data imported from Excel sheet: {sheet_name}"]
                    )
                    slides.append(slide)
                    
            logger.info(f"Successfully extracted {len(slides)} sheets from {file_path}")
            return slides
            
        except Exception as e:
            logger.error(f"Error parsing Excel file {file_path}: {e}")
            raise

    def parse_appendix(self, file_path: str) -> dict:
        """
        Parse Excel workbook and convert to Appendix structure.
        
        Args:
            file_path: Path to the Excel file
            
        Returns:
            Dictionary matching Appendix schema
        """
        logger.info(f"Parsing Excel appendix: {file_path}")
        sheets = []
        
        try:
            with pd.ExcelFile(file_path) as xls:
                for sheet_name in xls.sheet_names:
                    # Read sheet, no index col, header at row 0
                    df = pd.read_excel(xls, sheet_name=sheet_name)
                    
                    if df.empty:
                        continue
                        
                    # Sanitize data
                    # fillna(None) replaces NaN with None, which json.dumps handles as null
                    # However, pandas fillna(None) can be tricky, object dtype safer.
                    df = df.where(pd.notnull(df), None)
                    
                    # Convert timestamps to ISO strings
                    for col in df.select_dtypes(include=['datetime', 'datetimetz']).columns:
                        df[col] = df[col].apply(lambda x: x.isoformat() if pd.notnull(x) else None)
                        
                    # Create table structure
                    table_rows = df.to_dict(orient='records')
                                        
                    # Simplify: One sheet = One "Table" for now, or detect multiple?
                    # Spec says "Detect data regions", but simplest is:
                    # Treat used range as one table.
                    
                    sheets.append({
                        "sheet_name": sheet_name,
                        "tables": [
                            {
                                "id": f"excel-{sheet_name}-range-used",
                                "name": "Used Range",
                                "rows": table_rows
                            }
                        ]
                    })
                    
            return {"sheets": sheets}

        except Exception as e:
            logger.error(f"Error parsing appendix {file_path}: {e}")
            raise

