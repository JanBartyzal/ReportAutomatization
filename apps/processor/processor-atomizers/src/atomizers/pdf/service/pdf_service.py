"""gRPC service implementing ``PdfAtomizerServiceServicer``."""

from __future__ import annotations

import logging
from typing import Any, TYPE_CHECKING

import grpc

from atomizer.v1 import pdf_pb2, pdf_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.atomizers.pdf.client.blob_client import PdfBlobClient
from src.atomizers.pdf.service.pdf_parser import PdfParser
from src.common.context import extract_context_from_grpc

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


class PdfAtomizerService(pdf_pb2_grpc.PdfAtomizerServiceServicer):
    """Async gRPC servicer for PDF atomization."""

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._parser = PdfParser(ocr_language=settings.ocr_language)

    async def ExtractPdf(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> pdf_pb2.PdfExtractionResponse:
        """Extract text and tables from a PDF file."""
        req_ctx = extract_context_from_grpc(context)
        logger.info("[%s] ExtractPdf file_id=%s", req_ctx.correlation_id, request.file_id)

        async with PdfBlobClient(self._settings) as blob:
            pdf_bytes = await blob.download_pdf_bytes(request.file_id, request.blob_url)

        pages = self._parser.extract_pages(pdf_bytes)

        detection_method = "TEXT_LAYER"
        ocr_pages = [p for p in pages if p.was_ocr]
        if ocr_pages:
            detection_method = "OCR"

        status = common_pb2.PROCESSING_STATUS_COMPLETED
        for page in ocr_pages:
            if page.ocr_confidence < self._settings.ocr_confidence_threshold:
                status = common_pb2.PROCESSING_STATUS_PARTIAL
                break

        pages_pb = []
        for page in pages:
            tables_pb = []
            for table in page.tables:
                rows_pb = [pdf_pb2.TableRow(cells=row) for row in table.rows]
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
