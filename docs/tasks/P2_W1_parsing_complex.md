# P2 – Wave 1: Complex Atomizers & Read Model (Opus)

**Phase:** P2 – Extended Parsing & Visualization
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~24 MD
**Depends on:** P1 (core pipeline working)

> Atomizers with non-trivial parsing logic, AI integration, and the CQRS query layer.

---

## P2-W1-001: MS-ATM-XLS – Excel Atomizer

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/processor/microservices/units/ms-atm-xls

**Tasks:**
- [ ] FastAPI + gRPC using `packages/python-base`
- [ ] Implement `ExcelAtomizerService` from `atomizer.v1.excel` proto:
  - `ExtractStructure` – list sheets, row/col counts, merged cell detection
  - `ExtractSheetContent` – per-sheet conversion to JSON (openpyxl)
  - `ExtractAll` – all sheets with partial success handling
- [ ] **Partial Success**:
  - 9/10 sheets succeed → `PARTIAL` status
  - Failed sheet returns error detail, successful sheets stored normally
- [ ] **Data Type Detection**:
  - Detect column types: STRING, NUMBER, DATE, CURRENCY, PERCENTAGE
  - Handle locale-specific formats (Czech number format: `1 234,56`)
- [ ] **Edge Cases**:
  - Merged cells → unmerge and fill
  - Hidden sheets → include with metadata flag
  - Formula cells → extract computed values, not formulas
  - Empty rows/columns → skip with configurable threshold
- [ ] Blob Storage integration (download/upload)
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Unit tests with sample Excel files (various formats)

**AC:**
- [ ] 10-sheet file, 1 sheet fails → 9 stored, status PARTIAL
- [ ] Czech number format correctly detected
- [ ] JSONB records indistinguishable from PPTX table records in DB

---

## P2-W1-002: MS-QRY – Query API (CQRS Read Model)

**Type:** Core Service
**Effort:** 6 MD
**Service:** apps/engine/microservices/units/ms-qry

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **REST endpoints** (frontend-facing via API Gateway):
  - `GET /api/query/files/{file_id}/data` – all parsed data for a file
  - `GET /api/query/files/{file_id}/slides` – slide content with image URLs
  - `GET /api/query/tables` – query OPEX table data (filterable, paginated)
  - `GET /api/query/documents/{document_id}` – single document
  - `GET /api/query/processing-logs/{file_id}` – processing step timeline
- [ ] **Materialized Views**:
  - `mv_file_summary` – file + processing status + latest workflow step
  - `mv_org_tables` – cross-file table data for org dashboards
- [ ] **Redis Caching**:
  - Cache query results with TTL 5 min
  - Cache invalidation on new data write (Dapr Pub/Sub subscriber)
- [ ] **RLS Enforcement**: All queries scoped to user's `org_id`
- [ ] Pagination: cursor-based for large datasets
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] File data query returns tables + documents + slide images
- [ ] Response < 500ms for typical queries (with cache hit)
- [ ] RLS prevents cross-tenant data access
- [ ] Cache auto-invalidates when new data arrives

---

## P2-W1-003: MS-DASH – Dashboard Aggregation

**Type:** Core Service
**Effort:** 10 MD
**Service:** apps/engine/microservices/units/ms-dash

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **REST endpoints** (frontend-facing):
  - `GET/POST /api/dashboards` – CRUD dashboard configurations
  - `GET /api/dashboards/{id}` – single dashboard config
  - `PUT /api/dashboards/{id}` – update config
  - `DELETE /api/dashboards/{id}` – delete dashboard
  - `POST /api/dashboards/{id}/data` – execute dashboard query
  - `POST /api/dashboards/period-comparison` – cross-period comparison
- [ ] **Dashboard Configuration Model**:
  - JSON-stored config: data source, GROUP BY, ORDER BY, filters
  - Chart type selection (bar, line, pie, heatmap)
  - Date range filter, org filter
  - `is_public` flag (Viewer can see public only)
- [ ] **SQL over JSONB**:
  - Dynamic SQL generation from UI config
  - PostgreSQL JSON functions: `->`, `->>`, `jsonb_array_elements`
  - Parameterized queries (SQL injection prevention)
- [ ] **Aggregation Engine**:
  - GROUP BY arbitrary JSONB keys
  - SUM, AVG, COUNT, MIN, MAX aggregations
  - Multi-level grouping (org → period → cost center)
- [ ] **Source Type Transparency**: `source_type: FILE / FORM` flag on all data
- [ ] Flyway migration: `dashboards` table
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Dashboard config saved and retrievable
- [ ] Dynamic SQL correctly aggregates JSONB data
- [ ] Period comparison returns delta values (absolute + percentage)
- [ ] Dashboard loads < 3s for 50+ companies
