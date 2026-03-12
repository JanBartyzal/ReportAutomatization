# P7 – Wave 1: Service-Now Integration & Smart Persistence (Opus)

**Phase:** P7 – External Integrations & Data Optimization
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~28 MD
**Depends on:** P4a (notifications, audit), P3a (schema mapping, admin), P4b (PPTX generation)

> New feature sets FS23 and FS24 introducing external system integration and intelligent data promotion.

---

## P7-W1-001: MS-EXT-SNOW – Service-Now REST API Connector

**Type:** New Service
**Effort:** 10 MD
**Service:** apps/engine/microservices/units/ms-ext-snow (new)
**Feature Set:** FS23

**Tasks:**
- [ ] **Service scaffolding** (Java 21 + Spring Boot):
  - New module `apps/engine/microservices/units/ms-ext-snow`
  - Base image from `packages/java-base`
  - Dapr sidecar configuration
  - Flyway migrations for connection config & schedule storage
- [ ] **Service-Now REST API Client**:
  - OAuth2 client credentials flow (client_id, client_secret from KeyVault)
  - Basic Auth fallback option (credentials from KeyVault)
  - Generic table query endpoint: `GET /api/now/table/{table_name}`
  - Pagination support (offset/limit) for large datasets
  - Rate limiting awareness (respect `X-RateLimit-*` headers)
  - Retry with exponential backoff on 429/503
- [ ] **Connection Configuration**:
  - Admin UI endpoint (REST via MS-GW): `POST /api/admin/integrations/servicenow`
  - Config entity: `{ instance_url, auth_type, credentials_ref, tables[], mapping_template_id }`
  - Credentials stored as KeyVault references (never plaintext in DB)
  - Connection test endpoint: `POST /api/admin/integrations/servicenow/test`
- [ ] **Data Fetch & Transform**:
  - Fetch raw JSON from Service-Now table
  - Apply Schema Mapping (MS-TMPL via Dapr gRPC) for column normalization
  - Transform to platform-standard JSONB format
  - Store via MS-SINK-TBL (Dapr gRPC)
- [ ] **Error handling**:
  - Connection failures → retry + log to MS-SINK-LOG
  - Auth failures → alert via MS-NOTIF (Dapr Pub/Sub)
  - Partial data fetch → store what's available, log gaps
- [ ] Unit tests with WireMock for Service-Now API simulation

**AC:**
- [ ] Admin configures Service-Now connection with valid credentials → test returns 200
- [ ] Data fetched from Service-Now table appears in platform DB in normalized format
- [ ] Invalid credentials → clear error message, no data loss
- [ ] Credentials never stored in DB plaintext (KeyVault refs only)

---

## P7-W1-002: MS-EXT-SNOW – Scheduled Data Sync (Scheduler)

**Type:** Feature Extension
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-ext-snow (extension)
**Feature Set:** FS23

**Tasks:**
- [ ] **Scheduler Engine**:
  - Spring `@Scheduled` with Cron expressions stored in DB
  - Configurable intervals: daily, weekly, custom cron
  - Schedule entity: `{ integration_id, cron_expression, enabled, last_run, next_run, status }`
  - Distributed lock (Redis) to prevent duplicate runs in multi-instance deployment
- [ ] **Sync Job Execution**:
  - Fetch → Transform → Store pipeline (reuse P7-W1-001 logic)
  - Incremental sync: track `sys_updated_on` for delta fetches
  - Full sync option (admin-triggered)
- [ ] **Job History & Monitoring**:
  - Log each run: `{ job_id, start_time, end_time, records_fetched, records_stored, status, error }`
  - Admin UI: list of sync jobs with status, duration, record counts
  - Failed jobs visible in existing Failed Jobs UI (MS-ADMIN)
- [ ] **Notifications**:
  - Sync completed → optional notification (MS-NOTIF, Dapr Pub/Sub)
  - Sync failed → mandatory alert to Admin
- [ ] Unit tests for scheduler logic, integration tests with Testcontainers

**AC:**
- [ ] Admin creates daily sync schedule → data refreshed automatically every 24h
- [ ] Incremental sync fetches only records updated since last run
- [ ] Failed sync job appears in Failed Jobs UI with error detail
- [ ] Concurrent scheduler instances do not create duplicate syncs

---

## P7-W1-003: MS-GEN-XLS – Excel Report Generator

**Type:** New Service
**Effort:** 5 MD
**Service:** apps/processor/microservices/units/ms-gen-xls (new)
**Feature Set:** FS23

**Tasks:**
- [ ] **Service scaffolding** (Python + FastAPI):
  - New module `apps/processor/microservices/units/ms-gen-xls`
  - Base image from `packages/python-base`
  - Dapr sidecar configuration
  - gRPC service definition in `packages/protos/generator/v1/`
- [ ] **Excel Generation Engine**:
  - `gRPC GenerateExcel(GenerateExcelRequest) → GenerateExcelResponse`
  - Input: query definition (SQL-like) + formatting config
  - openpyxl for .xlsx generation
  - Support: multiple sheets, headers, data types, basic formatting
  - Charts support (bar, line, pie) from data series
