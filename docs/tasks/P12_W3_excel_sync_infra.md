# P12 – Wave 3: Live Excel Export & External Sync – Boilerplate & Fixtures (Haiku/Gemini)

**Phase:** P12 – Live Excel Export & External Sync
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~3 MD
**Depends on:** P12-W1 (core logic), P12-W2 (configs & migrations)
**Feature Set:** FS27

> Boilerplate, test fixtures, documentation, and environment setup for FS27.

---

## P12-W3-001: Test Fixtures & Seed Data

**Type:** Testing
**Effort:** 1 MD

**Tasks:**
- [ ] **Excel test fixtures** (`tests/UAT/data/`):
  - `export_test_workbook.xlsx` – multi-sheet workbook:
    - Sheet "Data": 5 columns, 20 rows of sample OPEX data
    - Sheet "Summary": manual formulas referencing "Data" sheet (SUM, AVERAGE)
    - Sheet "Charts": bar chart and pie chart sourced from "Data"
  - `export_template_empty.xlsx` – workbook with "Summary" and "Charts" sheets only (no "Data" sheet)
  - `export_large_dataset.xlsx` – single sheet with 5,000 rows for performance validation
- [ ] **WireMock stubs for Microsoft Graph API** (`tests/fixtures/sharepoint/`):
  - `graph_token_response.json` – OAuth2 token response
  - `graph_drive_list.json` – drive listing response
  - `graph_file_metadata.json` – file item metadata
  - `graph_upload_success.json` – upload confirmation
  - `graph_rate_limited.json` – 429 response with Retry-After header
  - `graph_not_found.json` – 404 file not found response
- [ ] **SQL seed data** for integration tests:
  - Insert sample `export_flow_definitions` (2 records: LOCAL_PATH + SHAREPOINT type)
  - Insert sample `export_flow_executions` (3 records: SUCCESS, FAILED, RUNNING)
  - SQL query template referencing UAT seed data tables
- [ ] **Postman collection update**:
  - Add Export Flow CRUD endpoints (all 8 REST endpoints)
  - Add example request/response bodies
  - Add environment variables for export flow IDs

**AC:**
- [ ] Test Excel files open correctly in Excel/LibreOffice
- [ ] Multi-sheet workbook contains formulas and charts that reference data sheet
- [ ] WireMock stubs cover success, error, and rate-limit scenarios
- [ ] Seed data insertable without constraint violations

---

## P12-W3-002: Documentation & READMEs

**Type:** Documentation
**Effort:** 1 MD

**Tasks:**
- [ ] **Module README** (`apps/engine/engine-integrations/excel-sync/README.md`):
  - Module overview and purpose
  - Architecture diagram (Mermaid): event flow from orchestrator → excel-sync → processor-generators → target
  - Configuration reference (all properties with defaults)
  - API endpoint reference (table format)
  - SharePoint setup guide (Azure AD app registration steps)
  - Local path setup guide (Docker volume mount)
- [ ] **processor-generators README update** (`apps/processor/processor-generators/README.md`):
  - Add `UpdateSheet` gRPC endpoint documentation
  - Input/output format description
  - Sheet preservation guarantees
- [ ] **TOPICS.md update**:
  - Add `data-imported` topic with schema, publisher (engine-orchestrator), subscriber (engine-integrations:excel-sync)
- [ ] **OpenAPI spec** comments for all REST endpoints (Swagger annotations)

**AC:**
- [ ] README contains working Mermaid diagram
- [ ] Configuration reference lists all properties
- [ ] SharePoint setup guide is followable by new developer
- [ ] TOPICS.md includes new topic

---

## P12-W3-003: Environment Files & Dockerfiles

**Type:** Configuration
**Effort:** 1 MD

**Tasks:**
- [ ] **Update `.env.example`** (`infra/docker/.env.example`):
  ```env
  # FS27 – Live Excel Export & External Sync
  EXCEL_SYNC_ENABLED=true
  EXCEL_SYNC_THREAD_POOL_SIZE=4
  EXCEL_SYNC_ALLOWED_PATHS=/mnt/exports
  EXCEL_EXPORT_HOST_PATH=./data/exports
  SHAREPOINT_TENANT_ID=
  SHAREPOINT_CLIENT_ID=
  SHAREPOINT_SECRET_KEYVAULT_REF=
  ```
- [ ] **Update frontend `.env.example`** (`apps/frontend/.env.example`):
  ```env
  VITE_EXPORT_FLOWS_ENABLED=true
  ```
- [ ] **Verify Dockerfile** for engine-integrations includes excel-sync module:
  - Check `apps/engine/engine-integrations/Dockerfile` copies excel-sync sources
  - Verify build includes excel-sync dependencies in `pom.xml`
- [ ] **Create export data directory**:
  - `data/exports/.gitkeep` – ensure directory exists in repo
  - Add `data/exports/*.xlsx` to `.gitignore`
- [ ] **Tilt configuration** update (`tilt/`) for local dev:
  - Add volume mount for export directory
  - Hot-reload for excel-sync module

**AC:**
- [ ] `.env.example` documents all FS27 variables
- [ ] `data/exports/` directory exists and is gitignored for Excel files
- [ ] Docker build includes excel-sync module without errors
