"""gRPC servicer implementation for PptxGeneratorService.

Implements the ``GenerateReport`` and ``BatchGenerate`` RPCs defined in
``generator.v1.pptx_generator`` proto.
"""

from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import TYPE_CHECKING

import grpc

# Generated proto stubs
from common.v1 import common_pb2  # type: ignore[import-untyped]
from generator.v1 import pptx_generator_pb2, pptx_generator_pb2_grpc  # type: ignore[import-untyped]

from src.client.blob_client import GenPptxBlobClient
from src.models.context import extract_context_from_grpc
from src.service.chart_generator import ChartSeriesData
from src.service.pptx_renderer import ChartInput, RenderResult, TableInput, render

if TYPE_CHECKING:
    pass

logger = logging.getLogger(__name__)


class PptxGeneratorServiceImpl(pptx_generator_pb2_grpc.PptxGeneratorServiceServicer):
    """gRPC servicer for PPTX report generation."""

    async def GenerateReport(
        self,
        request: pptx_generator_pb2.GenerateReportRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_generator_pb2.GenerateReportResponse:
        """Generate a single PPTX report from template and data."""
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
            return pptx_generator_pb2.GenerateReportResponse(
                report_id=request.report_id,
                missing_placeholders=[],
                generated_at=_now_iso(),
            )

    async def BatchGenerate(
        self,
        request: pptx_generator_pb2.BatchGenerateRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_generator_pb2.BatchGenerateResponse:
        """Batch generate PPTX reports for multiple report IDs."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] BatchGenerate template_id=%s report_count=%d",
            req_ctx.correlation_id,
            request.template_id,
            len(request.report_ids),
        )

        results: list[pptx_generator_pb2.GenerateReportResponse] = []
        successful = 0
        failed = 0

        for report_id in request.report_ids:
            try:
                # Build a single-report request reusing the batch template
                single_request = pptx_generator_pb2.GenerateReportRequest(
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
                    pptx_generator_pb2.GenerateReportResponse(
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

        return pptx_generator_pb2.BatchGenerateResponse(
            results=results,
            successful=successful,
            failed=failed,
        )

    async def _generate_single(
        self,
        request: pptx_generator_pb2.GenerateReportRequest,
    ) -> pptx_generator_pb2.GenerateReportResponse:
        """Internal: generate a single report.

        1. Download template from blob storage
        2. Convert proto data to renderer input
        3. Render PPTX
        4. Upload result to blob storage
        5. Return response with blob reference
        """
        async with GenPptxBlobClient() as blob:
            # 1. Download template
            # Template blob URL is derived from template_id
            template_blob_url = f"{request.template_id}/current/template.pptx"
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
                        ChartSeriesData(name=s.name, values=list(s.values))
                        for s in chart_pb.series
                    ],
                )

            # 3. Render
            result: RenderResult = render(template_bytes, text_data, table_data, chart_data)

            # 4. Upload generated PPTX
            generated_url = await blob.upload_generated(request.report_id, result.pptx_bytes)

        # 5. Build response
        return pptx_generator_pb2.GenerateReportResponse(
            report_id=request.report_id,
            generated_file=common_pb2.BlobReference(
                blob_url=generated_url,
                content_type="application/vnd.openxmlformats-officedocument.presentationml.presentation",
                size_bytes=len(result.pptx_bytes),
            ),
            missing_placeholders=result.missing_placeholders,
            generated_at=_now_iso(),
        )


def _now_iso() -> str:
    """Return current UTC time in ISO 8601 format."""
    return datetime.now(timezone.utc).isoformat()