- [ ] **Template-Based Generation**:
  - Load Excel template from Blob Storage
  - Placeholder substitution: `{{variable_name}}` in cells
  - Table fill: `{{TABLE:query_name}}` expands to data rows
- [ ] **Output**:
  - Generated .xlsx stored in Blob Storage
  - URL returned in response
  - Cleanup Worker (MS-ATM-CLN) extended to include generated Excel files
- [ ] Unit tests with sample data, verify .xlsx validity with openpyxl

**AC:**
- [ ] Generate Excel report from Service-Now data → valid .xlsx file
- [ ] Template with placeholders → all values substituted correctly
- [ ] Multi-sheet report generated successfully
- [ ] Generated file downloadable from UI

---

## P7-W1-004: MS-EXT-SNOW – Automated Report Distribution

**Type:** Feature Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-ext-snow + MS-NOTIF
**Feature Set:** FS23

**Tasks:**
- [ ] **Distribution Configuration**:
  - Admin defines distribution rules: `{ schedule_id, report_template_id, recipients[], format: XLSX }`
  - Recipients: email addresses or platform user IDs
- [ ] **Distribution Pipeline**:
  - After scheduled sync: trigger MS-GEN-XLS (Dapr gRPC) to generate report
  - Attach generated Excel to email notification (MS-NOTIF, Dapr Pub/Sub)
  - SMTP delivery with .xlsx attachment
- [ ] **Audit Trail**:
  - Each distribution logged: `{ distribution_id, recipients, timestamp, status }`
  - Logged via MS-SINK-LOG (Dapr gRPC)
- [ ] Integration tests for full pipeline: fetch → generate → distribute

**AC:**
- [ ] Scheduled sync triggers automatic Excel generation and email delivery
- [ ] Recipients receive email with correct .xlsx attachment
- [ ] Distribution history visible in admin UI

---

## P7-W1-005: Smart Persistence Promotion – Detection & Proposal

**Type:** New Feature (MS-ADMIN + MS-TMPL Extension)
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-admin + ms-tmpl (extension)
**Feature Set:** FS24

**Tasks:**
- [ ] **Usage Tracking** (MS-TMPL extension):
  - Track Schema Mapping usage: `{ mapping_id, usage_count, last_used, distinct_org_count }`
  - Increment counter on each MS-ORCH → MS-TMPL mapping call
  - Flyway migration for usage tracking table
- [ ] **Promotion Detection**:
  - Background job (hourly): scan mappings with `usage_count >= threshold` (default: 5)
  - Analyze historical data for the mapping: infer column types, lengths, patterns
  - Mark mapping as `CANDIDATE_FOR_PROMOTION`
- [ ] **SQL Schema Proposal Generator**:
  - Analyze JSONB data stored via this mapping
  - Infer optimal PostgreSQL column types:
    - String fields → `VARCHAR(max_observed_length * 1.5)`
    - Numeric fields → `NUMERIC` or `INTEGER` based on observed values
    - Date fields → `DATE` or `TIMESTAMP`
    - Boolean fields → `BOOLEAN`
  - Generate DDL: `CREATE TABLE promoted_{mapping_name} (...)`
  - Include suggested indexes based on query patterns from MS-QRY logs
- [ ] **Admin Notification**:
  - Notify Admin via MS-NOTIF (Dapr Pub/Sub) when new candidate detected
  - Candidate visible in Admin UI with proposed schema
- [ ] Unit tests for type inference and DDL generation

**AC:**
- [ ] Mapping used 5+ times → appears as "Candidate for Promotion" in Admin UI
- [ ] Proposed schema shows correct column types based on historical data
- [ ] Admin can view and modify proposed DDL before approval
- [ ] Notification sent when new candidate detected

---

## P7-W1-006: Smart Persistence Promotion – Admin Approval & Table Creation

**Type:** Feature Extension
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-admin + ms-sink-tbl (extension)
**Feature Set:** FS24

**Tasks:**
- [ ] **Admin Review UI** (API for frontend):
  - `GET /api/admin/promotions` → list candidates with proposed DDL
  - `PUT /api/admin/promotions/{id}` → modify DDL (column names, types, indexes)
  - `POST /api/admin/promotions/{id}/approve` → trigger table creation
  - `DELETE /api/admin/promotions/{id}` → dismiss candidate
- [ ] **Table Creation** (MS-SINK-TBL extension):
  - Execute approved DDL via Flyway dynamic migration
  - Apply RLS policy to new table (copy from template)
  - Register new table in metadata registry
- [ ] **Transparent Routing** (MS-ORCH extension):
  - After promotion: MS-ORCH checks if mapping has promoted table
  - If yes: route data to dedicated table instead of generic JSONB store
  - Dual-write period: write to both old and new for 7 days (configurable)
  - Fallback: if promoted table write fails, fall back to JSONB
- [ ] **Data Migration** (optional, admin-triggered):
  - `POST /api/admin/promotions/{id}/migrate` → backfill historical data from JSONB to new table
  - Progress tracking via polling endpoint
- [ ] Integration tests for full promotion lifecycle

**AC:**
- [ ] Admin approves promotion → dedicated PostgreSQL table created with RLS
- [ ] New data for this mapping automatically routed to dedicated table
- [ ] Historical data migration completes without data loss
- [ ] Existing JSONB queries continue to work during transition

---
