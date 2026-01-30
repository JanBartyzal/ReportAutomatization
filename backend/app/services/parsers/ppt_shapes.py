"""
Pseudo-Table Detection Module for PowerPoint Shapes.

This module provides functionality to detect and extract tabular data from
PowerPoint text boxes that are visually arranged as tables but are not
native PowerPoint Table objects.
"""

import statistics
import logging
from typing import List, Optional, Dict, Any
from pydantic import BaseModel

logger = logging.getLogger(__name__)

class ShapeGeometry(BaseModel):
    """Geometry data for a single shape."""
    text: str
    top: float
    left: float
    width: float
    height: float

class PseudoTable(BaseModel):
    """Structure for detected pseudo-table data."""
    type: str = "pseudo_table"
    confidence_score: float
    bbox: List[float]  # [left, top, right, bottom]
    data: List[List[Optional[str]]]

class PseudoTableParser:
    """
    Parser for detecting pseudo-tables from PPTX text box arrays.

    Uses positional heuristics to identify grid-like arrangements of text boxes.
    """

    def __init__(
        self,
        x_tolerance: float = 10.0,
        y_tolerance: float = 10.0,
        min_rows: int = 3,
        min_cols: int = 2,
    ) -> None:
        """
        Initialize the PseudoTableParser.

        Args:
            x_tolerance: Max horizontal deviation for column alignment.
            y_tolerance: Max vertical deviation for row alignment.
            min_rows: Minimum rows required for a table.
            min_cols: Minimum columns required for a table.
        """
        self.x_tolerance = x_tolerance
        self.y_tolerance = y_tolerance
        self.min_rows = min_rows
        self.min_cols = min_cols

    def parse(self, shapes: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Parse slide shapes and extract pseudo-tables if present.

        Args:
            shapes: List of shape dictionaries with geometry and text.

        Returns:
            List of dictionaries matching PseudoTable model.
        """
        results = []
        # Convert to models for easier handling
        validated_shapes = []
        for s in shapes:
            try:
                if s.get("text", "").strip():
                    validated_shapes.append(ShapeGeometry(**s))
            except (ValueError, TypeError):
                continue

        if len(validated_shapes) < (self.min_rows * self.min_cols):
            return []

        # Sort: top-to-bottom, then left-to-right
        sorted_shapes = sorted(validated_shapes, key=lambda s: (s.top, s.left))

        # Cluster into rows
        rows = self._cluster_rows(sorted_shapes)
        if len(rows) < self.min_rows:
            return []

        # Detect columns
        columns = self._detect_columns(rows)
        if len(columns) < self.min_cols:
            return []

        # Validate grid
        if not self._validate_grid(rows, columns):
            return []

        # Extraction and Confidence
        confidence = self._calculate_confidence_score(rows, columns)
        grid_data = self._extract_grid_data(rows, columns)
        bbox = self._calculate_bbox(sorted_shapes)

        result = PseudoTable(
            confidence_score=confidence,
            bbox=bbox,
            data=grid_data
        )
        results.append(result.model_dump())

        return results

    def _cluster_rows(self, shapes: List[ShapeGeometry]) -> List[List[ShapeGeometry]]:
        """Group shapes into rows based on Y-coordinate."""
        if not shapes:
            return []

        rows: List[List[ShapeGeometry]] = []
        current_row: List[ShapeGeometry] = [shapes[0]]
        current_y = shapes[0].top

        for shape in shapes[1:]:
            if abs(shape.top - current_y) <= self.y_tolerance:
                current_row.append(shape)
            else:
                rows.append(current_row)
                current_row = [shape]
                current_y = shape.top
        
        if current_row:
            rows.append(current_row)

        for row in rows:
            row.sort(key=lambda s: s.left)
        
        return rows

    def _detect_columns(self, rows: List[List[ShapeGeometry]]) -> List[float]:
        """Detect column left boundaries."""
        all_lefts = sorted([s.left for row in rows for s in row])
        if not all_lefts:
            return []

        columns = [all_lefts[0]]
        for left in all_lefts[1:]:
            if abs(left - columns[-1]) > self.x_tolerance:
                columns.append(left)
        return columns

    def _validate_grid(self, rows: List[List[ShapeGeometry]], columns: List[float]) -> bool:
        """Heuristic check if the shapes truly form a grid."""
        row_lengths = [len(row) for row in rows]
        median_len = statistics.median(row_lengths)
        
        consistent_rows = sum(1 for l in row_lengths if abs(l - median_len) <= 1)
        if (consistent_rows / len(rows)) < 0.7:
            return False

        total_shapes = sum(row_lengths)
        expected_cells = len(rows) * len(columns)
        density = total_shapes / expected_cells if expected_cells > 0 else 0
        
        return density >= 0.7

    def _calculate_confidence_score(self, rows: List[List[ShapeGeometry]], columns: List[float]) -> float:
        """Calculate alignment quality score."""
        # Row jitter
        row_devs = []
        for row in rows:
            if len(row) > 1:
                tops = [s.top for s in row]
                row_devs.append(max(tops) - min(tops))
        row_score = 1.0 - (statistics.mean(row_devs) / (self.y_tolerance * 2)) if row_devs else 1.0
        
        # Column jitter
        col_devs = []
        for col_left in columns:
            col_shapes = [s for row in rows for s in row if abs(s.left - col_left) <= self.x_tolerance]
            if len(col_shapes) > 1:
                lefts = [s.left for s in col_shapes]
                col_devs.append(max(lefts) - min(lefts))
        col_score = 1.0 - (statistics.mean(col_devs) / (self.x_tolerance * 2)) if col_devs else 1.0

        # Density
        total = sum(len(r) for r in rows)
        expected = len(rows) * len(columns)
        density = total / expected

        score = (row_score * 0.3) + (col_score * 0.3) + (density * 0.4)
        return round(max(0.0, min(1.0, score)), 2)

    def _extract_grid_data(self, rows: List[List[ShapeGeometry]], columns: List[float]) -> List[List[Optional[str]]]:
        """Convert rows into 2D grid matrix."""
        grid = []
        for row in rows:
            grid_row = [None] * len(columns)
            for shape in row:
                best_col = 0
                min_dist = float('inf')
                for i, col_left in enumerate(columns):
                    dist = abs(shape.left - col_left)
                    if dist < min_dist:
                        min_dist = dist
                        best_col = i
                
                if min_dist <= self.x_tolerance:
                    grid_row[best_col] = shape.text.strip()
            grid.append(grid_row)
        return grid

    def _calculate_bbox(self, shapes: List[ShapeGeometry]) -> List[float]:
        """Calculate overall bounding box."""
        left = min(s.left for s in shapes)
        top = min(s.top for s in shapes)
        right = max(s.left + s.width for s in shapes)
        bottom = max(s.top + s.height for s in shapes)
        return [left, top, right, bottom]
