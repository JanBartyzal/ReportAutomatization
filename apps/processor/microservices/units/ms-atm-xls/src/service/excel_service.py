"""gRPC service implementing ``ExcelAtomizerServiceServicer``.

Each RPC downloads the Excel file from blob storage, parses it, and returns
the corresponding proto response.  ``ExtractAll`` processes every sheet and
collects per-sheet errors for partial-success reporting.
"""

from __future__ import annotations

import logging
from typing import Any

import grpc

# ---------------------------------------------------------------------------
# Generated proto stub imports (produced by buf / protoc from
# packages/protos/atomizer/v1/excel.proto and common/v1/common.proto).
# ---------------------------------------------------------------------------
from atomizer.v1 import excel_pb2, excel_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.client.blob_client import ExcelBlobClient
from src.models.context import extract_context_from_grpc
from src.service.data_type_detector import DataTypeDetector
from src.service.excel_parser import ExcelParser

logger = logging.getLogger(__name__)


class ExcelAtomizerService(excel_pb2_grpc.ExcelAtomizerServiceServicer):
    """Async gRPC servicer for Excel atomization.

    Provides three RPCs:
    - ``ExtractStructure`` – sheet list with metadata.
    - ``ExtractSheetContent`` – headers, rows, data types for one sheet.
    - ``ExtractAll`` – batch extraction of all sheets with partial-success handling.
    """

    def __init__(self) -> None:
        self._parser = ExcelParser()
        self._type_detector = DataTypeDetector()

    # ------------------------------------------------------------------
    # ExtractStructure
    # ------------------------------------------------------------------

    async def ExtractStructure(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> excel_pb2.ExcelStructureResponse:
        """Download an Excel file and return its high-level structure."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractStructure file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        async with ExcelBlobClient() as blob:
            excel_bytes = await blob.download_excel_bytes(request.file_id, request.blob_url)

        wb = self._parser.open(excel_bytes)

        try:
            structure = self._parser.extract_structure(wb)

            sheets_pb = [
                excel_pb2.SheetMetadata(
                    sheet_index=s.sheet_index,
                    name=s.name,
                    row_count=s.row_count,
                    col_count=s.col_count,
                    has_merged_cells=s.has_merged_cells,
                )
                for s in structure.sheets
            ]

            return excel_pb2.ExcelStructureResponse(
                file_id=request.file_id,
                sheets=sheets_pb,
            )
        finally:
            wb.close()

    # ------------------------------------------------------------------
    # ExtractSheetContent
    # ------------------------------------------------------------------

    async def ExtractSheetContent(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> excel_pb2.SheetContentResponse:
        """Extract headers, rows, and data types from a specific sheet."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractSheetContent file_id=%s sheet=%d",
            req_ctx.correlation_id,
            request.file_id,
            request.sheet_index,
        )

        async with ExcelBlobClient() as blob:
            excel_bytes = await blob.download_excel_bytes(request.file_id, request.blob_url)

        wb = self._parser.open(excel_bytes)

        try:
            try:
                content = self._parser.extract_sheet_content(wb, request.sheet_index)
            except IndexError as exc:
                await context.abort(grpc.StatusCode.OUT_OF_RANGE, str(exc))
                raise  # pragma: no cover

            # Detect data types for each column
            data_types_pb = self._detect_data_types(content.headers, content.rows)

            rows_pb = [
                excel_pb2.SheetRow(
                    row_index=r.row_index,
                    cells=r.cells,
                )
                for r in content.rows
            ]

            return excel_pb2.SheetContentResponse(
                sheet_index=content.sheet_index,
                sheet_name=content.sheet_name,
                headers=content.headers,
                rows=rows_pb,
                data_types=data_types_pb,
            )
        finally:
            wb.close()

    # ------------------------------------------------------------------
    # ExtractAll
    # ------------------------------------------------------------------

    async def ExtractAll(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> excel_pb2.ExcelFullExtractionResponse:
        """Batch-extract content from all sheets.

        Errors on individual sheets are recorded but do not abort the
        entire operation (partial success).
        """
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractAll file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        # Download once
        async with ExcelBlobClient() as blob:
            excel_bytes = await blob.download_excel_bytes(request.file_id, request.blob_url)

        wb = self._parser.open(excel_bytes)

        try:
            structure = self._parser.extract_structure(wb)

            successful_sheets: list[excel_pb2.SheetContentResponse] = []
            failed_sheets: list[excel_pb2.SheetExtractionError] = []

            for sheet_meta in structure.sheets:
                idx = sheet_meta.sheet_index
                try:
                    content = self._parser.extract_sheet_content(wb, idx)

                    # Detect data types
                    data_types_pb = self._detect_data_types(content.headers, content.rows)

                    rows_pb = [
                        excel_pb2.SheetRow(
                            row_index=r.row_index,
                            cells=r.cells,
                        )
                        for r in content.rows
                    ]

                    successful_sheets.append(
                        excel_pb2.SheetContentResponse(
                            sheet_index=content.sheet_index,
                            sheet_name=content.sheet_name,
                            headers=content.headers,
                            rows=rows_pb,
                            data_types=data_types_pb,
                        )
                    )
                except Exception as exc:
                    logger.error(
                        "Failed to extract content for sheet %d (%s): %s",
                        idx,
                        sheet_meta.name,
                        exc,
                        exc_info=True,
                    )
                    failed_sheets.append(
                        excel_pb2.SheetExtractionError(
                            sheet_index=idx,
                            sheet_name=sheet_meta.name,
                            error_code="SHEET_EXTRACTION_FAILED",
                            error_message=str(exc),
                        )
                    )

            # Determine overall status
            if failed_sheets and len(successful_sheets) == 0:
                status = common_pb2.PROCESSING_STATUS_FAILED
            elif failed_sheets:
                status = common_pb2.PROCESSING_STATUS_PARTIAL
            else:
                status = common_pb2.PROCESSING_STATUS_COMPLETED

            return excel_pb2.ExcelFullExtractionResponse(
                file_id=request.file_id,
                status=status,
                successful_sheets=successful_sheets,
                failed_sheets=failed_sheets,
            )
        finally:
            wb.close()

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _detect_data_types(
        self,
        headers: list[str],
        rows: list[Any],
    ) -> list[excel_pb2.ColumnDataType]:
        """Detect data types for each column by sampling row values.

        Args:
            headers: Column header names.
            rows: List of SheetRowData with cells.

        Returns:
            List of proto ColumnDataType messages.
        """
        if not headers:
            return []

        data_types: list[excel_pb2.ColumnDataType] = []

        for col_idx, header in enumerate(headers):
            # Collect values for this column across all rows
            column_values: list[str] = []
            for row in rows:
                if col_idx < len(row.cells):
                    column_values.append(row.cells[col_idx])

            detected_type = self._type_detector.detect_column_type(column_values)

            data_types.append(
                excel_pb2.ColumnDataType(
                    col_index=col_idx,
                    column_name=header,
                    detected_type=detected_type,
                )
            )

        return data_types
