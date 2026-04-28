"""gRPC service implementing ``ServiceNowAtomizerServiceServicer``.

Downloads the ServiceNow export file from Blob Storage, auto-detects its
format (CSV / JSON / Excel), parses it, and returns the same
``SheetContentResponse`` proto messages used by the Excel atomizer – so the
existing Sink pipeline requires no changes.
"""

from __future__ import annotations

import logging
from typing import Any, TYPE_CHECKING

import grpc

from atomizer.v1 import servicenow_pb2, servicenow_pb2_grpc  # type: ignore[import-untyped]
from atomizer.v1 import excel_pb2  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.atomizers.servicenow.client.blob_client import ServiceNowBlobClient
from src.atomizers.servicenow.models.context import ServiceNowExportFormat
from src.atomizers.servicenow.service.servicenow_parser import ServiceNowParser
from src.atomizers.xls.service.data_type_detector import DataTypeDetector
from src.common.context import extract_context_from_grpc

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)

# Proto enum mapping
_FORMAT_TO_PROTO: dict[ServiceNowExportFormat, int] = {
    ServiceNowExportFormat.UNSPECIFIED: servicenow_pb2.SERVICE_NOW_FORMAT_UNSPECIFIED,
    ServiceNowExportFormat.CSV: servicenow_pb2.SERVICE_NOW_FORMAT_CSV,
    ServiceNowExportFormat.JSON: servicenow_pb2.SERVICE_NOW_FORMAT_JSON,
    ServiceNowExportFormat.EXCEL: servicenow_pb2.SERVICE_NOW_FORMAT_EXCEL,
}

_PROTO_TO_FORMAT: dict[int, ServiceNowExportFormat] = {
    v: k for k, v in _FORMAT_TO_PROTO.items()
}


