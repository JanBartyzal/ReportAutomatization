# Task: Supplemental Excel Ingestion (FEAT-04)

**Spec:** `docs/specs/feature_04_excel_ingestion.md`
**Standards:** `docs/project_standards.md`
**DoD:** `docs/dod_criteria.md`

## Overview
Implement ingestion of Excel (`.xlsx`) files to support supplemental data execution. These should be converted to the standard JSON structure and indexed in RAG like PPTX tables.

## Checklist

### 1. Analysis & Setup
- [x] Review `docs/specs/feature_04_excel_ingestion.md`.
- [x] Ensure `pandas` and `openpyxl` are installed.

### 2. Implementation
- [x] **Parser Module**
    - [x] Create `backend/app/services/parsers/excel.py`.
    - [x] Implement `ExcelProcessor` class.
    - [x] Logic:
        - [x] Iterate all sheets.
        - [x] Detect headers (first non-empty row).
        - [x] Convert to standard Table JSON schema.
        - [x] Handle merged cells and formulas (use calculated values).
- [x] **API Updates**
    - [x] Update `POST /api/upload`:
        - [x] Allow `.xlsx` / `.xls` MIME types.
        - [x] Route to `ExcelProcessor` if extension matches.
- [x] **RAG Integration**
    - [x] Ensure Excel data follows the `process-and-vectorize` pipeline.
    - [x] Add metadata `type: "excel_supplement"` to chunks.

### 3. Verification & Testing
- [x] **Unit Tests**
    - [x] Create `tests/backend/test_excel_parser.py`.
    - [x] Test simple sheet (headers + rows).
    - [x] Test multi-sheet workbook.
    - [x] Test merged cells/formulas (verify values are correct).
- [x] **Integration Test**
    - [x] Upload Excel file -> Check Database -> Check RAG Index.
    - [x] Ask question via Chat API that requires Excel data.

### 4. Documentation
- [x] Update supported file types in `README.md`.
- [x] Update `docs/project_status.md`.
