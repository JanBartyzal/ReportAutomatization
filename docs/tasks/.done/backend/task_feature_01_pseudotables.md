# Task: Implement Pseudo-Table Detection (FEAT-01)

**Spec:** `docs/specs/feature_01_presudotables.md`
**Standards:** `docs/project_standards.md`
**DoD:** `docs/dod_criteria.md`

## Overview
Implement logic to detect visual grids of text boxes in PowerPoint slides and convert them into structured JSON tables. This addresses the issue where tables constructed from separate shapes are missed by the parser.

## Checklist

### 1. Analysis & Setup
- [X] Review `docs/specs/feature_01_presudotables.md` for algorithm details.
- [X] Ensure `thefuzz` or similar required libraries are installed (though this feature mainly relies on geometry, check if standard libs suffice).

### 2. Implementation
- [X] **Create/Update Parser Module**
    - [X] Create/Update `backend/app/services/parsers/ppt_shapes.py` or similar location.
    - [X] Implement `identify_pseudo_tables(shapes: List[Shape]) -> List[PseudoTable]`.
    - [X] Implement Heuristics:
        - [X] Filter text-containing shapes.
        - [X] Sort by Top/Left.
        - [X] Row Clustering (`y_tolerance` ~10px).
        - [X] Column Detection (`x_tolerance` ~10px).
        - [X] Grid Validation (Min 2 cols, 3 rows, grouping check).
- [X] **Data Extraction**
    - [X] Convert valid grids into List of Lists structure.
    - [X] Handle empty cells (null properties).
    - [X] Handle merged cells (if basic geometry allows implementation, otherwise note limitation).
- [X] **Output Formatting**
    - [X] Ensure output matches the JSON schema:
      ```json
      {
        "type": "pseudo_table",
        "confidence_score": 0.92,
        "bbox": [100, 200, 500, 400],
        "data": [...]
      }
      ```

### 3. Verification & Testing
- [X] **Unit Tests**
    - [X] Create `tests/backend/test_pseudo_tables.py`.
    - [X] Test case: Perfect grid.
    - [X] Test case: Grid with gaps (missing cells).
    - [X] Test case: Alignment jitter (shapes slightly off-axis).
    - [X] Test case: Not a table (random text boxes).
- [X] **Manual Check**
    - [X] Run parser against a sample PPTX with a "fake" table.
    - [X] Verify JSON output structure.

### 4. Documentation
- [X] Add docstrings to new functions (Google style).
- [X] Update `backend/README.md` (or module specific README) to mention Pseudo-Table capabilities.
