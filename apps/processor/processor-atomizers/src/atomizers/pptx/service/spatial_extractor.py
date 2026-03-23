"""Metadata-driven spatial table extractor for PPTX pseudo-tables.

Reconstructs tables from spatially arranged shapes (text boxes, rectangles,
lines) using a slide_metadata JSON template that defines column regions,
row detection methods, and parsing rules.

This replaces the generic MetaTableDetector (delimiter-based) when a matching
metadata template is available.
"""

from __future__ import annotations

import fnmatch
import logging
import re
from dataclasses import dataclass, field
from typing import Any

from pptx.enum.shapes import MSO_SHAPE_TYPE

from src.atomizers.pptx.service.pptx_parser import TableDataParsed, TableRowData

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class ShapeData:
    """Raw shape data with position and content."""
    name: str
    shape_type: str
    text: str
    left: int   # EMU
    top: int    # EMU
    width: int  # EMU
    height: int # EMU


@dataclass
class ExtractionResult:
    """Result of metadata-driven extraction for one slide."""
    tables: list[TableDataParsed]
    text_elements: dict[str, str]  # role -> text (title, subtitle, annotation)
    template_name: str = ""
    template_version: int = 0
    confidence: float = 0.0
    warnings: list[str] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Spatial Table Extractor
# ---------------------------------------------------------------------------

