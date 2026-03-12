"""Core PPTX rendering engine.

Orchestrates placeholder substitution, table population, and chart insertion
into a PPTX template while preserving original formatting.
"""

from __future__ import annotations

import io
import logging
import re
from dataclasses import dataclass, field

from pptx import Presentation
from pptx.util import Emu, Pt
from pptx.dml.color import RGBColor

from src.generators.pptx.service.placeholder_parser import PlaceholderType, extract_placeholders
from src.generators.pptx.service.table_generator import populate_table
from src.generators.pptx.service.chart_generator import ChartData, ChartSeriesData, render_chart

logger = logging.getLogger(__name__)

_PLACEHOLDER_RE = re.compile(r"\{\{(TABLE:|CHART:)?([^}]+)\}\}")
_MISSING_COLOR = RGBColor(0xFF, 0x00, 0x00)  # Red


@dataclass
class TableInput:
    """Table data for a single placeholder."""
    headers: list[str]
    rows: list[list[str]]


@dataclass
class ChartInput:
    """Chart data for a single placeholder."""
    chart_type: str
    labels: list[str]
    series: list[ChartSeriesData] = field(default_factory=list)


@dataclass
class RenderResult:
    """Result of rendering a PPTX template."""
    pptx_bytes: bytes
    missing_placeholders: list[str]


def render(
    template_bytes: bytes,
    text_data: dict[str, str],
    table_data: dict[str, TableInput],
    chart_data: dict[str, ChartInput],
) -> RenderResult:
    """Render a PPTX template with provided data.

    Args:
        template_bytes: Raw PPTX template file.
        text_data: Map of placeholder key to replacement text value.
        table_data: Map of table placeholder key to ``TableInput``.
        chart_data: Map of chart placeholder key to ``ChartInput``.

    Returns:
        ``RenderResult`` with generated PPTX bytes and list of missing placeholders.
    """
    prs = Presentation(io.BytesIO(template_bytes))
    placeholders = extract_placeholders(prs)
    missing: list[str] = []

    # Group placeholders by type
    for ph in placeholders:
        if ph.placeholder_type == PlaceholderType.TEXT:
            if ph.key in text_data:
                _substitute_text_in_slide(prs.slides[ph.slide_index], ph.original_tag, text_data[ph.key])
            else:
                _mark_missing(prs.slides[ph.slide_index], ph.shape_name, ph.original_tag)
                missing.append(ph.original_tag)

        elif ph.placeholder_type == PlaceholderType.TABLE:
            if ph.key in table_data:
                td = table_data[ph.key]
                # Clear the placeholder tag text first
                _substitute_text_in_slide(prs.slides[ph.slide_index], ph.original_tag, "")
                populated = populate_table(
                    prs.slides[ph.slide_index],
                    ph.shape_name,
                    td.headers,
                    td.rows,
                )
                if not populated:
                    missing.append(ph.original_tag)
            else:
                _mark_missing(prs.slides[ph.slide_index], ph.shape_name, ph.original_tag)
                missing.append(ph.original_tag)

        elif ph.placeholder_type == PlaceholderType.CHART:
            if ph.key in chart_data:
                cd = chart_data[ph.key]
                chart_info = ChartData(
                    chart_type=cd.chart_type,
                    labels=cd.labels,
                    series=cd.series,
                )
                png_bytes = render_chart(chart_info)
                _insert_chart_image(prs.slides[ph.slide_index], ph.shape_name, ph.original_tag, png_bytes)
            else:
                _mark_missing(prs.slides[ph.slide_index], ph.shape_name, ph.original_tag)
                missing.append(ph.original_tag)

    # Save to bytes
    output = io.BytesIO()
    prs.save(output)
    output.seek(0)

    logger.info("Render complete: %d placeholders processed, %d missing", len(placeholders), len(missing))

    return RenderResult(pptx_bytes=output.getvalue(), missing_placeholders=missing)


