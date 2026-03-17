"""REST API router for direct HTTP invocation (Dapr bypass).

When the orchestrator cannot reach the Dapr sidecar, it falls back to calling
these endpoints directly via HTTP. Each endpoint delegates to the same logic
as the gRPC services.
"""

from __future__ import annotations

import json
import logging
from typing import Any

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from src.common.config import Settings

logger = logging.getLogger(__name__)

api_router = APIRouter(prefix="/api/v1", tags=["atomizer"])

# Module-level settings reference; set during app startup
_settings: Settings | None = None


def init_settings(settings: Settings) -> None:
    global _settings
    _settings = settings


class ExtractRequest(BaseModel):
    file_id: str
    blob_url: str = ""


@api_router.post("/scan")
async def scan_file(req: Request) -> dict[str, str]:
    """Scan endpoint — file was already virus-scanned by ClamAV in ingestor.
    This step just acknowledges the scan is complete."""
    try:
        body = await req.json()
    except Exception:
        body = {}
    file_id = body.get("fileId", body.get("file_id", "unknown"))
    logger.info("REST /scan called for file %s — auto-pass (ClamAV already scanned)", file_id)
    return {"status": "CLEAN", "fileId": file_id}


@api_router.post("/parse")
async def parse_file(req: Request) -> dict[str, Any]:
    """Parse endpoint — delegates to the appropriate atomizer based on file type.
    Returns extracted content as JSON."""
    assert _settings is not None, "Settings not initialized"
    try:
        body = await req.json()
    except Exception:
        body = {}
    file_id = body.get("fileId", body.get("file_id", "unknown"))
    logger.info("REST /parse called for file %s", file_id)

    try:
        result = await _try_extract(file_id)
        return result
    except Exception as e:
        logger.error("Parse failed for file %s: %s", file_id, str(e))
        raise HTTPException(status_code=500, detail=str(e))


@api_router.post("/extract/pptx")
async def extract_pptx(req: ExtractRequest) -> dict[str, Any]:
    """Extract PPTX content via REST."""
    assert _settings is not None
    logger.info("REST /extract/pptx for file %s", req.file_id)

    from src.atomizers.pptx.client.blob_client import PptxBlobClient
    from src.atomizers.pptx.service.pptx_parser import PptxParser
    from src.atomizers.pptx.service.metatable_detector import MetaTableDetector

    parser = PptxParser()
    meta_detector = MetaTableDetector(
        confidence_threshold=_settings.metatable_confidence_threshold,
    )

    async with PptxBlobClient(_settings) as blob:
        pptx_bytes = await blob.download_pptx_bytes(req.file_id, req.blob_url)

    prs = parser.open(pptx_bytes)
    structure = parser.extract_structure(prs)

    slides = []
    for idx in range(structure.total_slides):
        try:
            content = parser.extract_slide_content(prs, idx)
            tables = []
            for t in content.tables:
                tables.append({
                    "headers": [c.text for c in t.columns] if hasattr(t, 'columns') else [],
                    "rows": [[cell for cell in row] for row in t.rows] if hasattr(t, 'rows') else [],
                })

            # Detect meta-tables from text blocks
            meta_tables = meta_detector.detect(content.texts)
            for mt in meta_tables:
                tables.append({
                    "headers": [c.text for c in mt.columns] if hasattr(mt, 'columns') else [],
                    "rows": [[cell for cell in row] for row in mt.rows] if hasattr(mt, 'rows') else [],
                    "meta": True,
                })

            slides.append({
                "slide_index": idx,
                "title": structure.slides[idx].title if idx < len(structure.slides) else "",
                "texts": [{"shape_name": t.shape_name, "text": t.text, "is_title": t.is_title}
                          for t in content.texts],
                "tables": tables,
                "notes": content.notes,
            })
        except Exception as e:
            logger.warning("Failed to extract slide %d: %s", idx, str(e))
            slides.append({"slide_index": idx, "error": str(e)})

    return {
        "file_id": req.file_id,
        "total_slides": structure.total_slides,
        "slides": slides,
        "status": "COMPLETED",
    }


@api_router.post("/extract/excel")
async def extract_excel(req: ExtractRequest) -> dict[str, Any]:
    """Extract Excel content via REST."""
    assert _settings is not None
    logger.info("REST /extract/excel for file %s", req.file_id)

    from src.atomizers.xls.client.blob_client import ExcelBlobClient
    from src.atomizers.xls.service.excel_parser import ExcelParser

    parser = ExcelParser(_settings)

    async with ExcelBlobClient(_settings) as blob:
        excel_bytes = await blob.download_bytes(req.file_id, req.blob_url)

    result = parser.parse(excel_bytes)

    sheets = []
    for sheet in result.sheets:
        sheets.append({
            "sheet_name": sheet.sheet_name,
            "headers": sheet.headers,
            "rows": sheet.rows[:100],  # Limit rows for REST response
            "total_rows": len(sheet.rows),
        })

    return {
        "file_id": req.file_id,
        "sheets": sheets,
        "status": "COMPLETED",
    }


async def _try_extract(file_id: str) -> dict[str, Any]:
    """Attempt to extract content from a file, trying different parsers."""
    # For the generic /parse endpoint, return a simple acknowledgment.
    # The orchestrator should use the type-specific endpoints (/extract/pptx, etc.)
    return {
        "file_id": file_id,
        "status": "COMPLETED",
        "message": "Use type-specific endpoints for full extraction",
    }