class SpatialTableExtractor:
    """Extracts structured tables from PPTX shapes using a metadata template.

    The metadata template defines:
    - Column regions (EMU x-coordinate ranges)
    - Row detection method (horizontal lines or vertical position)
    - Shape name patterns for filtering
    - Number parsing rules
    """

    def __init__(self, template: dict[str, Any]):
        self.template = template
        self.parsing_rules = template.get("parsing_rules", {})
        self.number_rules = self.parsing_rules.get("number_parsing", {})
        self.strip_patterns = self.number_rules.get("strip_patterns", ["M€", "M€", "€", " "])
        self.wip_markers = self.number_rules.get("wip_markers", ["WIP", "N/A", "TBD", "-"])

    def extract_slide(self, slide: Any, slide_def: dict) -> ExtractionResult:
        """Extract tables and text from a slide using metadata definition.

        Args:
            slide: python-pptx Slide object
            slide_def: The slide definition from metadata template

        Returns:
            ExtractionResult with tables and text elements
        """
        # Collect all shapes with position data
        shapes = self._collect_shapes(slide)
        logger.info("Collected %d shapes from slide", len(shapes))

        # Filter out ignored shapes
        ignore_patterns = slide_def.get("ignore_shapes", {}).get("patterns", [])
        shapes = self._filter_shapes(shapes, ignore_patterns)
        logger.info("After filtering: %d shapes", len(shapes))

        # Extract tables
        tables = []
        for table_def in slide_def.get("tables", []):
            table = self._extract_table(shapes, table_def)
            if table:
                tables.append(table)

        # Extract text elements
        text_elements = self._extract_text_elements(shapes, slide_def.get("text_elements_extraction", {}))

        return ExtractionResult(
            tables=tables,
            text_elements=text_elements,
            template_name=self.template.get("name", ""),
            template_version=self.template.get("version", 0),
            confidence=0.9 if tables else 0.3,
            warnings=[],
        )

    def try_match_slide(self, slide: Any, slide_def: dict) -> float:
        """Score how well a slide matches a metadata template definition.

        Returns confidence 0.0-1.0 based on:
        - Layout name match
        - Title text match
        - Shape count similarity
        - Presence of expected header text patterns
        """
        score = 0.0
        total_checks = 0

        # Check title
        title_detection = slide_def.get("title_detection", {})
        if title_detection:
            total_checks += 1
            actual_title = ""
            for shape in slide.shapes:
                if shape.has_text_frame and hasattr(shape, 'placeholder_format'):
                    try:
                        if shape.placeholder_format and shape.placeholder_format.idx in (0, 1):
                            actual_title = shape.text_frame.text.strip()
                            break
                    except Exception:
                        pass
            if actual_title:
                score += 0.3  # Has a title

        # Check for expected table headers
        for table_def in slide_def.get("tables", []):
            columns = table_def.get("columns", [])
            for col in columns:
                pattern = col.get("header_text_pattern", "")
                if pattern:
                    total_checks += 1
                    for shape in slide.shapes:
                        if shape.has_text_frame:
                            text = shape.text_frame.text.strip()
                            if fnmatch.fnmatch(text, pattern) or pattern.rstrip("*") in text:
                                score += 0.15
                                break

        # Check shape count (rough match)
        shape_count = len(list(slide.shapes))
        if shape_count > 20:  # Pseudo-tables typically have many shapes
            score += 0.1

        return min(score, 1.0)

    # -- private methods ---------------------------------------------------

    def _collect_shapes(self, slide: Any) -> list[ShapeData]:
        """Collect all shapes with their position and content."""
        shapes = []
        for shape in slide.shapes:
            text = ""
            if shape.has_text_frame:
                text = shape.text_frame.text

            shape_type = str(shape.shape_type) if hasattr(shape, 'shape_type') else "UNKNOWN"

            shapes.append(ShapeData(
                name=shape.name or "",
                shape_type=shape_type,
                text=text,
                left=int(shape.left) if shape.left else 0,
                top=int(shape.top) if shape.top else 0,
                width=int(shape.width) if shape.width else 0,
                height=int(shape.height) if shape.height else 0,
            ))
        return shapes

    def _filter_shapes(self, shapes: list[ShapeData], ignore_patterns: list[dict]) -> list[ShapeData]:
        """Remove shapes matching ignore patterns."""
        filtered = []
        for s in shapes:
            should_ignore = False
            for pattern in ignore_patterns:
                if "type" in pattern and pattern["type"] in s.shape_type:
                    should_ignore = True
                    break
                if "name_pattern" in pattern and fnmatch.fnmatch(s.name, pattern["name_pattern"]):
                    should_ignore = True
                    break
            if not should_ignore:
                filtered.append(s)
        return filtered

    def _extract_table(self, shapes: list[ShapeData], table_def: dict) -> TableDataParsed | None:
        """Extract a single table based on its metadata definition."""
        columns = table_def.get("columns", [])
        row_detection = table_def.get("row_detection", {})
        table_id = table_def.get("table_id", "unknown")
        output_sheet = table_def.get("output_sheet_name", table_id)

        if not columns:
            return None

        # Get data region
        data_region = row_detection.get("data_region", {})
        top_boundary = data_region.get("top_emu", 0)
        bottom_boundary = data_region.get("bottom_emu", 999999999)

        # Collect shapes per column (within data region)
        column_shapes: list[list[ShapeData]] = [[] for _ in columns]
        for s in shapes:
            if not s.text.strip():
                continue
            if s.top < top_boundary or s.top > bottom_boundary:
                continue

            # Match shape to column by x-position
            for col_idx, col_def in enumerate(columns):
                region = col_def.get("region", {})
                left_min = region.get("left_min_emu", 0)
                left_max = region.get("left_max_emu", 999999999)

                # Check if shape name matches column pattern
                name_pattern = col_def.get("shape_name_pattern", "*")
                name_matches = fnmatch.fnmatch(s.name, name_pattern) or name_pattern == "*"

                if left_min <= s.left <= left_max and name_matches:
                    column_shapes[col_idx].append(s)
                    break

        # Detect rows using the configured method
        method = row_detection.get("method", "vertical_position")
        multivalue_split = row_detection.get("multivalue_split", "\n")

        if method == "horizontal_lines":
            rows = self._detect_rows_by_lines(shapes, column_shapes, columns,
                                              row_detection, multivalue_split)
        else:
            rows = self._detect_rows_by_position(column_shapes, columns, multivalue_split)

        if not rows:
            logger.info("No rows extracted for table '%s'", table_id)
            return None

        # Build headers
        headers = []
        for col_def in columns:
            header_pattern = col_def.get("header_text_pattern", col_def.get("id", ""))
            # Clean header: remove wildcards
            header = header_pattern.rstrip("*").strip()
            headers.append(header)

        logger.info("Extracted table '%s': %d headers, %d rows", output_sheet, len(headers), len(rows))

        return TableDataParsed(
            table_id=output_sheet,
            headers=headers,
            rows=[TableRowData(cells=row) for row in rows],
            confidence=0.9,
        )

    def _detect_rows_by_lines(self, all_shapes: list[ShapeData],
                               column_shapes: list[list[ShapeData]],
                               columns: list[dict],
                               row_detection: dict,
                               multivalue_split: str) -> list[list[str]]:
        """Detect rows using horizontal line separators."""
        # Find separator lines
        sep_pattern = row_detection.get("separator_name_pattern", "Straight Connector *")
        line_shapes = [s for s in all_shapes
                       if fnmatch.fnmatch(s.name, sep_pattern) and s.height == 0]
        line_y_positions = sorted(set(s.top for s in line_shapes))

        if not line_y_positions:
            # Fallback to position-based detection
            return self._detect_rows_by_position(column_shapes, columns, multivalue_split)

        # Each row is between two consecutive line positions
        rows = []
        data_top = row_detection.get("data_region", {}).get("top_emu", 0)
        boundaries = [data_top] + line_y_positions

        for i in range(len(boundaries)):
            y_min = boundaries[i]
            y_max = boundaries[i + 1] if i + 1 < len(boundaries) else 999999999

            row_cells = []
            for col_idx, col_def in enumerate(columns):
                # Find shapes in this column within this row's y-range
                cell_texts = []
                for s in column_shapes[col_idx]:
                    if y_min <= s.top < y_max:
                        cell_texts.append(s.text.strip())

                # Handle multivalue: one shape may contain multiple rows
                expanded = []
                for t in cell_texts:
                    if multivalue_split and multivalue_split in t:
                        expanded.extend(t.split(multivalue_split))
                    else:
                        expanded.append(t)

                cell_value = " ".join(expanded).strip() if expanded else ""
                cell_value = self._parse_value(cell_value, col_def.get("data_type", "text"))
                row_cells.append(cell_value)

            # Skip empty rows
            if any(c.strip() for c in row_cells):
                # Handle multivalue expansion (one shape = multiple rows)
                max_values = 1
                expanded_cols: list[list[str]] = []
                for col_idx, col_def in enumerate(columns):
                    values = []
                    for s in column_shapes[col_idx]:
                        if y_min <= s.top < y_max:
                            text = s.text.strip()
                            if multivalue_split and multivalue_split in text:
                                values.extend(text.split(multivalue_split))
                            elif text:
                                values.append(text)
                    max_values = max(max_values, len(values))
                    expanded_cols.append(values)

                if max_values > 1:
                    # Expand into multiple rows
                    for v_idx in range(max_values):
                        expanded_row = []
                        for col_idx, col_def in enumerate(columns):
                            vals = expanded_cols[col_idx]
                            val = vals[v_idx] if v_idx < len(vals) else vals[0] if vals else ""
                            val = self._parse_value(val.strip(), col_def.get("data_type", "text"))
                            expanded_row.append(val)
                        if any(c.strip() for c in expanded_row):
                            rows.append(expanded_row)
                else:
                    rows.append(row_cells)

        return rows

    def _detect_rows_by_position(self, column_shapes: list[list[ShapeData]],
                                  columns: list[dict],
                                  multivalue_split: str) -> list[list[str]]:
        """Detect rows by grouping shapes by y-position proximity."""
        # Collect all y-positions
        all_tops = set()
        for shapes in column_shapes:
            for s in shapes:
                all_tops.add(s.top)

        if not all_tops:
            return []

        # Cluster y-positions (within 50000 EMU = ~1.3mm tolerance)
        sorted_tops = sorted(all_tops)
        clusters = []
        current_cluster = [sorted_tops[0]]
        for y in sorted_tops[1:]:
            if y - current_cluster[-1] < 50000:
                current_cluster.append(y)
            else:
                clusters.append(current_cluster)
                current_cluster = [y]
        clusters.append(current_cluster)

        rows = []
        for cluster in clusters:
            y_min = min(cluster) - 25000
            y_max = max(cluster) + 25000

            row_cells = []
            for col_idx, col_def in enumerate(columns):
                cell_texts = []
                for s in column_shapes[col_idx]:
                    if y_min <= s.top <= y_max:
                        text = s.text.strip()
                        if multivalue_split and multivalue_split in text:
                            cell_texts.extend(text.split(multivalue_split))
                        elif text:
                            cell_texts.append(text)
                cell_value = cell_texts[0] if len(cell_texts) == 1 else " ".join(cell_texts)
                cell_value = self._parse_value(cell_value.strip(), col_def.get("data_type", "text"))
                row_cells.append(cell_value)

            if any(c.strip() for c in row_cells):
                rows.append(row_cells)

        return rows

    def _extract_text_elements(self, shapes: list[ShapeData],
                                extraction_config: dict) -> dict[str, str]:
        """Extract non-table text elements (title, subtitle, annotation)."""
        elements = {}
        rules = extraction_config.get("rules", [])

        for rule in rules:
            role = rule.get("role", "")
            if not role:
                continue

            for s in shapes:
                matched = False

                if "shape_name" in rule:
                    if s.name == rule["shape_name"]:
                        matched = True
                elif "name_pattern" in rule:
                    if fnmatch.fnmatch(s.name, rule["name_pattern"]):
                        matched = True
                elif "shape_type" in rule:
                    if rule["shape_type"] in s.shape_type:
                        matched = True

                if "region_top_emu_min" in rule and s.top < rule["region_top_emu_min"]:
                    matched = False

                if matched and s.text.strip():
                    elements[role] = s.text.strip()
                    break

        return elements

    def _parse_value(self, value: str, data_type: str) -> str:
        """Parse a cell value according to column data type and parsing rules."""
        if not value:
            return ""

        if data_type == "number":
            # Check WIP markers
            if value.upper() in [m.upper() for m in self.wip_markers]:
                return value  # Keep as-is

            # Strip unit patterns
            cleaned = value
            for pattern in self.strip_patterns:
                cleaned = cleaned.replace(pattern, "")
            cleaned = cleaned.strip()

            # Handle European decimal separator (comma -> dot)
            if "," in cleaned and "." not in cleaned:
                cleaned = cleaned.replace(",", ".")

            # Try to parse as number
            try:
                float(cleaned)
                return cleaned
            except ValueError:
                return value  # Return original if can't parse

        return value


# ---------------------------------------------------------------------------
# Template matcher
# ---------------------------------------------------------------------------

def match_templates(slide: Any, templates: list[dict[str, Any]]) -> list[tuple[dict, float]]:
    """Try all available metadata templates against a slide and return ranked matches.

    Args:
        slide: python-pptx Slide object
        templates: List of slide_metadata template dicts

    Returns:
        List of (template, confidence) tuples, sorted by confidence descending
    """
    matches = []
    for tmpl in templates:
        slides_def = tmpl.get("slides", [])
        for slide_def in slides_def:
            extractor = SpatialTableExtractor(tmpl)
            score = extractor.try_match_slide(slide, slide_def)
            if score > 0.2:
                matches.append((tmpl, score))

    matches.sort(key=lambda x: x[1], reverse=True)
    return matches
