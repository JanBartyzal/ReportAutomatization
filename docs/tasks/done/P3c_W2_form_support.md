# P3c – Wave 2: Supporting Services (Sonnet)

**Phase:** P3c – Form Builder & Data Collection
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~8 MD

---

## P3c-W2-001: MS-TMPL Extension – Excel-to-Form Mapping

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-tmpl (extension)

**Tasks:**
- [x] Implement `MapExcelToForm` from proto
- [x] Mapping logic:
  - Match Excel column headers to form field names
  - Fuzzy matching with confidence score
  - Historical mappings (same org previously imported)
  - AI suggestion via MS-ATM-AI for low-confidence matches
- [x] Response includes: mapped pairs, unmapped Excel columns, unmapped form fields
- [ ] Integration test with sample Excel and form definitions

---

## P3c-W2-002: MS-ORCH Extension – Form Import Workflow

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-orch (extension)

**Tasks:**
- [x] New workflow: `FORM_IMPORT`
  - Triggered when `upload_purpose: FORM_IMPORT`
  - Steps: Parse Excel (MS-ATM-XLS) → Suggest Mapping (MS-TMPL) → Return to FE for confirmation
  - After user confirms: Store in form_responses (MS-SINK-TBL)
- [x] JSON workflow definition for FORM_IMPORT
- [ ] Error handling: malformed Excel, version mismatch

---

## P3c-W2-003: MS-QRY Extension – Form Data Queries

**Type:** Service Extension
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-qry (extension)

**Tasks:**
- [x] New endpoints:
  - `GET /api/query/forms/{form_id}/data` – aggregated form response data
  - `GET /api/query/forms/{form_id}/responses/{org_id}` – specific org response
- [ ] Form data available alongside file data in dashboards
- [ ] `source_type: FORM` flag on form-sourced data
