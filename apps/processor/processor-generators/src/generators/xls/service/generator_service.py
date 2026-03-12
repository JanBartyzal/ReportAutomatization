"""gRPC servicer implementation for ExcelGeneratorService.

Implements the ``GenerateReport`` and ``BatchGenerate`` RPCs defined in
``generator.v1.excel_generator`` proto.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import TYPE_CHECKING

import grpc

# Generated proto stubs
from common.v1 import common_pb2  # type: ignore[import-untyped]
from generator.v1 import excel_generator_pb2, excel_generator_pb2_grpc  # type: ignore[import-untyped]

from src.common.blob_client import GenXlsBlobClient
from src.generators.xls.models.context import extract_context_from_grpc
from src.generators.xls.service.excel_renderer import ChartInput, ChartSeriesInput, ExcelRenderer, RenderResult, TableInput

if TYPE_CHECKING:
    pass

logger = logging.getLogger(__name__)


class ExcelGeneratorServiceImpl(excel_generator_pb2_grpc.ExcelGeneratorServiceServicer):
    """gRPC servicer for Excel report generation."""

    def __init__(self) -> None:
        self._renderer = ExcelRenderer()

    async def GenerateReport(
        self,
        request: excel_generator_pb2.GenerateReportRequest,
        context: grpc.aio.ServicerContext,
    ) -> excel_generator_pb2.GenerateReportResponse:
        """Generate a single Excel report from template and data."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] GenerateReport template_id=%s report_id=%s",
            req_ctx.correlation_id,
            request.template_id,
            request.report_id,
        )

        try:
            result = await self._generate_single(request)
            return result
        except Exception as exc:
            logger.error(
                "[%s] GenerateReport failed for report_id=%s: %s",
                req_ctx.correlation_id,
                request.report_id,
                exc,
                exc_info=True,
            )
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Generation failed: {exc}")
            return excel_generator_pb2.GenerateReportResponse(
                report_id=request.report_id,
                missing_placeholders=[],
                generated_at=_now_iso(),
            )

    async def BatchGenerate(
        self,
        request: excel_generator_pb2.BatchGenerateRequest,
        context: grpc.aio.ServicerContext,
    ) -> excel_generator_pb2.BatchGenerateResponse:
        """Batch generate Excel reports for multiple report IDs."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] BatchGenerate template_id=%s report_count=%d",
            req_ctx.correlation_id,
            request.template_id,
            len(request.report_ids),
        )

        results: list[excel_generator_pb2.GenerateReportResponse] = []
        successful = 0
        failed = 0

        for report_id in request.report_ids:
            try:
                # Build a single-report request reusing the batch template
                single_request = excel_generator_pb2.GenerateReportRequest(
                    context=request.context,
                    template_id=request.template_id,
                    report_id=report_id,
                )
                result = await self._generate_single(single_request)
                results.append(result)
                successful += 1
                logger.info("[%s] Batch: report_id=%s succeeded", req_ctx.correlation_id, report_id)
            except Exception as exc:
                failed += 1
                logger.error(
                    "[%s] Batch: report_id=%s failed: %s",
                    req_ctx.correlation_id,
                    report_id,
                    exc,
                    exc_info=True,
                )
                results.append(
                    excel_generator_pb2.GenerateReportResponse(
                        report_id=report_id,
                        generated_at=_now_iso(),
                    )
                )

        logger.info(
            "[%s] BatchGenerate complete: %d successful, %d failed",
            req_ctx.correlation_id,
            successful,
            failed,
        )

        return excel_generator_pb2.BatchGenerateResponse(
            results=results,
            successful=successful,
            failed=failed,
        )

    async def _generate_single(
        self,
        request: excel_generator_pb2.GenerateReportRequest,
    ) -> excel_generator_pb2.GenerateReportResponse:
        """Internal: generate a single report.

        1. Download template from blob storage (if template_id provided)
        2. Convert proto data to renderer input
        3. Render Excel
        4. Upload result to blob storage
        5. Return response with blob reference
        """
        async with GenXlsBlobClient() as blob:
            # 1. Download template (optional)
            template_bytes: bytes | None = None
            if request.template_id:
                template_blob_url = f"{request.template_id}/current/template.xlsx"
                template_bytes = await blob.download_template(request.template_id, template_blob_url)

            # 2. Convert proto data to renderer input
            text_data = dict(request.text_placeholders)

            table_data: dict[str, TableInput] = {}
            for table_pb in request.tables:
                table_data[table_pb.placeholder_key] = TableInput(
                    headers=list(table_pb.headers),
                    rows=[list(row.cells) for row in table_pb.rows],
                )

            chart_data: dict[str, ChartInput] = {}
            for chart_pb in request.charts:
                chart_data[chart_pb.placeholder_key] = ChartInput(
                    chart_type=chart_pb.chart_type,
                    labels=list(chart_pb.labels),
                    series=[
                        ChartSeriesInput(name=s.name, values=list(s.values))
                        for s in chart_pb.series
                    ],
                )

            # 3. Render
            result: RenderResult = self._renderer.render(
                template_bytes,
                text_data,
                table_data,
                chart_data,
            )

            # 4. Upload generated Excel
            generated_url = await blob.upload_generated(request.report_id, result.xlsx_bytes)

        # 5. Build response
        return excel_generator_pb2.GenerateReportResponse(
            report_id=request.report_id,
            generated_file=common_pb2.BlobReference(
                blob_url=generated_url,
                content_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                size_bytes=len(result.xlsx_bytes),
            ),
            missing_placeholders=result.missing_placeholders,
            generated_at=_now_iso(),
        )


def _now_iso() -> str:
    """Return current UTC time in ISO 8601 format."""
    return datetime.now(timezone.utc).isoformat()
