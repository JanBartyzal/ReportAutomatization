# P3c – Wave 1: Form Engine Core (Opus)

**Phase:** P3c – Form Builder & Data Collection
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~30 MD
**Depends on:** P1 (sinks), P3a (schema mapping), P3b (lifecycle, periods)

> Complex form definition engine with versioning, validation rules, and Excel import/export.

---

## P3c-W1-001: MS-FORM – Form Builder & Data Collection Service

**Type:** Core Service
**Effort:** 24 MD
**Service:** apps/engine/microservices/units/ms-form

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Form Definition Model**:
  - JSON Schema-based form definitions stored in PostgreSQL
  - Field types: `text`, `number` (with currency/unit), `percentage`, `date`, `dropdown`, `table`, `file_attachment`
  - Field properties: `required`, `min`, `max`, `regex`, `dependent_on`
  - Sections with descriptive text for structure
  - Scope: `CENTRAL` (holding) or `LOCAL` (company) – data model ready for FS21
  - `owner_org_id` for future local forms
- [ ] **REST Endpoints** (frontend-facing via API Gateway):
  - **Builder (Admin)**:
    - `GET/POST /api/forms` – list/create forms
    - `GET/PUT/DELETE /api/forms/{id}` – CRUD
    - `POST /api/forms/{id}/publish` – DRAFT → PUBLISHED
    - `POST /api/forms/{id}/close` – → CLOSED
    - `GET /api/forms/{id}/versions` – list versions
    - `GET /api/forms/{id}/preview` – render preview
  - **Responses (Editor)**:
    - `GET /api/forms/{id}/responses` – list responses for form
    - `POST /api/forms/{id}/responses` – create new response
    - `GET/PUT /api/forms/{id}/responses/{resp_id}` – get/update response
    - `PUT /api/forms/{id}/responses/{resp_id}/auto-save` – partial save
  - **Excel Integration**:
    - `GET /api/forms/{id}/export/excel-template` – download Excel template
    - `POST /api/forms/{id}/import/excel` – import filled Excel
  - **Assignments**:
    - `GET/POST /api/forms/{id}/assignments` – assign form to orgs
- [ ] **Form Versioning**:
  - Editing published form creates new version (v1 → v2)
  - Existing responses bound to their version
  - Historical data never overwritten by form updates
- [ ] **Validation Engine**:
  - Per-field validation: min/max, regex, required
  - Cross-field dependencies: `if field_A > 0 then field_B required`
  - Validation returns all errors at once (not one by one)
  - Validate on auto-save (warnings) and on submit (blocking)
- [ ] **Auto-Save**:
  - `PUT /auto-save` endpoint called every 30s or on section change
  - Stores partial response in DB with `status: DRAFT`
  - Survives connection loss (data preserved)
- [ ] **Field-Level Comments**:
  - Editor can add comments to any field value
  - Comments stored alongside field data
  - Visible to Reviewer during approval
- [ ] **Form State Management**:
  - `DRAFT` → `PUBLISHED` → `CLOSED`
  - PUBLISHED: visible to assigned users
  - CLOSED: no new submissions (deadline or manual)
  - Deadline integration: auto-close from MS-PERIOD
- [ ] **Form Assignment**:
  - Assign form to specific orgs (not all fill same form)
  - Per-org assignment status tracking
- [ ] **Submission Integration**: After submit → create/update report in MS-LIFECYCLE (DRAFT → SUBMITTED)
- [ ] Flyway migrations: `forms`, `form_versions`, `form_fields`, `form_responses`, `form_field_values`, `form_field_comments`, `form_assignments` tables
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/forms/*` → MS-FORM

**AC:**
- [ ] HoldingAdmin creates and publishes form in < 10 minutes
- [ ] Auto-save works; data preserved after connection loss
- [ ] Validation returns all errors at once
- [ ] Form v1 and v2 data stored separately; historical data intact
- [ ] Import from Excel: mapping auto-suggested, Editor confirms in < 2 minutes

---

## P3c-W1-002: MS-FORM – Excel Template Export

**Type:** Service Feature
**Effort:** 4 MD
**Service:** apps/engine/microservices/units/ms-form (extension)

**Tasks:**
- [ ] **Excel Template Generation** (Apache POI):
  - One sheet per form section
  - Columns = form fields with headers
  - Data validation rules in Excel (dropdowns, number ranges)
  - Hidden metadata sheet `__form_meta` with `form_id` and `form_version_id`
  - Cell formatting hints (currency, percentage, date)
- [ ] **Excel Import Back**:
  - Read metadata sheet → verify `form_version_id`
  - Version match: direct mapping (no user confirmation needed)
  - Version mismatch: warn user, offer best-effort mapping via MS-TMPL
  - Parse values per column → map to form fields
  - Return pre-populated form in UI for visual review before submit
- [ ] **Arbitrary Excel Import** (no template):
  - Upload any Excel file
  - Parse via MS-ATM-XLS (Dapr gRPC → MS-ORCH)
  - Offer column → form field mapping via MS-TMPL
  - Editor reviews and confirms mapping
  - Import into form as editable data
  - Original Excel stored as audit attachment

**AC:**
- [ ] Exported Excel template opens in MS Excel and LibreOffice
- [ ] Template re-import with matching version: zero mapping effort
- [ ] Arbitrary Excel import: mapping suggested automatically

---

## P3c-W1-003: MS-SINK-TBL Extension – Form Response Storage

**Type:** Service Extension
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-sink-tbl (extension)

**Tasks:**
- [ ] Extend `form_responses` table with:
  - Versioned storage per form_version_id
  - Field-level audit trail (old_value, new_value, changed_by, changed_at)
  - Index on `(org_id, period_id, form_version_id)` for fast queries
- [ ] Flyway migration for schema updates
- [ ] MS-QRY extension: form response data accessible via existing query API
- [ ] RLS policy on form_responses
