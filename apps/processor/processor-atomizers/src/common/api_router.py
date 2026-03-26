"""REST API router for direct HTTP invocation (Dapr bypass).

When the orchestrator cannot reach the Dapr sidecar, it falls back to calling
these endpoints directly via HTTP. Each endpoint delegates to the same logic
as the gRPC services.
"""

from __future__ import annotations

import json
import logging
import time
from pathlib import Path
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


_metadata_cache: list[dict[str, Any]] | None = None
_metadata_cache_time: float = 0


def _load_metadata_templates() -> list[dict[str, Any]]:
    """Load slide metadata templates from local files and cache.

    Templates are loaded from:
    1. Local files in /app/data/ or configured directory
    2. In future: from engine-data API
    """
    global _metadata_cache, _metadata_cache_time
    import time

    # Cache for 60 seconds
    if _metadata_cache is not None and time.time() - _metadata_cache_time < 60:
        return _metadata_cache

    templates = []

    # Load from local files (mounted or bundled)
    search_dirs = [
        Path("/app/data"),
        Path(__file__).parent.parent.parent.parent / "tests" / "UAT" / "data",
        Path("/data"),
    ]

    for search_dir in search_dirs:
        if not search_dir.exists():
            continue
        for json_file in search_dir.glob("*metadata*.json"):
            try:
                with open(json_file, "r", encoding="utf-8") as f:
                    tmpl = json.load(f)
                if "slides" in tmpl:
                    templates.append(tmpl)
                    logger.info("Loaded metadata template: %s (%s)",
                                tmpl.get("name", json_file.name), json_file)
            except Exception as e:
                logger.warning("Failed to load metadata template %s: %s", json_file, e)

    _metadata_cache = templates
    _metadata_cache_time = time.time()

    if templates:
        logger.info("Loaded %d metadata template(s)", len(templates))
    return templates


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
    file_type = body.get("fileType", body.get("file_type", "")).upper()
    blob_url = body.get("blobUrl", body.get("blob_url", ""))
    logger.info("REST /parse called for file %s, type %s, blobUrl %s", file_id, file_type, blob_url)

    try:
        result = await _try_extract(file_id, file_type, blob_url)
        return result
    except Exception as e:
        logger.error("Parse failed for file %s: %s", file_id, str(e))
        raise HTTPException(status_code=500, detail=str(e))


@api_router.post("/extract/pptx")
async def extract_pptx(req: ExtractRequest) -> dict[str, Any]:
    """Extract PPTX content via REST.

    Tries metadata-driven extraction first (SpatialTableExtractor) using all
    available slide_metadata templates. Falls back to generic MetaTableDetector
    if no template matches. Stores info about which template was used.
    """
    assert _settings is not None
    logger.info("REST /extract/pptx for file %s", req.file_id)

    from src.atomizers.pptx.client.blob_client import PptxBlobClient
    from src.atomizers.pptx.service.pptx_parser import PptxParser
    from src.atomizers.pptx.service.metatable_detector import MetaTableDetector
    from src.atomizers.pptx.service.spatial_extractor import SpatialTableExtractor, match_templates

    parser = PptxParser()
    meta_detector = MetaTableDetector(
        confidence_threshold=_settings.metatable_confidence_threshold,
    )

    async with PptxBlobClient(_settings) as blob:
        pptx_bytes = await blob.download_pptx_bytes(req.file_id, req.blob_url)

    prs = parser.open(pptx_bytes)
    structure = parser.extract_structure(prs)

    # Load available metadata templates
    metadata_templates = _load_metadata_templates()

    slides = []
    template_used = None
    template_confidence = 0.0

    for idx in range(structure.total_slides):
        try:
            slide = prs.slides[idx]
            slide_title = structure.slides[idx].title if idx < len(structure.slides) else ""

            # 1. Try metadata-driven extraction
            spatial_result = None
            if metadata_templates:
                matches = match_templates(slide, metadata_templates)
                if matches:
                    best_template, best_score = matches[0]
                    logger.info("Slide %d: best metadata match '%s' (score=%.2f)",
                                idx, best_template.get("name", "?"), best_score)

                    if best_score >= 0.3:
                        slides_def = best_template.get("slides", [])
                        # Find matching slide definition
                        slide_def = slides_def[0] if slides_def else None
                        for sd in slides_def:
                            if sd.get("slide_index") == idx:
                                slide_def = sd
                                break

                        if slide_def:
                            extractor = SpatialTableExtractor(best_template)
                            spatial_result = extractor.extract_slide(slide, slide_def)
                            template_used = best_template.get("name", "")
                            template_confidence = best_score

            # 2. Build slide output
            if spatial_result and spatial_result.tables:
                # Metadata-driven extraction succeeded
                tables = []
                for t in spatial_result.tables:
                    tables.append({
                        "table_id": t.table_id,
                        "headers": t.headers,
                        "rows": [row.cells for row in t.rows],
                        "confidence": t.confidence,
                        "extraction_method": "spatial_metadata",
                        "template_name": spatial_result.template_name,
                    })

                slides.append({
                    "slide_index": idx,
                    "title": slide_title,
                    "texts": [{"role": k, "text": v} for k, v in spatial_result.text_elements.items()],
                    "tables": tables,
                    "notes": "",
                    "extraction_method": "spatial_metadata",
                    "template_name": spatial_result.template_name,
                    "template_confidence": spatial_result.confidence,
                    "warnings": spatial_result.warnings,
                })
                logger.info("Slide %d: extracted %d tables via metadata template '%s'",
                            idx, len(tables), spatial_result.template_name)
            else:
                # 3. Fallback to generic extraction
                content = parser.extract_slide_content(prs, idx)
                tables = []

                # Native PPTX tables
                for t in content.tables:
                    tables.append({
                        "table_id": t.table_id,
                        "headers": t.headers,
                        "rows": [row.cells for row in t.rows],
                        "confidence": t.confidence,
                        "extraction_method": "native_table",
                    })

                # MetaTable detector (delimiter heuristics)
                meta_tables = meta_detector.detect(content.texts)
                for mt in meta_tables:
                    tables.append({
                        "table_id": mt.table_id,
                        "headers": mt.headers,
                        "rows": [row.cells for row in mt.rows],
                        "confidence": mt.confidence,
                        "extraction_method": "metatable_heuristic",
                    })

                slides.append({
                    "slide_index": idx,
                    "title": slide_title,
                    "texts": [{"shape_name": t.shape_name, "text": t.text, "is_title": t.is_title}
                              for t in content.texts],
                    "tables": tables,
                    "notes": content.notes,
                    "extraction_method": "generic",
                })

        except Exception as e:
            logger.warning("Failed to extract slide %d: %s", idx, str(e))
            slides.append({"slide_index": idx, "error": str(e)})

    # Check if any slide used metatable extraction
    has_metatable = any(
        s.get("extraction_method") in ("metatable_heuristic", "spatial_metadata")
        or any(
            t.get("extraction_method") in ("metatable_heuristic", "spatial_metadata")
            for t in s.get("tables", [])
            if isinstance(t, dict)
        )
        for s in slides
        if isinstance(s, dict)
    )

    return {
        "file_id": req.file_id,
        "total_slides": structure.total_slides,
        "slides": slides,
        "status": "COMPLETED",
        "template_used": template_used,
        "template_confidence": template_confidence,
        "metatable_support": True,
        "metatable_detected": has_metatable,
    }


