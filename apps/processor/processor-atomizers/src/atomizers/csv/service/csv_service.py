"""gRPC service implementing ``CsvAtomizerServiceServicer``."""

from __future__ import annotations

import logging
from typing import Any, TYPE_CHECKING

import grpc

from atomizer.v1 import csv_pb2, csv_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.atomizers.csv.client.blob_client import CsvBlobClient
from src.atomizers.csv.service.csv_parser import CsvParser
from src.common.context import extract_context_from_grpc

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


class CsvAtomizerService(csv_pb2_grpc.CsvAtomizerServiceServicer):
    """Async gRPC servicer for CSV atomization."""

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._parser = CsvParser(max_rows_to_sample=settings.max_rows_to_sample)

    async def ExtractCsv(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> csv_pb2.CsvExtractionResponse:
        """Extract data from a CSV file."""
        req_ctx = extract_context_from_grpc(context)
        logger.info("[%s] ExtractCsv file_id=%s", req_ctx.correlation_id, request.file_id)

        async with CsvBlobClient(self._settings) as blob:
            csv_bytes = await blob.download_csv_bytes(request.file_id, request.blob_url)

        result = self._parser.parse(csv_bytes)

        rows_pb = [
            csv_pb2.SheetRow(row_index=idx, cells=row)
            for idx, row in enumerate(result.rows)
        ]

        data_types_pb = [
            csv_pb2.ColumnDataType(
                col_index=idx,
                column_name=name,
                detected_type=dt,
            )
            for idx, (name, dt) in enumerate(zip(result.headers, result.data_types))
        ]

        return csv_pb2.CsvExtractionResponse(
            file_id=request.file_id,
            status=common_pb2.PROCESSING_STATUS_COMPLETED,
            detected_delimiter=result.detected_delimiter,
            detected_encoding=result.detected_encoding,
            total_rows=result.total_rows,
            headers=result.headers,
            rows=rows_pb,
            data_types=data_types_pb,
        )
