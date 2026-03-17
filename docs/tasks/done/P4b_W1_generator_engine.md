# P4b – Wave 1: Generator Engine & Template Manager (Opus)

**Phase:** P4b – PPTX Report Generation
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~31 MD
**Depends on:** P3b (lifecycle – APPROVED reports), P3c (form data)

> Complex rendering engine with placeholder substitution, chart generation, and batch processing.

---

## P4b-W1-001: processor-generators:pptx – PPTX Generator

**Type:** Core Service
**Effort:** 16 MD
**Service:** apps/processor/microservices/units/ms-gen-pptx

**Tasks:**
- [ ] FastAPI + gRPC using `packages/python-base`
- [ ] Implement `PptxGeneratorService` from `generator.v1.pptx_generator` proto:
  - `GenerateReport` – single report generation
  - `BatchGenerate` – multiple reports
- [ ] **Template Loading**:
  - Download PPTX template from Blob Storage
  - Parse placeholder tags: `{{variable_name}}`, `{{TABLE:name}}`, `{{CHART:name}}`
- [ ] **Text Placeholder Substitution** (python-pptx):
  - Find and replace text placeholders in all text frames
  - Preserve original formatting (font, size, color)
  - Missing data → red border + `DATA MISSING` text
- [ ] **Table Generation**:
  - `{{TABLE:table_name}}` → populate table shape with data rows
  - Auto-resize rows for data volume
  - Style: match template table style
- [ ] **Chart Generation** (matplotlib/plotly → pptx):
  - `{{CHART:metric_name}}` → generate chart as image, insert into slide
  - Chart types: bar, line, pie (configurable per placeholder)
  - Style: match template color scheme
  - Data source: aggregated from approved form responses / uploaded data
- [ ] **Missing Data Handling**:
  - Slide with missing placeholder → visual warning (red frame, "DATA MISSING")
  - Report generation does NOT fail on missing data
  - Missing placeholders listed in response
- [ ] **Output**:
  - Generated PPTX saved to Blob Storage
  - URL stored on report entity
  - Response includes `BlobReference` to generated file
- [ ] **Batch Generation**:
  - Accept list of report_ids
  - Process sequentially (parallel optional via async workers)
  - Return per-report results (success/failure)
  - 10 reports < 15 minutes target
- [ ] **Async Processing**:
  - Generation runs asynchronously
  - Completion notification via Dapr Pub/Sub → engine-reporting:notification
  - WebSocket/SSE push to frontend
- [ ] Dockerfile with python-pptx, matplotlib, LibreOffice (for validation)
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] 20-slide PPTX generated in < 60 seconds
- [ ] Output file valid in MS PowerPoint and LibreOffice
- [ ] Missing data → "DATA MISSING" marker, not failure
- [ ] Batch 10 reports < 15 minutes
- [ ] Generated file downloadable from UI

---

## P4b-W1-002: engine-reporting:pptx-template – PPTX Template Manager

**Type:** Core Service
**Effort:** 9 MD
**Service:** apps/engine/microservices/units/ms-tmpl-pptx

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **REST Endpoints** (frontend-facing):
  - `POST /api/templates/pptx` – upload PPTX as template
  - `GET /api/templates/pptx` – list templates
  - `GET /api/templates/pptx/{id}` – template detail with placeholder list
  - `PUT /api/templates/pptx/{id}` – update template (new version)
  - `DELETE /api/templates/pptx/{id}` – deactivate template
  - `GET /api/templates/pptx/{id}/preview` – preview without data
  - `GET /api/templates/pptx/{id}/placeholders` – extracted placeholder list
  - `POST /api/templates/pptx/{id}/mapping` – configure placeholder → data source mapping
  - `GET /api/templates/pptx/{id}/mapping` – get current mapping config
- [ ] **Template Upload & Parsing**:
  - Upload PPTX file to Blob Storage
  - Parse PPTX: extract all `{{...}}` placeholders from text, tables, charts
  - Return placeholder list with type detection (TEXT, TABLE, CHART)
- [ ] **Template Versioning**:
  - v1, v2, v3... for same template
  - Assignment to `period_id` or `report_type`
  - Old versions preserved, not deleted
- [ ] **Placeholder Mapping Configuration**:
  - Map `{{it_costs}}` → field `amount_czk` from form / column from Excel
  - Mapping stored as template config (not per-report)
  - Support both form field sources and uploaded file data sources
- [ ] **Template Scope**: `CENTRAL` (HoldingAdmin only) – `LOCAL` prepared for FS21
- [ ] Flyway migrations: `pptx_templates`, `template_versions`, `template_placeholders`, `placeholder_mappings` tables
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/templates/pptx/*` → engine-reporting:pptx-template

**AC:**
- [ ] Upload PPTX → placeholders auto-extracted and listed
- [ ] Mapping config saved and retrievable
- [ ] Template versioning preserves old versions
- [ ] Preview renders template with placeholder names visible

---

## P4b-W1-003: engine-orchestrator Extension – Generation Workflow

**Type:** Service Extension
**Effort:** 6 MD
**Service:** apps/engine/microservices/units/ms-orch (extension)

**Tasks:**
- [ ] New workflow: `PPTX_GENERATION`
  - Triggered by: manual request or APPROVED event
  - Steps:
    1. Load template config from engine-reporting:pptx-template (Dapr gRPC)
    2. Load report data from engine-data:query (Dapr gRPC)
    3. Apply placeholder mapping
    4. Call processor-generators:pptx.GenerateReport (Dapr gRPC)
    5. Store generated file URL on report entity
    6. Notify user (Dapr Pub/Sub → engine-reporting:notification)
- [ ] **Batch Generation Workflow**:
  - HoldingAdmin triggers batch for all APPROVED reports in period
  - Orchestrate sequential generation with progress tracking
  - Report individual successes/failures
- [ ] **Error Handling**:
  - Generation failure → retry 2x, then DLQ
  - Partial batch: continue with remaining reports if one fails
- [ ] JSON workflow definitions for generation flows
- [ ] Integration test: APPROVED report → generated PPTX downloadable
