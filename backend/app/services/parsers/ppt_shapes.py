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
        x_tolerance: float = 200000.0,  # ~0.22 inches in EMU (PowerPoint units)
        y_tolerance: float = 200000.0,  # ~0.22 inches in EMU
        min_rows: int = 3,
        min_cols: int = 2,
    ) -> None:
        """
        Initialize the PseudoTableParser.
        
        PowerPoint coordinates use EMU (English Metric Units) where:
        - 914400 EMU = 1 inch
        - Default tolerances of 200000 EMU ≈ 0.22 inches

        Args:
            x_tolerance: Max horizontal deviation for column alignment (EMU).
            y_tolerance: Max vertical deviation for row alignment (EMU).
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
        
        Tries two detection strategies:
        1. Row-based layout (shapes arranged horizontally in rows)
        2. Column-based layout (shapes arranged vertically in columns)

        Args:
            shapes: List of shape dictionaries with geometry and text.

        Returns:
            List of dictionaries matching PseudoTable model.
        """
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

        # Try row-based detection first (original logic)
        row_based_result = self._try_row_based_detection(validated_shapes)
        if row_based_result:
            logger.info("Detected row-based pseudo-table")
            return row_based_result
        
        # Fall back to column-based detection
        column_based_result = self._try_column_based_detection(validated_shapes)
        if column_based_result:
            logger.info("Detected column-based pseudo-table")
            return column_based_result
        
        return []
    
    def _try_row_based_detection(self, validated_shapes: List[ShapeGeometry]) -> List[Dict[str, Any]]:
        """
        Try to detect table using row-based layout (original logic).
        
        Args:
            validated_shapes: List of validated shape geometries.
            
        Returns:
            List with single PseudoTable dict if detected, empty list otherwise.
        """
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
        return [result.model_dump()]
    
    def _try_column_based_detection(self, validated_shapes: List[ShapeGeometry]) -> List[Dict[str, Any]]:
        """
        Try to detect table using column-based layout.
        
        In this layout, each column is a vertical set of text boxes.
        
        Args:
            validated_shapes: List of validated shape geometries.
            
        Returns:
            List with single PseudoTable dict if detected, empty list otherwise.
        """
        # Sort: left-to-right, then top-to-bottom
        sorted_shapes = sorted(validated_shapes, key=lambda s: (s.left, s.top))
        
        # Cluster into columns
        columns = self._cluster_columns(sorted_shapes)
        if len(columns) < self.min_cols:
            return []
        
        # Detect rows within columns (get Y-coordinates)
        rows = self._detect_rows_in_columns(columns)
        if len(rows) < self.min_rows:
            return []
        
        # Validate column grid
        if not self._validate_column_grid(columns, rows):
            return []
        
        # Transpose columns to rows for final grid
        grid_data = self._transpose_columns_to_rows(columns, rows)
        
        # Calculate confidence
        confidence = self._calculate_column_confidence_score(columns, rows)
        bbox = self._calculate_bbox(sorted_shapes)
        
        result = PseudoTable(
            confidence_score=confidence,
            bbox=bbox,
            data=grid_data
        )
        return [result.model_dump()]

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
    
    def _cluster_columns(self, shapes: List[ShapeGeometry]) -> List[List[ShapeGeometry]]:
        """
        Group shapes into columns based on X-coordinate.
        
        Args:
            shapes: List of shapes sorted by (left, top).
            
        Returns:
            List of columns, where each column is a list of shapes.
        """
        if not shapes:
            return []
        
        columns: List[List[ShapeGeometry]] = []
        current_column: List[ShapeGeometry] = [shapes[0]]
        current_x = shapes[0].left
        
        for shape in shapes[1:]:
            if abs(shape.left - current_x) <= self.x_tolerance:
                current_column.append(shape)
            else:
                columns.append(current_column)
                current_column = [shape]
                current_x = shape.left
        
        if current_column:
            columns.append(current_column)
        
        # Sort shapes within each column by top position
        for column in columns:
            column.sort(key=lambda s: s.top)
        
        return columns
    
    def _detect_rows_in_columns(self, columns: List[List[ShapeGeometry]]) -> List[float]:
        """
        Detect row top boundaries from columns.
        
        Args:
            columns: List of columns (each column is a list of shapes).
            
        Returns:
            List of Y-coordinates representing row positions.
        """
        all_tops = sorted([s.top for col in columns for s in col])
        if not all_tops:
            return []
        
        rows = [all_tops[0]]
        for top in all_tops[1:]:
            if abs(top - rows[-1]) > self.y_tolerance:
                rows.append(top)
        return rows
    
    def _validate_column_grid(self, columns: List[List[ShapeGeometry]], rows: List[float]) -> bool:
        """
        Heuristic check if columns truly form a grid.
        
        Args:
            columns: List of columns.
            rows: List of row Y-coordinates.
            
        Returns:
            True if valid grid structure, False otherwise.
        """
        column_lengths = [len(col) for col in columns]
        median_len = statistics.median(column_lengths)
        
        # Check if most columns have similar number of cells
        consistent_cols = sum(1 for l in column_lengths if abs(l - median_len) <= 1)
        if (consistent_cols / len(columns)) < 0.7:
            return False
        
        # Check density
        total_shapes = sum(column_lengths)
        expected_cells = len(columns) * len(rows)
        density = total_shapes / expected_cells if expected_cells > 0 else 0
        
        return density >= 0.7
    
    def _transpose_columns_to_rows(self, columns: List[List[ShapeGeometry]], rows: List[float]) -> List[List[Optional[str]]]:
        """
        Convert column-major layout to row-major grid.
        
        Args:
            columns: List of columns (each column is a list of shapes).
            rows: List of row Y-coordinates.
            
        Returns:
            2D grid matrix (row-major).
        """
        grid = []
        for row_y in rows:
            grid_row = [None] * len(columns)
            for col_idx, column in enumerate(columns):
                # Find shape in this column that matches this row
                best_shape = None
                min_dist = float('inf')
                for shape in column:
                    dist = abs(shape.top - row_y)
                    if dist < min_dist:
                        min_dist = dist
                        best_shape = shape
                
                if best_shape and min_dist <= self.y_tolerance:
                    grid_row[col_idx] = best_shape.text.strip()
            grid.append(grid_row)
        return grid
    
    def _calculate_column_confidence_score(self, columns: List[List[ShapeGeometry]], rows: List[float]) -> float:
        """
        Calculate alignment quality score for column-based layout.
        
        Args:
            columns: List of columns.
            rows: List of row Y-coordinates.
            
        Returns:
            Confidence score between 0.0 and 1.0.
        """
        # Column jitter (horizontal alignment)
        col_devs = []
        for column in columns:
            if len(column) > 1:
                lefts = [s.left for s in column]
                col_devs.append(max(lefts) - min(lefts))
        col_score = 1.0 - (statistics.mean(col_devs) / (self.x_tolerance * 2)) if col_devs else 1.0
        
        # Row jitter (vertical alignment across columns)
        row_devs = []
        for row_y in rows:
            row_shapes = [s for col in columns for s in col if abs(s.top - row_y) <= self.y_tolerance]
            if len(row_shapes) > 1:
                tops = [s.top for s in row_shapes]
                row_devs.append(max(tops) - min(tops))
        row_score = 1.0 - (statistics.mean(row_devs) / (self.y_tolerance * 2)) if row_devs else 1.0
        
        # Density
        total = sum(len(col) for col in columns)
        expected = len(columns) * len(rows)
        density = total / expected
        
        score = (col_score * 0.3) + (row_score * 0.3) + (density * 0.4)
        return round(max(0.0, min(1.0, score)), 2)
