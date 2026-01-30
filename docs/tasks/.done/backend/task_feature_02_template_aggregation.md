# Task: Implement Template Aggregation Engine (FEAT-02)

**Spec:** `docs/specs/feature_02_template_aggregation.md`
**Standards:** `docs/project_standards.md`
**DoD:** `docs/dod_criteria.md`

## Overview
Implement a service to identify identical table structures across multiple files (Schema Fingerprinting) and aggregate their data into a master dataset.

## Checklist

### 1. Analysis & Setup
- [x] Review `docs/specs/feature_02_template_aggregation.md`.
- [x] Add `thefuzz` (or `python-Levenshtein`) to `backend/requirements.txt` if not present.

### 2. Implementation
- [x] **Aggregation Service**
    - [x] Create `backend/app/services/aggregation.py`.
    - [x] Implement `generate_schema_hash(headers: List[str]) -> str`.
        - [x] Normalize headers (lowercase, remove special chars).
        - [x] Create hash based on columns + count + types.
    - [x] Implement Fuzzy Matching Helper.
        - [x] Match similar headers (Threshold > 90%).
- [x] **Aggregation Logic**
    - [x] Implement "Virtual Merge" logic (UNION ALL concept).
    - [x] Ensure `source_file`, `slide_number`, `country/region` are preserved.
    - [x] Handle missing columns (fill with null).
    - [x] Handle type conflicts (cast to text or warn).
- [x] **API Endpoints**
    - [x] Update/Create `backend/app/routers/analytics.py`.
    - [x] `POST /api/analytics/aggregate/preview` (Input inputs IDs, Output detected schema).
    - [x] `GET /api/analytics/aggregate/{schema_fingerprint}` (Return merged data).

### 3. Verification & Testing
- [x] **Unit Tests**
    - [x] Create `tests/backend/test_aggregation.py`.
    - [x] Test Schema Fingerprinting (same headers different order/case).
    - [x] Test Fuzzy Matching (headers with slight typo).
    - [x] Test Data Aggregation (merging 2 simple datasets).
- [x] **Integration Test**
    - [x] Run against two "Monthly Report" files with same structure but different data.
    - [x] Verify API returns combined list.

### 4. Documentation
- [x] Update `docs/api/openapi_summary.md` (if exists) or Swagger.
- [x] Update project status in `docs/project_status.md`.
