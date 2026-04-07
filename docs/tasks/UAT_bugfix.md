# UAT Bug Fix Tasks — ReportAutomatization

**Source:** UAT Report 2026-04-07 (90.8% pass rate — 10 failures, 12 skips)
**Priority:** Fix before next UAT run
**Total failures:** 10 FAIL + 12 SKIP (of which 8 are cascaded from root cause #1)

---

## Summary of Findings

| Priority | ID | Area | Impact |
|----------|----|------|--------|
| CRITICAL | UAT-01 | File Upload — POST /api/upload returns 500 | Cascades to 8 downstream failures |
| HIGH | UAT-02 | Batch RLS — admin2 sees admin1's batch | Security violation |
| HIGH | UAT-03 | Dashboards — listing / viewer access / widget data | 4 failures |
| MEDIUM | UAT-04 | Form Builder — Excel template export returns 500 | 1 failure |
| MEDIUM | UAT-05 | Notifications SSE stream — connection timeout | 2 failures across steps 09 + 13 |
| MEDIUM | UAT-06 | Schema Mapping — POST /api/query/templates/mappings returns 403 | 1 failure |
| MEDIUM | UAT-07 | PPTX template — POST upload-pptx-template returns 400 | 1 failure |
| MEDIUM | UAT-08 | Period — POST clone-period returns 500 | 1 failure |
| LOW | UAT-09 | Form Builder — Excel import not implemented (skipped) | missing feature |
| LOW | UAT-10 | Smart Persistence — routing after promotion (skipped) | missing feature |

---

## UAT-01 — CRITICAL: File Upload 500 Error

**Steps affected:** step02 (4 failures), step03/04/05 (FATAL — no file_id), step06/10/14 (SKIP — no file_id)
**Endpoint:** `POST /api/upload`
**Symptom:** Both PPTX and XLSX upload return HTTP 500 Internal Server Error
**Error body:** `{"status": 500, "error": "Internal Server Error", "path": "/api/upload"}`

**Investigation checklist:**
- [ ] Check engine-ingestor logs for the actual exception stack trace
- [ ] Verify Blob Storage connection (MinIO/Azure) — check credentials and reachability
- [ ] Verify ClamAV service is running and reachable from engine-ingestor
- [ ] Check `upload_purpose` discrimination logic (PARSE vs FORM_IMPORT)
- [ ] Verify Dapr sidecar is healthy on engine-ingestor container
- [ ] Check multipart parsing configuration (max file size, temp dir)
- [ ] Confirm Orchestrator topic publish does not fail synchronously

**Fix scope:** engine-ingestor service

**AC:**
- [ ] `POST /api/upload` with valid PPTX returns HTTP 201 with `{ "fileId": "..." }`
- [ ] `POST /api/upload` with valid XLSX returns HTTP 201 with `{ "fileId": "..." }`
- [ ] step02 passes all 8 assertions
- [ ] step03, step04, step05 unblock (no more FATAL)

---

## UAT-02 — HIGH: Batch RLS — Cross-Tenant Visibility

**Step affected:** step08 (1 failure)
**Symptom:** `admin2 does NOT see admin1's batch 'Q1/2026'` — assertion fails, meaning admin2 CAN see admin1's batch
**Security impact:** Row-Level Security not isolating batches between organizations

**Investigation checklist:**
- [ ] Verify RLS policy exists on `batches` table in PostgreSQL
- [ ] Check that `org_id` column is present and populated on batch records
- [ ] Verify `SET app.current_org_id = ?` is called before queries in engine-core
- [ ] Confirm the DB connection pool is not reusing sessions without resetting RLS context
- [ ] Test: `SET app.current_org_id = '<admin2_org>'; SELECT * FROM batches;` — should not return admin1's batch

**Fix scope:** engine-core (batch module), PostgreSQL RLS migration

**AC:**
- [ ] admin2 cannot list batches belonging to admin1's organization
- [ ] RLS policy verified with direct SQL test
- [ ] step08 passes all 12 assertions

---

## UAT-03 — HIGH: Dashboards — Listing, Viewer Access, Widget Data

**Step affected:** step11 (4 failures, 82% pass rate)
**Failures:**
1. `Created dashboard {uuid} found in list` — dashboard not appearing after creation
2. `Viewer can see public dashboard` — viewer role blocked despite `is_public=true`
3. `Dashboard data contains widget results` — no widget data returned
4. `Dashboard data includes 5 project rows` — query returns wrong/empty result

**Investigation checklist:**
- [ ] **Listing bug:** Check if dashboard creation returns 201 but list endpoint uses different filter (`org_id`? `owner_id`?)
- [ ] **Public flag:** Verify `is_public` column exists and is checked in authorization middleware for Viewer role
- [ ] **Widget results:** Check widget execution pipeline — SQL runner, data binding to dashboard response
- [ ] **Row count:** Verify seed data — 5 project rows must exist; check if `source_type` (FORM/FILE) filter is applied correctly
- [ ] Confirm `engine-data:dashboard` service routing is correct in API Gateway

**Fix scope:** engine-data (dashboard module), PostgreSQL (RLS for public dashboards)

**AC:**
- [ ] Created dashboard appears in list response
- [ ] Viewer can read dashboards with `is_public = true`
- [ ] Widget results populated in dashboard data response
- [ ] Dashboard query returns correct number of rows
- [ ] step11 passes all 22 assertions

---

## UAT-04 — MEDIUM: Form Builder — Excel Template Export 500

**Step affected:** step19 (1 failure)
**Endpoint:** `GET /api/forms/{form_id}/export/excel-template`
**Symptom:** Returns HTTP 500, content-type `application/problem+json`

**Investigation checklist:**
- [ ] Check engine-reporting (form module) logs for exception during Excel template generation
- [ ] Verify Apache POI / OpenPyXL dependency is available in the container
- [ ] Check if form definition (field types, validation rules) is fully loaded before generation
- [ ] Verify temp file write permissions in container

**Fix scope:** engine-reporting (form module) or processor-generators (xls)

**AC:**
- [ ] `GET /api/forms/{id}/export/excel-template` returns HTTP 200 with valid `.xlsx` binary
- [ ] Excel file contains metadata sheet and column headers matching form fields

---

## UAT-05 — MEDIUM: Notifications SSE Stream Timeout

**Steps affected:** step09 (port 8105), step13 (port 8105)
**Endpoints:**
- `GET /api/notifications/stream` (step09)
- `GET /api/v1/notifications/stream` (step13)
**Symptom:** `HTTPConnectionPool(host='localhost', port=8105): Read timed out`

**Investigation checklist:**
- [ ] Verify engine-reporting:notification service is running and healthy on port 8105 (or routed via gateway)
- [ ] Check if SSE `Content-Type: text/event-stream` is set correctly (no buffering middleware)
- [ ] Verify the test timeout is appropriate for SSE (should use streaming read, not standard HTTP timeout)
- [ ] Check if Traefik/nginx is buffering the SSE response (disable proxy buffering)
- [ ] Confirm the `/api/notifications/stream` route is registered in API Gateway config
- [ ] Check if CORS preflight blocks the SSE endpoint

**Fix scope:** engine-reporting (notification module), infra/docker (gateway config)

**AC:**
- [ ] SSE stream connects and sends initial `ping` or `connected` event within 2s
- [ ] step09 and step13 notification stream assertions pass

---

## UAT-06 — MEDIUM: Schema Mapping — POST Returns 403

**Step affected:** step15 (1 failure)
**Endpoint:** `POST /api/query/templates/mappings`
**Symptom:** Returns HTTP 403 (expected 201) — authorization denied

**Investigation checklist:**
- [ ] Verify the endpoint exists and is registered (not 404 disguised as 403)
- [ ] Check `@PreAuthorize` annotation — which roles are allowed? Admin only? Editor?
- [ ] Verify the test user's role — is the correct JWT role claim being sent?
- [ ] Check if the endpoint was moved during P8 consolidation and route is stale

**Fix scope:** engine-data (template/query module), authorization config

**AC:**
- [ ] Admin/Editor role can `POST /api/query/templates/mappings` and receive 201
- [ ] step15 passes all 9 assertions

---

## UAT-07 — MEDIUM: PPTX Template Upload Returns 400

**Step affected:** step18 (1 failure)
**Endpoint:** `POST /api/templates/pptx/generate/batch` (or upload endpoint)
**Symptom:** Returns HTTP 400 Bad Request (expected 201)

**Investigation checklist:**
- [ ] Check request body format — is the multipart or JSON body correct?
- [ ] Verify required fields in the upload request (template_name, version, etc.)
- [ ] Check if the endpoint URL has changed since the test was written
- [ ] Review validation constraints (file size, MIME type check for `.pptx`)

**Fix scope:** engine-reporting (pptx-template module)

**AC:**
- [ ] PPTX template upload returns 201 with `template_id`
- [ ] step18 passes all 4 assertions (including batch generation)

---

## UAT-08 — MEDIUM: Period Clone Returns 500

**Step affected:** step20 (1 failure)
**Endpoint:** `POST /api/periods/{id}/clone` (or equivalent clone endpoint)
**Symptom:** Returns HTTP 500 Internal Server Error

**Investigation checklist:**
- [ ] Check engine-reporting (period module) logs for exception during clone
- [ ] Verify clone logic — does it copy all sub-entities (deadlines, assignments)?
- [ ] Check for foreign key constraint violations during clone (duplicate keys?)
- [ ] Verify UUID generation for cloned period

**Fix scope:** engine-reporting (period module)

**AC:**
- [ ] `POST clone-period` returns 201 with new period data
- [ ] Cloned period has new UUID, same configuration as source

---

## UAT-09 — LOW: Form Builder — Excel Import Not Implemented

**Step affected:** step19 (1 skip)
**Endpoint:** `POST /api/forms/{id}/import/excel`
**Status:** Explicitly marked as missing feature in UAT

**Task:** Implement Excel import endpoint for Form Builder
- Parse uploaded Excel file using metadata sheet
- Map columns to form field definitions (reuse schema mapping logic)
- Validate data types per field definition
- Return import summary (rows imported, errors)

**AC:**
- [ ] `POST /api/forms/{id}/import/excel` returns 200 with import summary
- [ ] Invalid rows reported with row number and error description

---

## UAT-10 — LOW: Smart Persistence — Routing After Promotion

**Step affected:** step24 (1 skip)
**Endpoint:** `GET /api/admin/promotions/candidates/{id}`
**Status:** Skipped — verify routing update after promotion not implemented

**Task:** Implement post-promotion routing verification
- After admin approves a promotion candidate, query routing should transparently redirect to new SQL table
- Endpoint should confirm routing has been updated

**AC:**
- [ ] After promotion approval, data queries route to promoted SQL table
- [ ] `GET /api/admin/promotions/candidates/{id}` returns routing status

---

## Cascaded Skips — Resolve by Fixing UAT-01

The following 8 skipped assertions will automatically pass once file upload is fixed (UAT-01):

| Step | Skipped assertion |
|------|------------------|
| step06 | XLSX row-count check (no xlsx file_id) |
| step06 | XLSX content check (no xlsx file_id) |
| step10 | No XLSX file uploaded |
| step10 | `GET /api/files/<id>` — no xlsx_file_id |
| step10 | `GET /api/query/files/<id>/data` × 2 |
| step14 | `GET /api/versions/file/{file_id}` — no file_id |
| step03 | All assertions (FATAL: no PPTX file_id) |
| step04 | All assertions (FATAL: no PPTX file_id) |
| step05 | All assertions (FATAL: no PPTX file_id) |

---

## Execution Order

Fix in this order to maximize UAT coverage per fix:

1. **UAT-01** — File Upload 500 (unblocks 8 downstream tests)
2. **UAT-02** — Batch RLS (security, must not ship broken)
3. **UAT-03** — Dashboards (4 failures, core feature)
4. **UAT-05** — SSE Notifications (2 failures across 2 steps)
5. **UAT-06** — Schema Mapping 403
6. **UAT-04** — Form Builder Excel Export
7. **UAT-07** — PPTX Template Upload
8. **UAT-08** — Period Clone
9. **UAT-09** — Excel Import (missing feature)
10. **UAT-10** — Smart Persistence routing (missing feature)

---

## Target: UAT Pass Rate ≥ 98%

| After fixing | Expected pass rate |
|-------------|-------------------|
| Current | 90.8% (217/239) |
| After UAT-01 | ~95%+ (unblocks ~20 tests) |
| After UAT-01 to UAT-08 | ~98%+ |
| After UAT-09, UAT-10 | ~99%+ |
