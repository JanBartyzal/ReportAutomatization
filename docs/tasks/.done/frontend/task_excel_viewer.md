# Task: Excel Viewer / Data Import (FEAT-05)

**Status:** TODO
**Spec:** `docs/specs/frontend/feature_01_ExcelViewer.md`
**DoD:** `docs/dod_criteria.md`
**Standards:** `docs/project_standards.md`, `docs/project_defaults.md`

## Context
Implement the ability for users to upload Excel files (.xlsx/.xls), parse them into a standardized JSON structure, and attach them as an appendix to an existing PowerPoint Plan. This allows financial analysts to provide supporting calculations alongside their reports.

## Phase 1: Backend Implementation (FastAPI + Python)

- [ ] **Dependency Management & Setup**
    - [ ] Verify `pandas` and `openpyxl` are installed/available in `requirements.txt` or strictly managed.
    - [ ] Create a new module/service `services/excel_service.py` (or similar) for handling Excel processing.

- [ ] **Data Modeling**
    - [ ] Define Pydantic models for the Appendix structure in `schemas/plan.py` or `schemas/excel.py`.
        - Use the JSON structure defined in Section 5 of Feature Spec.
        - Ensure validation for `sheet_name`, `tables`, `rows`.

- [ ] **Excel Processing Logic (Service Layer)**
    - [ ] Implement function to read `.xlsx` file from bytes/file-like object.
    - [ ] Iterate through sheets.
    - [ ] Detect used range as table (Row 1 = Headers).
    - [ ] Handle data sanitization (NaN -> null, Infinity -> null, Dates -> ISO 8601 string).
    - [ ] Return structured data matching the Pydantic model.

- [ ] **API Endpoint Implementation**
    - [ ] Create/Modify endpoint: `POST /api/import/upload/opex/excel`.
        - **Input:** Multipart/form-data (file), optional `plan_id` (if context known) or logic to associate later. *Note: Spec mentions association might happen via a selector, but typically upload might need a target plan ID or return a temporary ID. Spec Section 5.9 says `POST /api/import/append-excel/{plan_id}`. Let's start with that.*
    - [ ] Endpoint logic:
        - Validate file extension/mime-type (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`).
        - Call Excel Processing Service.
        - Fetch Plan by `plan_id`.
        - Update Plan's `appendix` field with new data.
        - Save to DB.
    - [ ] Return 200 OK with summary (processed sheets count, etc.).

- [ ] **Backend Testing**
    - [ ] Unit tests for Excel parsing (mock different Excel file structures, empty sheets, invalid data).
    - [ ] Integration test for the API endpoint (mock DB).

## Phase 2: Frontend Implementation (React + TypeScript)

- [ ] **API Client Update**
    - [ ] implementation of `uploadExcelAppendix(planId: string, file: File)` in the frontend API layer.

- [ ] **UI Components**
    - [ ] Create/Update `ExcelUploadComponent.tsx`.
    - [ ] Implement Drag & Drop zone (accept `.xlsx`, `.xls`).
    - [ ] Add file validation (Max 20MB, extension check).
    - [ ] Add Progress Bar state.
    - [ ] Add Plan Association Selector (if `planId` is not passed from parent context). *If specific UI design isn't provided, use a simple dropdown of available plans.*

- [ ] **Integration**
    - [ ] Integrate into Route `/import/upload/opex/excel` (or modal).
    - [ ] Connect Upload Success action -> Trigger data refresh/Plan detail view update.
    - [ ] Display success/error toasts.

- [ ] **Frontend Testing**
    - [ ] Component tests for Upload interactions.
    - [ ] Mock API calls.

## Phase 3: Documentation & Verification

- [ ] **Documentation**
    - [ ] Update Swagger/OpenAPI docs (auto-generated, generic check).
    - [ ] Add "How to use" note in `docs/user_guide.md` (if exists) or Module README.

- [ ] **Verification (Manual)**
    - [ ] Upload a valid Excel file -> Verify JSON in DB.
    - [ ] Upload an invalid file -> Verify Error handling.
    - [ ] Check UI feedback.
