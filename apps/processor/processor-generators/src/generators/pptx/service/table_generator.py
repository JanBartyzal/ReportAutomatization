"""Populate table shapes in PPTX slides with data rows.

Handles ``{{TABLE:table_name}}`` placeholders by finding the target shape
and populating it with tabular data while preserving template styling.
"""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING


if TYPE_CHECKING:
    from pptx.slide import Slide
    from pptx.table import Table

logger = logging.getLogger(__name__)


def populate_table(
    slide: Slide,
    shape_name: str,
    headers: list[str],
    rows: list[list[str]],
) -> bool:
    """Populate a table shape with data.

    If the shape is a table, fills it with the provided headers and rows.
    If there are more data rows than table rows, new rows are appended.

    Args:
        slide: The slide containing the target shape.
        shape_name: Name of the shape to populate.
        headers: Column headers.
        rows: Data rows (list of cell value lists).

    Returns:
        True if the table was populated, False if shape was not found or not a table.
    """
    target_shape = None
    for shape in slide.shapes:
        if shape.name == shape_name:
            target_shape = shape
            break

    if target_shape is None:
        logger.warning("Table shape '%s' not found on slide", shape_name)
        return False

    if not target_shape.has_table:
        logger.warning("Shape '%s' is not a table", shape_name)
        return False

    table = target_shape.table
    _fill_table(table, headers, rows)
    return True


def _fill_table(table: Table, headers: list[str], rows: list[list[str]]) -> None:
    """Fill table cells with headers and data rows.

    Captures styling from the first data row to apply to any newly added rows.
    """
    num_cols = len(table.columns)

    # Capture reference style from the first row's first cell
    # (reserved for future use with font sizing)
    # ref_font_size = Pt(10)
    if len(table.rows) > 0 and len(table.rows[0].cells) > 0:
        ref_cell = table.rows[0].cells[0]
        if ref_cell.text_frame.paragraphs:
            ref_runs = ref_cell.text_frame.paragraphs[0].runs
            if ref_runs and ref_runs[0].font.size is not None:
                _ref_font_size = ref_runs[0].font.size  # noqa: F841 (reserved for future use)

    # Fill header row (row 0)
    if len(table.rows) > 0:
        for col_idx, header in enumerate(headers[:num_cols]):
            cell = table.rows[0].cells[col_idx]
            _set_cell_text(cell, header)

    # Fill data rows
    total_needed = len(rows) + 1  # +1 for header
    existing_rows = len(table.rows)

    # Add missing rows if needed
    while len(table.rows) < total_needed:
        _add_table_row(table, num_cols)

    for row_idx, row_data in enumerate(rows):
        table_row = table.rows[row_idx + 1]  # Skip header row
        for col_idx, value in enumerate(row_data[:num_cols]):
            cell = table_row.cells[col_idx]
            _set_cell_text(cell, value)

    logger.info(
        "Populated table: %d headers, %d data rows (existing=%d, needed=%d)",
        len(headers),
        len(rows),
        existing_rows,
        total_needed,
    )


def _set_cell_text(cell, text: str) -> None:
    """Set cell text while trying to preserve formatting."""
    if cell.text_frame.paragraphs:
        paragraph = cell.text_frame.paragraphs[0]
        if paragraph.runs:
            paragraph.runs[0].text = text
            # Clear any additional runs
            for run in paragraph.runs[1:]:
                run.text = ""
        else:
            paragraph.text = text
    else:
        cell.text = text


def _add_table_row(table: Table, num_cols: int) -> None:
    """Add a new row to the table by cloning the XML of the last row."""
    from copy import deepcopy

    tbl = table._tbl
    # Clone the last row
    last_tr = tbl.findall(f"{{{tbl.nsmap['a']}}}tr")[-1]
    new_tr = deepcopy(last_tr)
    # Clear text in cloned cells
    for tc in new_tr.findall(f".//{{{tbl.nsmap['a']}}}t"):
        tc.text = ""
    tbl.append(new_tr)
