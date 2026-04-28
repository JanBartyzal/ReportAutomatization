"""gRPC service implementing ``PptxAtomizerServiceServicer``.

Each RPC downloads the PPTX from blob storage, parses it, and returns the
corresponding proto response. ``ExtractAll`` processes every slide and
collects per-slide errors for partial-success reporting.
"""

from __future__ import annotations

import logging
import os
from typing import Any, TYPE_CHECKING

import grpc

# Generated proto stub imports
from atomizer.v1 import pptx_pb2, pptx_pb2_grpc  # type: ignore[import-untyped]
from common.v1 import common_pb2  # type: ignore[import-untyped]

from src.atomizers.pptx.client.blob_client import PptxBlobClient
from src.atomizers.pptx.service.image_renderer import ImageRenderer
from src.atomizers.pptx.service.metatable_detector import MetaTableDetector
from src.atomizers.pptx.service.pptx_parser import PptxParser
from src.common.context import extract_context_from_grpc

if TYPE_CHECKING:
    from src.common.config import Settings

logger = logging.getLogger(__name__)


class PptxAtomizerService(pptx_pb2_grpc.PptxAtomizerServiceServicer):
    """Async gRPC servicer for PPTX atomization.

    Provides four RPCs:
    - ``ExtractStructure`` -- slide list + document properties.
    - ``ExtractSlideContent`` -- texts, tables (incl. MetaTable), notes for one slide.
    - ``RenderSlideImage`` -- render a slide to PNG via LibreOffice (with fallback).
    - ``ExtractAll`` -- batch extraction of all slides with partial-success handling.
    """

    def __init__(self, settings: "Settings") -> None:
        self._settings = settings
        self._parser = PptxParser()
        self._metatable_detector = MetaTableDetector(
            confidence_threshold=settings.metatable_confidence_threshold,
        )
        self._image_renderer = ImageRenderer(settings)

    # ------------------------------------------------------------------
    # ExtractStructure
    # ------------------------------------------------------------------

    async def ExtractStructure(
        self,
        request: pptx_pb2.ExtractRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_pb2.PptxStructureResponse:
        """Download a PPTX and return its high-level structure."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractStructure file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        async with PptxBlobClient(self._settings) as blob:
            pptx_bytes = await blob.download_pptx_bytes(request.file_id, request.blob_url)

        prs = self._parser.open(pptx_bytes)
        structure = self._parser.extract_structure(prs)

        slides_pb = [
            pptx_pb2.SlideMetadata(
                slide_index=s.slide_index,
                title=s.title,
                layout_name=s.layout_name,
                has_tables=s.has_tables,
                has_text=s.has_text,
                has_images=s.has_images,
                has_charts=s.has_charts,
                has_notes=s.has_notes,
            )
            for s in structure.slides
        ]

        return pptx_pb2.PptxStructureResponse(
            file_id=request.file_id,
            total_slides=structure.total_slides,
            slides=slides_pb,
            document_properties=structure.document_properties,
        )

    # ------------------------------------------------------------------
    # ExtractSlideContent
    # ------------------------------------------------------------------

    async def ExtractSlideContent(
        self,
        request: pptx_pb2.SlideRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_pb2.SlideContentResponse:
        """Extract text, tables, and notes from a specific slide."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractSlideContent file_id=%s slide=%d",
            req_ctx.correlation_id,
            request.file_id,
            request.slide_index,
        )

        async with PptxBlobClient(self._settings) as blob:
            pptx_bytes = await blob.download_pptx_bytes(request.file_id, request.blob_url)

        prs = self._parser.open(pptx_bytes)

        try:
            content = self._parser.extract_slide_content(prs, request.slide_index)
        except IndexError as exc:
            await context.abort(grpc.StatusCode.OUT_OF_RANGE, str(exc))
            raise  # pragma: no cover

        tables_pb = _tables_to_proto(content.tables)

        meta_tables = self._metatable_detector.detect(content.texts)
        tables_pb.extend(_tables_to_proto(meta_tables))

        texts_pb = [
            pptx_pb2.TextBlock(
                shape_name=t.shape_name,
                text=t.text,
                is_title=t.is_title,
                position_x=t.position_x,
                position_y=t.position_y,
            )
            for t in content.texts
        ]

        return pptx_pb2.SlideContentResponse(
            slide_index=content.slide_index,
            texts=texts_pb,
            tables=tables_pb,
            notes=content.notes,
        )

    # ------------------------------------------------------------------
    # RenderSlideImage
    # ------------------------------------------------------------------

    async def RenderSlideImage(
        self,
        request: pptx_pb2.SlideRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_pb2.SlideImageResponse:
        """Render a slide as a PNG and upload to blob storage."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] RenderSlideImage file_id=%s slide=%d",
            req_ctx.correlation_id,
            request.file_id,
            request.slide_index,
        )

        async with PptxBlobClient(self._settings) as blob:
            pptx_path = await blob.download_pptx(request.file_id, request.blob_url)

        try:
            png_bytes = await self._image_renderer.render_slide(pptx_path, request.slide_index)
        finally:
            try:
                os.unlink(pptx_path)
            except OSError as e:
                logger.debug("Could not delete temp PPTX file %s: %s", pptx_path, e)

        async with PptxBlobClient(self._settings) as blob:
            image_url = await blob.upload_slide_image(
                request.file_id,
                request.slide_index,
                png_bytes,
            )

        return pptx_pb2.SlideImageResponse(
            slide_index=request.slide_index,
            image=common_pb2.BlobReference(
                blob_url=image_url,
                content_type="image/png",
                size_bytes=len(png_bytes),
            ),
        )

    # ------------------------------------------------------------------
    # ExtractAll
    # ------------------------------------------------------------------

    async def ExtractAll(
        self,
        request: pptx_pb2.ExtractRequest,
        context: grpc.aio.ServicerContext,
    ) -> pptx_pb2.PptxFullExtractionResponse:
        """Batch-extract structure, content, and images for all slides."""
        req_ctx = extract_context_from_grpc(context)
        logger.info(
            "[%s] ExtractAll file_id=%s",
            req_ctx.correlation_id,
            request.file_id,
        )

        errors: list[pptx_pb2.ExtractionError] = []

        async with PptxBlobClient(self._settings) as blob:
            pptx_bytes = await blob.download_pptx_bytes(request.file_id, request.blob_url)
            pptx_path = await blob.download_pptx(request.file_id, request.blob_url)

        prs = self._parser.open(pptx_bytes)

        structure = self._parser.extract_structure(prs)
        structure_pb = pptx_pb2.PptxStructureResponse(
            file_id=request.file_id,
            total_slides=structure.total_slides,
            slides=[
                pptx_pb2.SlideMetadata(
                    slide_index=s.slide_index,
                    title=s.title,
                    layout_name=s.layout_name,
                    has_tables=s.has_tables,
                    has_text=s.has_text,
                    has_images=s.has_images,
                    has_charts=s.has_charts,
                    has_notes=s.has_notes,
                )
                for s in structure.slides
            ],
            document_properties=structure.document_properties,
        )

        slide_contents: list[pptx_pb2.SlideContentResponse] = []
        slide_images: list[pptx_pb2.SlideImageResponse] = []

        for idx in range(structure.total_slides):
            try:
                content = self._parser.extract_slide_content(prs, idx)

                tables_pb = _tables_to_proto(content.tables)
                meta_tables = self._metatable_detector.detect(content.texts)
                tables_pb.extend(_tables_to_proto(meta_tables))

                texts_pb = [
                    pptx_pb2.TextBlock(
                        shape_name=t.shape_name,
                        text=t.text,
                        is_title=t.is_title,
                        position_x=t.position_x,
                        position_y=t.position_y,
                    )
                    for t in content.texts
                ]

                slide_contents.append(
                    pptx_pb2.SlideContentResponse(
                        slide_index=idx,
                        texts=texts_pb,
                        tables=tables_pb,
                        notes=content.notes,
                    )
                )
            except Exception as exc:
                logger.error("Failed to extract content for slide %d: %s", idx, exc, exc_info=True)
                errors.append(
                    pptx_pb2.ExtractionError(
                        slide_index=idx,
                        error_code="CONTENT_EXTRACTION_FAILED",
                        error_message=str(exc),
                    )
                )

            try:
                png_bytes = await self._image_renderer.render_slide(pptx_path, idx)

                async with PptxBlobClient(self._settings) as blob:
                    image_url = await blob.upload_slide_image(request.file_id, idx, png_bytes)

                slide_images.append(
                    pptx_pb2.SlideImageResponse(
                        slide_index=idx,
                        image=common_pb2.BlobReference(
                            blob_url=image_url,
                            content_type="image/png",
                            size_bytes=len(png_bytes),
                        ),
                    )
                )
            except Exception as exc:
                logger.error("Failed to render image for slide %d: %s", idx, exc, exc_info=True)
                errors.append(
                    pptx_pb2.ExtractionError(
                        slide_index=idx,
                        error_code="IMAGE_RENDER_FAILED",
                        error_message=str(exc),
                    )
                )

        try:
            os.unlink(pptx_path)
        except OSError as e:
            logger.debug("Could not delete temp PPTX file %s: %s", pptx_path, e)

        if errors and len(slide_contents) == 0:
            status = common_pb2.PROCESSING_STATUS_FAILED
        elif errors:
            status = common_pb2.PROCESSING_STATUS_PARTIAL
        else:
            status = common_pb2.PROCESSING_STATUS_COMPLETED

        return pptx_pb2.PptxFullExtractionResponse(
            file_id=request.file_id,
            status=status,
            structure=structure_pb,
            slide_contents=slide_contents,
            slide_images=slide_images,
            errors=errors,
        )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _tables_to_proto(
    tables: list[Any],
) -> list[pptx_pb2.TableData]:
    """Convert internal table data to proto ``TableData`` messages."""
    return [
        pptx_pb2.TableData(
            table_id=t.table_id,
            headers=t.headers,
            rows=[pptx_pb2.TableRow(cells=r.cells) for r in t.rows],
            confidence=t.confidence,
        )
        for t in tables
    ]
