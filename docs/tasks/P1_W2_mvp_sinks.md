# P1 – Wave 2: Sink Services & Supporting Backend (Sonnet)

**Phase:** P1 – MVP Core
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~16 MD
**Depends on:** P1-W1-001 (java-base)

> Write-path services with clear contracts, moderate complexity.

---

## P1-W2-001: MS-SINK-TBL – Table API (Sink)

**Type:** Data Service
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-sink-tbl

**Tasks:**
- [ ] Spring Boot 3.x + gRPC using `packages/java-base`
- [ ] Implement `TableSinkService` from `sink.v1.table` proto:
  - `BulkInsert` – batch insert into PostgreSQL (JSONB columns)
  - `DeleteByFileId` – Saga compensating action
  - `StoreFormResponse` – form data storage
- [ ] Flyway migrations:
  - `parsed_tables` table: `(id, file_id, org_id, source_sheet, headers JSONB, rows JSONB, metadata JSONB, created_at)`
  - `form_responses` table: `(id, org_id, period_id, form_version_id, field_id, value, data_type, submitted_at)`
  - RLS policies on both tables
- [ ] Batch insert optimization (bulk `INSERT ... VALUES`)
- [ ] Docker Compose entry + Dapr sidecar config
- [ ] Unit tests + integration tests with Testcontainers

**AC:**
- [ ] BulkInsert of 100 records < 500ms
- [ ] RLS prevents cross-tenant access
- [ ] DeleteByFileId removes all records for given file_id

---

## P1-W2-002: MS-SINK-DOC – Document API (Sink)

**Type:** Data Service
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-sink-doc

**Tasks:**
- [ ] Spring Boot 3.x + gRPC using `packages/java-base`
- [ ] Implement `DocumentSinkService` from `sink.v1.document` proto:
  - `StoreDocument` – insert into PostgreSQL (JSONB) + queue embedding generation
  - `DeleteByFileId` – Saga compensating action
- [ ] Flyway migrations:
  - `documents` table: `(id, file_id, org_id, document_type, content JSONB, metadata JSONB, created_at)`
  - `document_embeddings` table: `(id, document_id, embedding vector(1536), created_at)`
  - RLS policies
  - pgVector extension setup
- [ ] After StoreDocument → publish Dapr event for embedding generation (async)
- [ ] Docker Compose entry + Dapr sidecar config

**AC:**
- [ ] Document stored with correct org_id
- [ ] Embedding queued flag returned in response
- [ ] RLS isolation verified

---

## P1-W2-003: MS-SINK-LOG – Log API (Sink)

**Type:** Data Service
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-sink-log

**Tasks:**
- [ ] Spring Boot 3.x + gRPC using `packages/java-base`
- [ ] Implement `LogSinkService` from `sink.v1.log` proto:
  - `AppendLog` – single log entry (append-only)
  - `BatchAppendLog` – multiple entries
- [ ] Flyway migration:
  - `processing_logs` table: `(id, file_id, workflow_id, step_name, status, duration_ms, error_detail, metadata JSONB, created_at)`
  - INSERT-only permissions for app user (no UPDATE/DELETE)
- [ ] Docker Compose entry + Dapr sidecar config

**AC:**
- [ ] Log entries are immutable (no UPDATE/DELETE)
- [ ] BatchAppendLog inserts all entries atomically

---

## P1-W2-004: MS-SCAN – Security Scanner Setup

**Type:** Infrastructure / Sidecar
**Effort:** 4 MD
**Service:** apps/engine/microservices/units/ms-scan

**Tasks:**
- [ ] ClamAV Docker container with clamd TCP socket (port 3310)
- [ ] Thin gRPC wrapper service implementing `ScannerService`:
  - `ScanFile` – download from Blob, send to ClamAV via TCP, return result
  - `SanitizeFile` – remove VBA macros (Apache POI), remove external links, upload sanitized version
- [ ] ClamAV signature database update mechanism (daily cron)
- [ ] Docker Compose entry with health check
- [ ] EICAR test file for integration tests

**AC:**
- [ ] EICAR test signature detected
- [ ] Clean file returns `SCAN_RESULT_CLEAN`
- [ ] VBA macros removed from `.xlsm` files
- [ ] Scan completes in < 5s for 50 MB file