def _substitute_text_in_slide(slide, tag: str, replacement: str) -> None:
    """Replace a placeholder tag in all text frames on a slide, preserving formatting.

    Handles placeholders that may span multiple runs by merging adjacent runs
    that together form the tag, then performing the replacement on the merged run.
    """
    for shape in slide.shapes:
        if shape.has_text_frame:
            _replace_in_text_frame(shape.text_frame, tag, replacement)
        if shape.has_table:
            for row in shape.table.rows:
                for cell in row.cells:
                    _replace_in_text_frame(cell.text_frame, tag, replacement)


def _replace_in_text_frame(text_frame, tag: str, replacement: str) -> None:
    """Replace tag in a text frame, preserving run-level formatting."""
    for paragraph in text_frame.paragraphs:
        # First try: tag is fully within a single run
        for run in paragraph.runs:
            if tag in run.text:
                run.text = run.text.replace(tag, replacement)
                return

        # Second try: tag spans multiple runs - merge and replace
        full_text = "".join(run.text for run in paragraph.runs)
        if tag not in full_text:
            continue

        # Find which runs contain the tag and merge them
        _merge_and_replace_runs(paragraph, tag, replacement)


def _merge_and_replace_runs(paragraph, tag: str, replacement: str) -> None:
    """Merge runs that together contain the tag, then replace."""
    runs = list(paragraph.runs)
    if not runs:
        return

    # Build cumulative text to find tag boundaries
    cumulative = ""
    for i, run in enumerate(runs):
        cumulative += run.text

    full_text = cumulative
    tag_start = full_text.find(tag)
    if tag_start == -1:
        return

    # Find which runs the tag spans
    char_pos = 0
    start_run_idx = -1
    end_run_idx = -1

    for i, run in enumerate(runs):
        run_start = char_pos
        run_end = char_pos + len(run.text)

        if start_run_idx == -1 and run_end > tag_start:
            start_run_idx = i

        if run_end >= tag_start + len(tag):
            end_run_idx = i
            break

        char_pos = run_end

    if start_run_idx == -1 or end_run_idx == -1:
        return

    # Merge text from start_run to end_run into start_run
    merged_text = "".join(runs[i].text for i in range(start_run_idx, end_run_idx + 1))
    runs[start_run_idx].text = merged_text.replace(tag, replacement)

    # Clear subsequent merged runs
    for i in range(start_run_idx + 1, end_run_idx + 1):
        runs[i].text = ""


def _mark_missing(slide, shape_name: str, tag: str) -> None:
    """Mark a placeholder as missing with a red border and 'DATA MISSING' text."""
    for shape in slide.shapes:
        if shape.name == shape_name:
            # Add red border
            if hasattr(shape, "line"):
                shape.line.color.rgb = _MISSING_COLOR
                shape.line.width = Pt(2)

            # Replace tag with DATA MISSING
            if shape.has_text_frame:
                _replace_in_text_frame(shape.text_frame, tag, "DATA MISSING")

            logger.warning("Marked missing placeholder: %s in shape '%s'", tag, shape_name)
            return


def _insert_chart_image(slide, shape_name: str, tag: str, png_bytes: bytes) -> None:
    """Replace a placeholder shape with a chart image at the same position and size."""
    target = None
    for shape in slide.shapes:
        if shape.name == shape_name:
            target = shape
            break

    if target is None:
        logger.warning("Chart shape '%s' not found", shape_name)
        return

    # Capture position and size before modifying
    left = target.left
    top = target.top
    width = target.width
    height = target.height

    # Clear the placeholder text
    if target.has_text_frame:
        _replace_in_text_frame(target.text_frame, tag, "")

    # Add chart image at the same position
    image_stream = io.BytesIO(png_bytes)
    slide.shapes.add_picture(image_stream, left, top, width, height)

    logger.info("Inserted chart image at shape '%s' (%dx%d)", shape_name, width, height)
