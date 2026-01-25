"""
Pseudo-Table Detection Module

This module provides functionality to detect and extract tabular data from
PowerPoint text boxes that are visually arranged as tables but are not
native PowerPoint Table objects.

The parser uses positional heuristics (bounding box analysis) to identify
grid-like arrangements of text boxes and convert them into structured JSON.
"""

from typing import List, Optional, TypedDict
import statistics


class Shape(TypedDict):
    """Type definition for shape geometry data.

    Attributes:
        text: Text content of the shape.
        top: Y-coordinate of the top edge (in points/pixels).
        left: X-coordinate of the left edge (in points/pixels).
        width: Width of the shape (in points/pixels).
        height: Height of the shape (in points/pixels).
    """

    text: str
    top: float
    left: float
    width: float
    height: float


class PseudoTable(TypedDict):
    """Type definition for parsed pseudo-table output.

    Attributes:
        type: Identifier for the table type (always "pseudo_table").
        confidence_score: Alignment quality score (0.0-1.0, higher is better).
        bbox: Bounding box of the entire table [left, top, right, bottom].
        data: 2D array of cell contents (rows × columns), None for empty cells.
    """

    type: str
    confidence_score: float
    bbox: List[float]
    data: List[List[Optional[str]]]


class PseudoTableParser:
    """
    Parser for detecting pseudo-tables from PPTX text box arrays.

    A pseudo-table is a grid-like arrangement of text boxes that visually
    appears as a table but is not a native PowerPoint Table object.

    The parser uses heuristics to identify:
    - Row alignment (shapes with similar Y-coordinates)
    - Column alignment (shapes with similar X-coordinates)
    - Grid coherence (consistent row/column structure)

    Example:
        >>> parser = PseudoTableParser(x_tolerance=10, y_tolerance=10)
        >>> shapes = [
        ...     {"text": "H1", "top": 100, "left": 50, "width": 100, "height": 20},
        ...     {"text": "H2", "top": 100, "left": 160, "width": 100, "height": 20},
        ... ]
        >>> result = parser.parse(shapes)
        >>> if result:
        ...     print(result["confidence_score"])

    Attributes:
        x_tolerance: Maximum horizontal deviation (pixels) for column alignment.
        y_tolerance: Maximum vertical deviation (pixels) for row alignment.
        min_rows: Minimum number of rows required to be considered a table.
        min_cols: Minimum number of columns required to be considered a table.
    """

    def __init__(
        self,
        x_tolerance: int = 10,
        y_tolerance: int = 10,
        min_rows: int = 3,
        min_cols: int = 2,
    ) -> None:
        """
        Initialize the PseudoTableParser.

        Args:
            x_tolerance: Maximum horizontal deviation (pixels) for column alignment.
                Default is 10 pixels.
            y_tolerance: Maximum vertical deviation (pixels) for row alignment.
                Default is 10 pixels.
            min_rows: Minimum number of rows required. Default is 3.
            min_cols: Minimum number of columns required. Default is 2.
        """
        self.x_tolerance = x_tolerance
        self.y_tolerance = y_tolerance
        self.min_rows = min_rows
        self.min_cols = min_cols

    def parse(self, shapes: List[Shape]) -> Optional[PseudoTable]:
        """
        Parse a list of shapes and extract pseudo-table if present.

        This is the main public API for the parser.

        Args:
            shapes: List of shape dictionaries with geometry and text data.

        Returns:
            PseudoTable dictionary if a valid table is detected, None otherwise.

        Raises:
            None: Gracefully handles all edge cases and returns None on failure.
        """
        # Step 1: Filter text-containing shapes
        text_shapes = self._filter_text_shapes(shapes)

        # Early exit: insufficient shapes
        if len(text_shapes) < (self.min_rows * self.min_cols):
            return None

        # Step 2: Sort shapes by position (top, then left)
        sorted_shapes = self._sort_shapes(text_shapes)

        # Step 3: Cluster shapes into candidate rows
        rows = self._cluster_rows(sorted_shapes)

        # Early exit: insufficient rows
        if len(rows) < self.min_rows:
            return None

        # Step 4: Detect column boundaries
        columns = self._detect_columns(rows)

        # Early exit: insufficient columns
        if len(columns) < self.min_cols:
            return None

        # Step 5: Validate grid coherence
        if not self._validate_grid(rows, columns):
            return None

        # Step 6: Calculate confidence score
        confidence = self._calculate_confidence_score(rows, columns)

        # Step 7: Extract grid data
        grid_data = self._extract_grid_data(rows, columns)

        # Step 8: Calculate bounding box
        bbox = self._calculate_bbox(sorted_shapes)

        return PseudoTable(
            type="pseudo_table", confidence_score=confidence, bbox=bbox, data=grid_data
        )

    def _filter_text_shapes(self, shapes: List[Shape]) -> List[Shape]:
        """
        Filter shapes that contain non-empty text.

        Args:
            shapes: List of all shapes.

        Returns:
            List of shapes with non-empty text content.
        """
        return [shape for shape in shapes if shape.get("text", "").strip()]

    def _sort_shapes(self, shapes: List[Shape]) -> List[Shape]:
        """
        Sort shapes by vertical position (top) then horizontal (left).

        Args:
            shapes: List of shapes to sort.

        Returns:
            Sorted list of shapes (top-to-bottom, left-to-right).
        """
        return sorted(shapes, key=lambda s: (s["top"], s["left"]))

    def _cluster_rows(self, shapes: List[Shape]) -> List[List[Shape]]:
        """
        Group shapes into rows based on vertical alignment.

        Shapes are considered in the same row if their top coordinates
        differ by less than y_tolerance from the first shape in that row.

        Args:
            shapes: Sorted list of shapes.

        Returns:
            List of rows, where each row is a list of shapes.
        """
        if not shapes:
            return []

        rows: List[List[Shape]] = []
        current_row: List[Shape] = [shapes[0]]
        current_y = shapes[0]["top"]

        for shape in shapes[1:]:
            if abs(shape["top"] - current_y) <= self.y_tolerance:
                # Same row
                current_row.append(shape)
            else:
                # New row
                rows.append(current_row)
                current_row = [shape]
                current_y = shape["top"]

        # Don't forget the last row
        if current_row:
            rows.append(current_row)

        # Sort each row by left coordinate
        for row in rows:
            row.sort(key=lambda s: s["left"])

        return rows

    def _detect_columns(self, rows: List[List[Shape]]) -> List[float]:
        """
        Detect common column boundaries across all rows.

        Uses clustering of left coordinates to identify column positions.

        Args:
            rows: List of rows (each row is a list of shapes).

        Returns:
            List of column left-edge positions (sorted).
        """
        if not rows:
            return []

        # Collect all left coordinates
        all_left_coords: List[float] = []
        for row in rows:
            for shape in row:
                all_left_coords.append(shape["left"])

        if not all_left_coords:
            return []

        # Cluster left coordinates using tolerance
        all_left_coords.sort()
        columns: List[float] = [all_left_coords[0]]

        for coord in all_left_coords[1:]:
            if abs(coord - columns[-1]) > self.x_tolerance:
                # New column
                columns.append(coord)

        return columns

    def _validate_grid(self, rows: List[List[Shape]], columns: List[float]) -> bool:
        """
        Validate that rows and columns form a coherent grid.

        A grid is considered coherent if:
        - Most rows have similar number of shapes (within ±1)
        - At least 70% of expected cells are populated

        Args:
            rows: List of rows.
            columns: List of column positions.

        Returns:
            True if grid is coherent, False otherwise.
        """
        if not rows or not columns:
            return False

        # Check row length consistency
        row_lengths = [len(row) for row in rows]
        median_length = statistics.median(row_lengths)

        # Allow ±1 variation from median for irregular grids
        consistent_rows = sum(
            1 for length in row_lengths if abs(length - median_length) <= 1
        )

        consistency_ratio = consistent_rows / len(rows)

        # Require at least 70% of rows to be consistent
        if consistency_ratio < 0.7:
            return False

        # Check cell density (at least 70% of grid populated)
        total_shapes = sum(len(row) for row in rows)
        expected_cells = len(rows) * len(columns)
        density = total_shapes / expected_cells if expected_cells > 0 else 0

        return density >= 0.7

    def _calculate_confidence_score(
        self, rows: List[List[Shape]], columns: List[float]
    ) -> float:
        """
        Calculate confidence score based on alignment quality.

        Score components:
        - Row alignment quality (Y-axis deviation)
        - Column alignment quality (X-axis deviation)
        - Grid completeness (% of cells populated)

        Args:
            rows: List of rows.
            columns: List of column positions.

        Returns:
            Confidence score between 0.0 and 1.0.
        """
        if not rows or not columns:
            return 0.0

        # Component 1: Row alignment quality
        row_deviations: List[float] = []
        for row in rows:
            if len(row) > 1:
                row_tops = [shape["top"] for shape in row]
                deviation = max(row_tops) - min(row_tops)
                row_deviations.append(deviation)

        avg_row_deviation = statistics.mean(row_deviations) if row_deviations else 0.0
        row_score = max(0.0, 1.0 - (avg_row_deviation / (self.y_tolerance * 2)))

        # Component 2: Column alignment quality
        col_deviations: List[float] = []
        for col_idx, col_left in enumerate(columns):
            # Get all shapes in this column across all rows
            col_shapes: List[Shape] = []
            for row in rows:
                for shape in row:
                    if abs(shape["left"] - col_left) <= self.x_tolerance:
                        col_shapes.append(shape)

            if len(col_shapes) > 1:
                col_lefts = [shape["left"] for shape in col_shapes]
                deviation = max(col_lefts) - min(col_lefts)
                col_deviations.append(deviation)

        avg_col_deviation = statistics.mean(col_deviations) if col_deviations else 0.0
        col_score = max(0.0, 1.0 - (avg_col_deviation / (self.x_tolerance * 2)))

        # Component 3: Grid completeness
        total_shapes = sum(len(row) for row in rows)
        expected_cells = len(rows) * len(columns)
        completeness_score = (
            total_shapes / expected_cells if expected_cells > 0 else 0.0
        )

        # Weighted average: alignment 60%, completeness 40%
        confidence = (row_score * 0.3) + (col_score * 0.3) + (completeness_score * 0.4)

        return round(confidence, 2)

    def _extract_grid_data(
        self, rows: List[List[Shape]], columns: List[float]
    ) -> List[List[Optional[str]]]:
        """
        Extract 2D grid of cell contents.

        Creates a matrix where each cell contains the text from the corresponding
        shape, or None if the cell is empty.

        Args:
            rows: List of rows.
            columns: List of column positions.

        Returns:
            2D list of cell contents (None for empty cells).
        """
        grid: List[List[Optional[str]]] = []

        for row in rows:
            grid_row: List[Optional[str]] = [None] * len(columns)

            for shape in row:
                # Find which column this shape belongs to
                min_distance = float("inf")
                col_idx = 0

                for idx, col_left in enumerate(columns):
                    distance = abs(shape["left"] - col_left)
                    if distance < min_distance:
                        min_distance = distance
                        col_idx = idx

                # Place text in the appropriate column
                if min_distance <= self.x_tolerance:
                    grid_row[col_idx] = shape["text"].strip()

            grid.append(grid_row)

        return grid

    def _calculate_bbox(self, shapes: List[Shape]) -> List[float]:
        """
        Calculate bounding box of all shapes.

        Args:
            shapes: List of shapes.

        Returns:
            Bounding box as [left, top, right, bottom].
        """
        if not shapes:
            return [0.0, 0.0, 0.0, 0.0]

        min_left = min(shape["left"] for shape in shapes)
        min_top = min(shape["top"] for shape in shapes)
        max_right = max(shape["left"] + shape["width"] for shape in shapes)
        max_bottom = max(shape["top"] + shape["height"] for shape in shapes)

        return [min_left, min_top, max_right, max_bottom]
