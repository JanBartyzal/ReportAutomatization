"""Parse placeholder tags from PPTX templates.

Supports three placeholder types:
- ``{{variable_name}}`` -- text replacement
- ``{{TABLE:table_name}}`` -- table data population
- ``{{CHART:metric_name}}`` -- chart image insertion
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from pptx.presentation import Presentation

# Regex to match {{...}} placeholders, optionally prefixed with TABLE: or CHART:
_PLACEHOLDER_RE = re.compile(r"\{\{(TABLE:|CHART:)?([^}]+)\}\}")


class PlaceholderType(Enum):
    TEXT = "TEXT"
    TABLE = "TABLE"
    CHART = "CHART"


@dataclass(frozen=True, slots=True)
class PlaceholderInfo:
    """Parsed placeholder metadata."""

    key: str
    """The placeholder key without prefix (e.g. ``it_costs``)."""

    placeholder_type: PlaceholderType
    """TEXT, TABLE, or CHART."""

    original_tag: str
    """The full tag string as found in the template (e.g. ``{{TABLE:it_costs}}``)."""

    slide_index: int
    """Zero-based index of the slide containing this placeholder."""

    shape_name: str
    """Name of the shape containing this placeholder."""


def parse_placeholders_from_text(text: str) -> list[tuple[str, PlaceholderType, str]]:
    """Extract placeholder tuples from a text string.

    Returns:
        List of (key, type, original_tag) tuples.
    """
    results: list[tuple[str, PlaceholderType, str]] = []
    for match in _PLACEHOLDER_RE.finditer(text):
        prefix = match.group(1) or ""
        key = match.group(2).strip()
        original_tag = match.group(0)

        if prefix == "TABLE:":
            ptype = PlaceholderType.TABLE
        elif prefix == "CHART:":
            ptype = PlaceholderType.CHART
        else:
            ptype = PlaceholderType.TEXT

        results.append((key, ptype, original_tag))
    return results


def extract_placeholders(prs: Presentation) -> list[PlaceholderInfo]:
    """Scan all slides in a presentation for placeholder tags.

    Args:
        prs: An opened python-pptx ``Presentation`` object.

    Returns:
        Deduplicated list of ``PlaceholderInfo`` objects.
    """
    placeholders: list[PlaceholderInfo] = []
    seen: set[str] = set()

    for slide_idx, slide in enumerate(prs.slides):
        for shape in slide.shapes:
            text = ""
            if shape.has_text_frame:
                text = shape.text_frame.text
            elif shape.has_table:
                # Scan all cells in the table
                for row in shape.table.rows:
                    for cell in row.cells:
                        text += " " + cell.text_frame.text

            for key, ptype, original_tag in parse_placeholders_from_text(text):
                dedup_key = f"{slide_idx}:{shape.name}:{original_tag}"
                if dedup_key not in seen:
                    seen.add(dedup_key)
                    placeholders.append(
                        PlaceholderInfo(
                            key=key,
                            placeholder_type=ptype,
                            original_tag=original_tag,
                            slide_index=slide_idx,
                            shape_name=shape.name,
                        )
                    )

    return placeholders
