"""PDF Parser with text extraction and OCR fallback.

This module provides:
- Text layer extraction using PyPDF2 and pdfplumber
- OCR fallback using Tesseract when text layer is empty
- Table extraction from text-based PDFs
"""

from __future__ import annotations

import io
import logging
from dataclasses import dataclass
from typing import Any

import pdfplumber
import pytesseract
from PIL import Image

logger = logging.getLogger(__name__)


@dataclass
class TableData:
    """Represents extracted table data from a PDF page."""

    table_id: str
    headers: list[str]
    rows: list[list[str]]
    confidence: float = 1.0


@dataclass
class PdfPageContent:
    """Represents extracted content from a single PDF page."""

    page_number: int
    text: str
    tables: list[TableData]
    was_ocr: bool = False
    ocr_confidence: float = 0.0


class PdfParser:
    """PDF parser with text extraction and OCR fallback.

    Tries text extraction first (PyPDF2/pdfplumber), and falls back to
    Tesseract OCR if the text layer is empty or insufficient.
    """

    def __init__(self, ocr_language: str = "ces+eng+deu") -> None:
        """Initialize the PDF parser.

        Args:
            ocr_language: Tesseract language code (e.g., "ces+eng+deu").
        """
        self._ocr_language = ocr_language

    def extract_pages(self, pdf_bytes: bytes) -> list[PdfPageContent]:
        """Extract content from all pages of a PDF.

        Args:
            pdf_bytes: Raw PDF file bytes.

        Returns:
            List of PdfPageContent, one per page.
        """
        pages: list[PdfPageContent] = []

        # Try pdfplumber first for text extraction
        try:
            with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
                for page_num, page in enumerate(pdf.pages, start=1):
                    page_content = self._extract_page_content(page, page_num)
                    pages.append(page_content)
        except Exception as e:
            logger.error("Failed to extract with pdfplumber: %s", e)
            # Fall back to PyPDF2
            pages = self._extract_with_pypdf2(pdf_bytes)

        return pages

    def _extract_page_content(
        self, page: pdfplumber.page.Page, page_num: int
    ) -> PdfPageContent:
        """Extract content from a single pdfplumber page.

        Args:
            page: pdfplumber page object.
            page_num: Page number (1-indexed).

        Returns:
            PdfPageContent with extracted text and tables.
        """
        # Try to extract text
        text = page.extract_text() or ""

        # Check if text is sufficient (not empty or mostly whitespace)
        has_text = bool(text.strip()) and len(text.strip()) > 10

        was_ocr = False
        ocr_confidence = 0.0

        # If no text, try OCR
        if not has_text:
            logger.info("Page %d has no text layer, attempting OCR", page_num)
            text, ocr_confidence = self._perform_ocr(page)
            was_ocr = True

        # Extract tables
        tables = self._extract_tables(page)

        return PdfPageContent(
            page_number=page_num,
            text=text,
            tables=tables,
            was_ocr=was_ocr,
            ocr_confidence=ocr_confidence,
        )

    def _extract_with_pypdf2(self, pdf_bytes: bytes) -> list[PdfPageContent]:
        """Extract content using PyPDF2 as fallback.

        Args:
            pdf_bytes: Raw PDF file bytes.

        Returns:
            List of PdfPageContent with extracted text.
        """
        # Import here to handle case where pdfplumber fails
        import PyPDF2

        pages: list[PdfPageContent] = []

        try:
            with io.BytesIO(pdf_bytes) as f:
                reader = PyPDF2.PdfReader(f)
                for page_num, page in enumerate(reader.pages, start=1):
                    text = page.extract_text() or ""

                    # Try OCR if no text
                    was_ocr = False
                    ocr_confidence = 0.0
                    if not text.strip():
                        logger.info("Page %d has no text layer, attempting OCR", page_num)
                        # For PyPDF2, we need to render to image first
                        # This is a limitation - pdfplumber is preferred
                        text = "[OCR not available with PyPDF2 fallback]"
                        was_ocr = True

                    pages.append(
                        PdfPageContent(
                            page_number=page_num,
                            text=text,
                            tables=[],
                            was_ocr=was_ocr,
                            ocr_confidence=ocr_confidence,
                        )
                    )
        except Exception as e:
            logger.error("PyPDF2 extraction failed: %s", e)

        return pages

    def _perform_ocr(
        self, page: pdfplumber.page.Page
    ) -> tuple[str, float]:
        """Perform OCR on a PDF page.

        Args:
            page: pdfplumber page object.

        Returns:
            Tuple of (extracted_text, confidence_score).
        """
        try:
            # Convert page to image
            img = page.to_image(resolution=300)
            pil_image = img.original

            # Perform OCR
            text = pytesseract.image_to_string(pil_image, lang=self._ocr_language)

            # Get confidence data
            data = pytesseract.image_to_data(
                pil_image, lang=self._ocr_language, output_type=pytesseract.Output.DICT
            )

            # Calculate average confidence
            confidences = [
                int(conf)
                for conf in data["conf"]
                if conf != "-1"  # -1 means no text in that region
            ]
            avg_confidence = sum(confidences) / len(confidences) / 100.0 if confidences else 0.0

            logger.info(
                "OCR completed for page, confidence: %.2f, text length: %d",
                avg_confidence,
                len(text),
            )

            return text, avg_confidence

        except Exception as e:
            logger.error("OCR failed: %s", e)
            return "", 0.0

    def _extract_tables(self, page: pdfplumber.page.Page) -> list[TableData]:
        """Extract tables from a PDF page.

        Args:
            page: pdfplumber page object.

        Returns:
            List of TableData objects.
        """
        tables: list[TableData] = []

        try:
            extracted_tables = page.extract_tables()

            if extracted_tables:
                for table_idx, table in enumerate(extracted_tables):
                    if not table:
                        continue

                    # First row as headers if multiple rows exist
                    headers = table[0] if table else []
                    rows = table[1:] if len(table) > 1 else []

                    tables.append(
                        TableData(
                            table_id=f"table_{page.page_number}_{table_idx}",
                            headers=headers,
                            rows=rows,
                            confidence=0.95,  # pdfplumber table extraction is reliable
                        )
                    )

        except Exception as e:
            logger.warning("Table extraction failed for page %d: %s", page.page_number, e)

        return tables

    def get_page_count(self, pdf_bytes: bytes) -> int:
        """Get the number of pages in a PDF.

        Args:
            pdf_bytes: Raw PDF file bytes.

        Returns:
            Number of pages.
        """
        try:
            with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
                return len(pdf.pages)
        except Exception:
            # Fallback to PyPDF2
            import PyPDF2

            try:
                with io.BytesIO(pdf_bytes) as f:
                    reader = PyPDF2.PdfReader(f)
                    return len(reader.pages)
            except Exception:
                return 0