class ServiceNowAtomizerService(servicenow_pb2_grpc.ServiceNowAtomizerServiceServicer):
    """Async gRPC servicer for ServiceNow export atomization."""

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._parser = ServiceNowParser(
            strip_system_columns=True,
            max_rows=getattr(settings, "servicenow_max_rows", 100_000),
        )
        self._type_detector = DataTypeDetector()

    # ------------------------------------------------------------------
    # RPC handlers
    # ------------------------------------------------------------------

    async def ExtractStructure(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> servicenow_pb2.ServiceNowStructureResponse:
        """Download and detect the export format; return table list."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractStructure file_id=%s format_hint=%s",
            req_ctx.correlation_id, request.file_id, request.format,
        )

        raw = await self._download(request.file_id, request.blob_url)
        fmt = self._resolve_format(request.format, raw)
        structure = self._parser.extract_structure(raw, fmt, request.snow_table_name)

        tables_pb = [
            servicenow_pb2.ServiceNowTableMetadata(
                table_index=t.table_index,
                name=t.name,
                row_count=t.row_count,
                col_count=t.col_count,
                snow_table_name=t.snow_table_name,
            )
            for t in structure.tables
        ]

        return servicenow_pb2.ServiceNowStructureResponse(
            file_id=request.file_id,
            detected_format=_FORMAT_TO_PROTO[fmt],
            tables=tables_pb,
        )

    async def ExtractTableContent(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> excel_pb2.SheetContentResponse:
        """Extract a single table as normalised headers + rows."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractTableContent file_id=%s table_index=%d",
            req_ctx.correlation_id, request.file_id, request.table_index,
        )

        raw = await self._download(request.file_id, request.blob_url)
        fmt = self._resolve_format(request.format, raw)

        try:
            result = self._parser.extract_table(raw, fmt, request.table_index,
                                                request.snow_table_name)
        except IndexError as exc:
            await context.abort(grpc.StatusCode.OUT_OF_RANGE, str(exc))
            raise  # pragma: no cover

        data_types_pb = self._detect_data_types(result.headers, result.rows)
        rows_pb = [
            excel_pb2.SheetRow(row_index=r.row_index, cells=r.cells)
            for r in result.rows
        ]

        # Enrich metadata: mark source_system in sheet_name for downstream
        sheet_name = f"SNOW:{result.snow_table_name or result.table_name}"

        return excel_pb2.SheetContentResponse(
            sheet_index=result.table_index,
            sheet_name=sheet_name,
            headers=result.headers,
            rows=rows_pb,
            data_types=data_types_pb,
        )

    async def ExtractAll(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> servicenow_pb2.ServiceNowFullExtractionResponse:
        """Batch-extract all tables from the export (partial success supported)."""
        req_ctx = extract_context_from_grpc(context)
        logger.info("[%s] ExtractAll file_id=%s", req_ctx.correlation_id, request.file_id)

        raw = await self._download(request.file_id, request.blob_url)
        fmt = self._resolve_format(request.format, raw)
        structure = self._parser.extract_structure(raw, fmt, request.snow_table_name)

        successful: list[excel_pb2.SheetContentResponse] = []
        failed: list[servicenow_pb2.ServiceNowExtractionError] = []

        for meta in structure.tables:
            idx = meta.table_index
            try:
                result = self._parser.extract_table(raw, fmt, idx, request.snow_table_name)
                data_types_pb = self._detect_data_types(result.headers, result.rows)
                rows_pb = [
                    excel_pb2.SheetRow(row_index=r.row_index, cells=r.cells)
                    for r in result.rows
                ]
                sheet_name = f"SNOW:{result.snow_table_name or result.table_name}"
                successful.append(excel_pb2.SheetContentResponse(
                    sheet_index=idx,
                    sheet_name=sheet_name,
                    headers=result.headers,
                    rows=rows_pb,
                    data_types=data_types_pb,
                ))
            except Exception as exc:
                logger.error("Failed to extract table %d (%s): %s",
                             idx, meta.name, exc, exc_info=True)
                failed.append(servicenow_pb2.ServiceNowExtractionError(
                    table_index=idx,
                    table_name=meta.name,
                    error_code="TABLE_EXTRACTION_FAILED",
                    error_message=str(exc),
                ))

        if failed and not successful:
            status = common_pb2.PROCESSING_STATUS_FAILED
        elif failed:
            status = common_pb2.PROCESSING_STATUS_PARTIAL
        else:
            status = common_pb2.PROCESSING_STATUS_COMPLETED

        return servicenow_pb2.ServiceNowFullExtractionResponse(
            file_id=request.file_id,
            status=status,
            detected_format=_FORMAT_TO_PROTO[fmt],
            successful_tables=successful,
            failed_tables=failed,
        )

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    async def _download(self, file_id: str, blob_url: str) -> bytes:
        async with ServiceNowBlobClient(self._settings) as blob:
            return await blob.download_export_bytes(file_id, blob_url)

    def _resolve_format(self, proto_format: int, raw: bytes) -> ServiceNowExportFormat:
        """Use proto hint if provided; otherwise auto-detect."""
        if proto_format and proto_format != servicenow_pb2.SERVICE_NOW_FORMAT_UNSPECIFIED:
            return _PROTO_TO_FORMAT.get(proto_format, ServiceNowExportFormat.UNSPECIFIED)
        return self._parser.detect_format(raw)

    def _detect_data_types(
        self,
        headers: list[str],
        rows: list[Any],
    ) -> list[excel_pb2.ColumnDataType]:
        if not headers:
            return []
        data_types: list[excel_pb2.ColumnDataType] = []
        for col_idx, header in enumerate(headers):
            column_values = [
                r.cells[col_idx] for r in rows if col_idx < len(r.cells)
            ]
            detected = self._type_detector.detect_column_type(column_values)
            data_types.append(excel_pb2.ColumnDataType(
                col_index=col_idx,
                column_name=header,
                detected_type=detected,
            ))
        return data_types
