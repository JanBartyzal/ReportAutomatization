# Feature Specification: Supplemental Excel Ingestion

**Feature ID:** FEAT-04
**Parent Epic:** Data Ingestion
**Prerequisites:** Batch Processing (FEAT-03)

---

## 1. Problem Statement
In addition to PPTX presentations, users often have raw data in Excel (`.xlsx`) files that support the presentation. We need to ingest these files, convert them to the SAME JSON structure as PPTX tables, and include them in the same Batch and RAG index.

## 2. Technical Requirements

### Parser Module (`backend/app/services/parsers/excel.py`)
- Use `pandas` or `openpyxl`.
- **Sheet Iteration:** Process ALL sheets in the workbook.
- **Header Detection:** Assume the first non-empty row is the header.
- **Output:** Convert to the standardized JSON schema used by `TableExtractor`.

### API Updates
- Update `POST /api/upload` to accept `.xlsx` and `.xls` MIME types.
- Add logic: `if file.endswith('.pptx') -> use SlideProcessor` ELSE `if file.endswith('.xlsx') -> use ExcelProcessor`.

### RAG Integration
- Excel data must be vectorized immediately using the existing `process-and-vectorize` pipeline.
- Metadata must distinguish source type (`type: "excel_supplement"` vs `type: "pptx_table"`).

## 3. Edge Cases
- **Merged Cells:** `pandas` usually handles this, but verify behavior (fill forward or null).
- **Formulas:** We need the *calculated value*, not the formula string (default behavior of `openpyxl` data_only=True).
- **Empty Sheets:** Skip them.

## 4. DoD Specifics
- **Validation:** Upload a complex Excel file (merged headers, multiple sheets) and verify JSON output.
- **Search:** Verify that RAG can answer questions based on the Excel data.