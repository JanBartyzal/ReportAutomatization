"""
OCR-based table extraction from images using Tesseract and img2table.

This module provides intelligent table extraction from images with confidence
scoring to determine whether to use local OCR or escalate to AI Vision processing.
"""

import logging
from typing import Dict, Any, Tuple
import pytesseract
from pytesseract import Output
from img2table.document import Image as TableImage
from img2table.ocr import TesseractOCR
import numpy as np
import cv2
import io
from PIL import Image

logger = logging.getLogger(__name__)


class TableImageData:
    """
    Table extraction from images using OCR and structure detection.
    
    Uses multi-tier strategy:
    1. Tesseract OCR for confidence scoring and title detection
    2. img2table for table structure detection
    3. Escalates to AI Vision if confidence < 85%
    """
    
    def __init__(self) -> None:
        """Initialize OCR engine with single thread and English language."""
        self.ocr_engine = TesseractOCR(n_threads=1, lang="eng")

    def get_tesseract_confidence(self, pil_image: Image.Image) -> Tuple[float, str]:
        """
        Calculate average OCR confidence and extract table title.
        
        Analyzes image with Tesseract to determine:
        - Overall text recognition confidence (0-100)
        - Detected table title (first line of text)
        
        Args:
            pil_image: PIL Image object to analyze
            
        Returns:
            Tuple of (average_confidence, detected_title)
            - average_confidence: Mean confidence score (0-100)
            - detected_title: Extracted title from first text line
        """
        # Get detailed word-level OCR data
        data = pytesseract.image_to_data(
            pil_image,
            output_type=Output.DICT,
            lang='ces'  # Czech language support
        )
        
        confidences = []
        lines: Dict[int, list] = {}
        
        n_boxes = len(data['text'])
        for i in range(n_boxes):
            # Ignore empty strings and low confidence markers (-1)
            text = data['text'][i].strip()
            conf = int(data['conf'][i])
            
            if conf > 0 and len(text) > 0:
                confidences.append(conf)
                
                # Group text by vertical position (top coordinate) to reconstruct lines
                # Tolerance of 10px for same-line grouping
                top = data['top'][i]
                found_line = False
                for y in lines.keys():
                    if abs(y - top) < 10:
                        lines[y].append(text)
                        found_line = True
                        break
                if not found_line:
                    lines[top] = [text]

        # Calculate average confidence
        avg_conf = sum(confidences) / len(confidences) if confidences else 0.0
        
        # Extract title: first line of detected text
        sorted_ys = sorted(lines.keys())
        title = " ".join(lines[sorted_ys[0]]) if sorted_ys else "Unknown Table"

        return avg_conf, title

    def smart_extract(self, image_bytes: bytes) -> Dict[str, Any]:
        """
        Intelligent table extraction with confidence-based fallback.
        
        Extraction strategy:
        1. Quick confidence check with Tesseract
        2. Structure detection with img2table
        3. If confidence >= 85% AND valid table structure → use local OCR
        4. Otherwise → flag for AI Vision processing
        
        Args:
            image_bytes: Raw image bytes
            
        Returns:
            Dictionary with extraction results:
                - title: Detected table title
                - method: "LOCAL_OCR" or "AI_VISION"
                - data: Extracted table data (list of dicts) or None
                - confidence: OCR confidence score (0-100)
        """
        image_io = io.BytesIO(image_bytes)
        pil_img = Image.open(image_io)

        # Step 1: Fast Tesseract analysis (confidence + title)
        confidence, detected_title = self.get_tesseract_confidence(pil_img)
        logger.info(f"OCR Confidence: {confidence:.1f}%, Title estimate: {detected_title}")

        result: Dict[str, Any] = {
            "title": detected_title,
            "method": "AI_VISION",  # Default to AI if not perfect
            "data": None,
            "confidence": confidence
        }

        # Step 2: Attempt table extraction with img2table (local)
        image_io.seek(0)  # Reset stream
        doc = TableImage(image_io)
        
        try:
            # Extract tables (supports borderless tables)
            extracted_tables = doc.extract_tables(
                ocr=self.ocr_engine,
                borderless_tables=True
            )
            
            if extracted_tables and len(extracted_tables) > 0:
                # Table structure found!
                table = extracted_tables[0]
                df = table.df
                
                # Step 3: Decision logic (traffic light system)
                # Use local OCR only if: high confidence AND valid table shape
                if confidence > 85 and df.shape[1] > 1 and df.shape[0] > 1:
                    logger.info(" -> Using local OCR data")
                    result["method"] = "LOCAL_OCR"
                    result["data"] = df.to_dict(orient='records')
                else:
                    logger.info(" -> Table found but low OCR confidence. Escalating to AI Vision")
            else:
                logger.info(" -> img2table did not detect table grid. Escalating to AI Vision")

        except Exception as e:
            logger.error(f"Error in img2table extraction: {e}")

        return result