@api_router.post("/extract/excel")
async def extract_excel(req: ExtractRequest) -> dict[str, Any]:
    """Extract Excel content via REST."""
    assert _settings is not None
    logger.info("REST /extract/excel for file %s", req.file_id)

    from src.atomizers.xls.client.blob_client import ExcelBlobClient
    from src.atomizers.xls.service.excel_parser import ExcelParser

    parser = ExcelParser()

    async with ExcelBlobClient(_settings) as blob:
        excel_bytes = await blob.download_bytes(req.file_id, req.blob_url)

    wb = ExcelParser.open(excel_bytes)
    structure = parser.extract_structure(wb)

    sheets = []
    for sheet_meta in structure.sheets:
        content = parser.extract_sheet_content(wb, sheet_meta.sheet_index)
        rows_data = []
        for row in content.rows[:100]:
            rows_data.append(row.cells if hasattr(row, 'cells') else list(row))
        sheets.append({
            "sheet_name": content.sheet_name,
            "headers": content.headers,
            "rows": rows_data,
            "total_rows": len(content.rows),
        })

    return {
        "file_id": req.file_id,
        "sheets": sheets,
        "status": "COMPLETED",
    }


async def _try_extract(file_id: str, file_type: str = "", blob_url: str = "") -> dict[str, Any]:
    """Attempt to extract content from a file by routing to the appropriate parser.

    Delegates to the type-specific extract endpoints based on ``file_type``.
    """
    assert _settings is not None, "Settings not initialized"

    extract_req = ExtractRequest(file_id=file_id, blob_url=blob_url)

    # Route to the correct parser based on file type (extension or MIME type)
    ft_upper = file_type.upper()

    pptx_types = {"PPTX", "PPT", "POWERPOINT"}
    excel_types = {"XLSX", "XLS", "EXCEL", "CSV"}
    pptx_mimes = {"APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.PRESENTATIONML.PRESENTATION",
                   "APPLICATION/VND.MS-POWERPOINT"}
    excel_mimes = {"APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.SPREADSHEETML.SHEET",
                    "APPLICATION/VND.MS-EXCEL", "TEXT/CSV"}

    if ft_upper in pptx_types or ft_upper in pptx_mimes:
        logger.info("Routing file %s to PPTX parser", file_id)
        return await extract_pptx(extract_req)

    if ft_upper in excel_types or ft_upper in excel_mimes:
        logger.info("Routing file %s to Excel parser", file_id)
        return await extract_excel(extract_req)

    # Unknown file type — try Excel first (most common), then PPTX
    if file_type:
        logger.warning("Unknown file type '%s' for file %s, attempting auto-detection", file_type, file_id)
    else:
        logger.warning("No file type provided for file %s, attempting auto-detection", file_id)

    for parser_name, parser_fn in [("Excel", extract_excel), ("PPTX", extract_pptx)]:
        try:
            logger.info("Auto-detect: trying %s parser for file %s", parser_name, file_id)
            result = await parser_fn(extract_req)
            if result.get("status") == "COMPLETED":
                logger.info("Auto-detect: %s parser succeeded for file %s", parser_name, file_id)
                return result
        except Exception as e:
            logger.debug("Auto-detect: %s parser failed for file %s: %s", parser_name, file_id, str(e))
            continue

    raise ValueError(f"No parser could handle file {file_id} (fileType={file_type})")
