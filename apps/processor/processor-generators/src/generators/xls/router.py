"""FastAPI router for Excel HTTP endpoints.

Exposes UpdateSheet as a REST endpoint callable via Dapr service invocation.
Excel binary is base64-encoded in the request/response JSON to be transport-safe.
"""

from __future__ import annotations

import base64
import logging
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from src.generators.xls.service.sheet_updater import SheetUpdater

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1/excel", tags=["excel"])

_sheet_updater = SheetUpdater()


class CellValue(BaseModel):
    """A single cell value with an explicit type tag."""
    type: str = Field(..., description="One of: string, number, bool, date")
    value: Any = Field(..., description="Cell value matching the declared type")


class UpdateSheetRequest(BaseModel):
    excel_base64: str = Field(
        default="",
        description="Base64-encoded existing Excel binary. Empty string creates a new workbook.",
    )
    sheet_name: str = Field(..., description="Target sheet name to overwrite")
    headers: list[str] = Field(default_factory=list, description="Column header labels")
    data_rows: list[list[CellValue]] = Field(
        default_factory=list, description="Rows of typed cell values"
    )
    auto_filter: bool = Field(default=True, description="Apply auto-filter on header row")
    freeze_header: bool = Field(default=True, description="Freeze first row")
    auto_column_width: bool = Field(default=True, description="Auto-fit column widths")


class UpdateSheetResponse(BaseModel):
    excel_base64: str = Field(..., description="Base64-encoded updated Excel binary")
    rows_written: int
    sheet_name: str


@router.post("/update-sheet", response_model=UpdateSheetResponse)
async def update_sheet(request: UpdateSheetRequest) -> UpdateSheetResponse:
    """Overwrite a single named sheet in an Excel workbook.

    All other sheets (including their charts, formulas, and pivot tables) are
    preserved unchanged. If the workbook does not exist yet (empty
    ``excel_base64``), a new workbook is created.
    """
    # Decode incoming Excel bytes (empty string → None → new workbook)
    excel_binary: bytes | None = None
    if request.excel_base64:
        try:
            excel_binary = base64.b64decode(request.excel_base64)
        except Exception as exc:
            raise HTTPException(status_code=400, detail=f"Invalid base64 excel_base64: {exc}") from exc

    # Convert CellValue models to plain Python values
    plain_rows: list[list[Any]] = []
    for row in request.data_rows:
        plain_row: list[Any] = []
        for cell in row:
            plain_row.append(_coerce_cell(cell))
        plain_rows.append(plain_row)

    try:
        result = _sheet_updater.update_sheet(
            excel_binary=excel_binary,
            sheet_name=request.sheet_name,
            headers=request.headers,
            data_rows=plain_rows,
            auto_filter=request.auto_filter,
            freeze_header=request.freeze_header,
            auto_column_width=request.auto_column_width,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        logger.exception("UpdateSheet failed for sheet '%s': %s", request.sheet_name, exc)
        raise HTTPException(status_code=500, detail=f"Sheet update failed: {exc}") from exc

    return UpdateSheetResponse(
        excel_base64=base64.b64encode(result.xlsx_bytes).decode(),
        rows_written=result.rows_written,
        sheet_name=result.sheet_name,
    )


def _coerce_cell(cell: CellValue) -> Any:
    """Convert a typed CellValue to a plain Python value suitable for openpyxl."""
    if cell.type == "number":
        try:
            v = float(cell.value)
            return int(v) if v == int(v) else v
        except (TypeError, ValueError):
            return cell.value
    if cell.type == "bool":
        if isinstance(cell.value, bool):
            return cell.value
        return str(cell.value).lower() in ("true", "1", "yes")
    if cell.type == "date":
        from datetime import datetime
        try:
            return datetime.fromisoformat(str(cell.value))
        except ValueError:
            return cell.value
    # Default: string
    return str(cell.value) if cell.value is not None else ""
