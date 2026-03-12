"""gRPC service implementing ``PdfAtomizerServiceServicer``.

Each RPC downloads the PDF file from blob storage, parses it with text
extraction or OCR fallback, and returns the corresponding proto response.
"""

from __future__ import annotations

import logging
from typing import Any

import grpc

# Generated proto stub imports (produced by buf / protoc from
# packages/protos/atomizer/v1/pdf.proto and common/v1/common.proto).
from atomizer.v1 import pdf_pb2, pdf_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.client.blob_client import PdfBlobClient
from src.config import OCR_CONFIDENCE_THRESHOLD
from src.models.context import extract_context_from_grpc
from src.service.pdf_parser import PdfParser

logger = logging.getLogger(__name__)


class PdfAtomizerService(pdf_pb2_grpc.PdfAtomizerServiceServicer):
    """Async gRPC servicer for PDF atomization.

    Provides extraction of text and tables from PDF files with automatic
    detection of text layer vs scanned pages (OCR).
    """

    def __init__(self) -> None:
        self._parser = PdfParser()

    # ------------------------------------------------------------------
    # ExtractPdf
    # ------------------------------------------------------------------

    async def ExtractPdf(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> pdf_pb2.PdfExtractionResponse:
        """Extract text and tables from a PDF file.

        Automatically detects whether each page has a text layer or needs OCR.
        Returns per-page content including detected text, tables, and OCR
        confidence scores.
        """
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractPdf file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        # Download PDF from blob storage
        async with PdfBlobClient() as blob:
            pdf_bytes = await blob.download_pdf_bytes(request.file_id, request.blob_url)

        # Extract pages
        pages = self._parser.extract_pages(pdf_bytes)

        # Determine overall detection method and status
        detection_method = "TEXT_LAYER"
        ocr_pages = [p for p in pages if p.was_ocr]
        if ocr_pages:
            detection_method = "OCR"

        # Determine status based on OCR confidence
        status = common_pb2.PROCESSING_STATUS_COMPLETED
        for page in ocr_pages:
            if page.ocr_confidence < OCR_CONFIDENCE_THRESHOLD:
                status = common_pb2.PROCESSING_STATUS_PARTIAL
                break

        # Build proto response
        pages_pb = []
        for page in pages:
            tables_pb = []
            for table in page.tables:
                rows_pb = [
                    pdf_pb2.TableRow(cells=row) for row in table.rows
                ]
                tables_pb.append(
                    pdf_pb2.TableData(
                        table_id=table.table_id,
                        headers=table.headers,
                        rows=rows_pb,
                        confidence=table.confidence,
                    )
                )

            pages_pb.append(
                pdf_pb2.PdfPageContent(
                    page_number=page.page_number,
                    text=page.text,
                    tables=tables_pb,
                    was_ocr=page.was_ocr,
                    ocr_confidence=page.ocr_confidence,
                )
            )

        return pdf_pb2.PdfExtractionResponse(
            file_id=request.file_id,
            status=status,
            detection_method=detection_method,
            total_pages=len(pages),
            pages=pages_pb,
        )
