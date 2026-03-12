"""gRPC service implementing ``CsvAtomizerServiceServicer``.

Each RPC downloads the CSV file from blob storage, parses it with auto-detection
of delimiter, encoding, and data types, and returns the corresponding proto response.
"""

from __future__ import annotations

import logging
from typing import Any

import grpc

# Generated proto stub imports (produced by buf / protoc from
# packages/protos/atomizer/v1/csv.proto and common/v1/common.proto).
from atomizer.v1 import csv_pb2, csv_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.client.blob_client import CsvBlobClient
from src.models.context import extract_context_from_grpc
from src.service.csv_parser import CsvParser

logger = logging.getLogger(__name__)


class CsvAtomizerService(csv_pb2_grpc.CsvAtomizerServiceServicer):
    """Async gRPC servicer for CSV atomization.

    Provides extraction of CSV data with automatic detection of delimiter,
    encoding, headers, and column data types.
    """

    def __init__(self) -> None:
        self._parser = CsvParser()

    # ------------------------------------------------------------------
    # ExtractCsv
    # ------------------------------------------------------------------

    async def ExtractCsv(
        self,
        request: Any,
        context: grpc.aio.ServicerContext,
    ) -> csv_pb2.CsvExtractionResponse:
        """Extract data from a CSV file.

        Automatically detects delimiter, encoding, and data types.
        """
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractCsv file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        # Download CSV from blob storage
        async with CsvBlobClient() as blob:
            csv_bytes = await blob.download_csv_bytes(request.file_id, request.blob_url)

        # Parse CSV with auto-detection
        result = self._parser.parse(csv_bytes)

        # Build proto response
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
