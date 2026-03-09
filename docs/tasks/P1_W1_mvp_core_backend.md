# P1 – Wave 1: Core Backend Services (Opus)

**Phase:** P1 – MVP Core
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~56 MD
**Depends on:** P0 (proto definitions and API contracts)

> Complex services with deep business logic, Saga patterns, state machines, and security-critical code.

---

## P1-W1-001: Java Base Image & Shared Libraries

**Type:** Infrastructure
**Effort:** 3 MD
**Service:** packages/java-base

**Tasks:**
- [ ] `packages/java-base/` – shared parent POM / Gradle build with:
  - Spring Boot 3.x BOM
  - Java 21 (Virtual Threads enabled)
  - Dapr SDK dependency (`io.dapr:dapr-sdk`, `io.dapr:dapr-sdk-springboot`)
  - gRPC dependencies (`grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`)
  - OpenTelemetry agent dependency
  - Flyway dependency
  - Common test dependencies (JUnit 5, Testcontainers, WireMock)
- [ ] Shared modules:
  - `java-base-security` – JWT validation utilities, RequestContext extraction
  - `java-base-grpc` – gRPC server/client interceptors for context propagation
  - `java-base-dapr` – Dapr client wrapper with retry logic
  - `java-base-observability` – OTEL + structured logging config
- [ ] Dockerfile template (multi-stage: build → JRE 21 slim runtime)
- [ ] GraalVM Native Image build profile (for future production use)

**AC:**
- [ ] New Java service bootstraps in < 30 seconds using shared base
- [ ] gRPC interceptor automatically propagates `RequestContext` (trace_id, user_id, org_id)
- [ ] Structured JSON logging output from first boot

---

## P1-W1-002: Python Base Image & Shared Libraries

**Type:** Infrastructure
**Effort:** 2 MD
**Service:** packages/python-base

**Tasks:**
- [ ] `packages/python-base/` – shared Python package:
  - FastAPI + Pydantic v2
  - gRPC server framework (`grpcio`, `grpcio-tools`, `grpcio-health-checking`)
  - Dapr client SDK (`dapr`, `dapr-ext-grpc`)
  - OpenTelemetry instrumentation
  - httpx for async HTTP (Blob Storage downloads)
  - Common test dependencies (pytest, pytest-asyncio, pytest-mock)
- [ ] Shared modules:
  - `python_base/grpc_server.py` – base gRPC server with health check
  - `python_base/context.py` – RequestContext extraction from gRPC metadata
  - `python_base/blob.py` – Azure Blob Storage client (download/upload)
  - `python_base/logging_config.py` – structured JSON logging
- [ ] Dockerfile template (Python 3.11 slim, non-root user)
- [ ] `pyproject.toml` with shared dependencies

**AC:**
- [ ] New Python atomizer bootstraps with `import python_base` and has gRPC server running
- [ ] Blob download/upload works with Azurite (local) and Azure Blob (prod)

---

## P1-W1-003: MS-ORCH – Custom Orchestrator (Core Engine)

**Type:** Core Service
**Effort:** 18 MD
**Service:** apps/engine/microservices/units/ms-orch

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Spring State Machine** configuration:
  - States: `RECEIVED`, `SCANNING`, `PARSING`, `MAPPING`, `STORING`, `COMPLETED`, `FAILED`
  - Transitions with guards and actions
  - JSON workflow definitions in `resources/workflows/`
- [ ] **gRPC Server** (port 50051):
  - Implement `OrchestratorService` from `orchestrator.v1` proto
  - `StartFileWorkflow` – create workflow instance, persist to Redis
  - `GetWorkflowStatus` – read from Redis/PG
  - `RetryWorkflow` – re-trigger from last failed step
  - `CancelWorkflow` – mark cancelled, publish event
- [ ] **Dapr Pub/Sub Subscriber**:
  - Subscribe to `file-uploaded` topic
  - Deserialize `FileUploadedEvent`, trigger `StartFileWorkflow`
- [ ] **Dapr gRPC Client** calls:
  - MS-ATM-* (atomizers) via Dapr service invocation
  - MS-SINK-* (sinks) via Dapr service invocation
  - MS-TMPL (template mapping) via Dapr service invocation
- [ ] **Saga Pattern**:
  - Each step defines compensating action
  - On failure: execute compensating actions in reverse order
  - Example: if MS-SINK-TBL.BulkInsert fails → MS-SINK-DOC.DeleteByFileId
- [ ] **Type-Safe Contracts**:
  - Java interfaces for each Atomizer and Sink call
  - DTOs generated from proto files
  - No loose JSON objects in workflow processing
- [ ] **Error Handling**:
  - Exponential backoff: 3 retries (1s, 5s, 30s)
  - After retries exhausted → write to `failed_jobs` table (DLQ)
  - Specific exception types: `ParsingException`, `StorageException`, `VirusDetectedException`
- [ ] **Idempotence**:
  - Redis key: `file_id:step_hash` → prevents duplicate processing
  - Duplicate detection returns cached result
- [ ] **State Management**:
  - Redis: running workflow state (low latency)
  - PostgreSQL: paused/waiting/failed workflows (persistence)
- [ ] **Router Logic**:
  - File type → Atomizer mapping (PPTX→MS-ATM-PPTX, etc.)
  - Filter after extraction: table data → MS-SINK-TBL, text → MS-SINK-DOC
- [ ] Flyway migrations for `failed_jobs` and `workflow_history` tables
- [ ] Dockerfile + Docker Compose entry (port 8095:8080, debug 5010)

**AC:**
- [ ] Upload new PPTX → workflow starts automatically (Dapr Pub/Sub)
- [ ] Atomizer HTTP 500 → data saved to `failed_jobs`, not lost
- [ ] Re-upload same `file_id` → no duplicate records in DB
- [ ] Full workflow: scan → parse → map → store completes < 30s for 50-slide PPTX

---

## P1-W1-004: MS-AUTH – Auth Service (Full Implementation)

**Type:** Core Service
**Effort:** 9 MD
**Service:** apps/engine/microservices/units/ms-auth

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Token Validation** (`POST /api/auth/verify`):
  - Azure Entra ID JWT v2 validation (JWKS)
  - Claims extraction: `oid`, `tid`, `roles`, `groups`
  - JWKS cache with 5-min TTL
  - Response headers: `X-User-Id`, `X-Org-Id`, `X-Roles`
- [ ] **RBAC Engine**:
  - Role hierarchy: HoldingAdmin > Admin > Editor > Viewer
  - Org-scoped permissions
  - AAD Security Group → internal role mapping
  - Conditional Access: membership in specific AAD group required
- [ ] **User Context** (`GET /api/auth/me`):
  - Returns current user's organizations, roles, active org
- [ ] **Org Switch** (`POST /api/auth/switch-org`):
  - Validates user membership, updates active org
- [ ] **KeyVault Integration**:
  - Reads secrets at startup via MSI
  - Fallback to env vars for local dev
- [ ] **API Key Validation**:
  - Bearer token validation for service accounts
  - bcrypt hashed keys stored in DB
- [ ] Flyway migrations: `organizations`, `roles`, `user_roles` tables
- [ ] PostgreSQL RLS setup for `organizations` table
- [ ] Seed data: default roles, test organizations
- [ ] Unit tests: valid/expired/wrong-issuer tokens, role checks
- [ ] Integration tests: WireMock for JWKS endpoint

**AC:**
- [ ] `401` for missing token, `403` for insufficient role
- [ ] HoldingAdmin sees all child orgs, Editor sees only own org
- [ ] KeyVault secret available as env var at startup

---

## P1-W1-005: MS-ING – File Ingestor (Full Implementation)

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/engine/microservices/units/ms-ing

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Upload Endpoint** (`POST /api/upload`):
  - Multipart/form-data streaming → Azure Blob Storage
  - Never loads entire file into memory (stream-through)
  - Max file size: 50 MB (PPTX/XLSX/CSV), 100 MB (PDF)
  - `upload_purpose` parameter: `PARSE` (default) or `FORM_IMPORT`
- [ ] **MIME Validation**:
  - Allowlist: `.pptx`, `.xlsx`, `.pdf`, `.csv`
  - Magic bytes verification (binary header check)
  - Reject with `415` for unsupported types
- [ ] **Security Scan** (Dapr gRPC → MS-SCAN):
  - Call `ScannerService.ScanFile()` before saving to Blob
  - Infected → reject with `422`
- [ ] **Sanitization** (Dapr gRPC → MS-SCAN):
  - Call `ScannerService.SanitizeFile()` – remove VBA macros, external links
  - Store original in `_raw/` path (90-day retention)
  - Store sanitized version at standard path
- [ ] **Metadata Write**:
  - Insert into `files` table (org_id, user_id, filename, size, mime, blob_url, scan_status)
- [ ] **Blob Naming**: `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}`
- [ ] **Orchestrator Trigger**:
  - Dapr Pub/Sub → topic `file-uploaded`
  - Event payload: `FileUploadedEvent` proto
- [ ] **File List** (`GET /api/files`):
  - Paginated list for current org (RLS enforced)
- [ ] **File Detail** (`GET /api/files/{file_id}`):
  - File metadata + processing status (join with workflow status)
- [ ] Flyway migration: `files` table with RLS
- [ ] Docker Compose entry (port 8082:8000, debug 5006)

**AC:**
- [ ] 20 MB PPTX upload < 5s on 100 Mbps
- [ ] EICAR test virus → `422 { error: "INFECTED" }`
- [ ] `.exe` upload → `415 Unsupported Media Type`
- [ ] Orchestrator event delivered within 1s of successful upload

---

## P1-W1-006: MS-ATM-PPTX – PPTX Atomizer (Full Implementation)

**Type:** Core Service
**Effort:** 16 MD
**Service:** apps/processor/microservices/units/ms-atm-pptx

**Tasks:**
- [ ] FastAPI + gRPC project using `packages/python-base`
- [ ] **gRPC Server** (port 50051):
  - Implement `PptxAtomizerService` from `atomizer.v1.pptx` proto
- [ ] **ExtractStructure**:
  - Download PPTX from Blob Storage
  - python-pptx: enumerate slides, detect titles, layouts, shapes
  - Return `PptxStructureResponse` with slide metadata
- [ ] **ExtractSlideContent**:
  - Extract text blocks with position data
  - Extract tables with headers and rows
  - Extract speaker notes
  - **MetaTable Logic**: reconstruct tables from unstructured text
    - Visual delimiter detection (tabs, spaces)
    - Header row comparison
    - Confidence threshold > 0.85 → table, else → plain text with `low_confidence` flag
- [ ] **RenderSlideImage**:
  - LibreOffice Headless (`--convert-to png`) for high-fidelity rendering
  - Output: 1280x720 PNG (~200 KB per slide)
  - Upload to Blob Storage, return URL
  - Fallback: python-pptx basic rendering if LibreOffice unavailable
- [ ] **ExtractAll** (batch):
  - Process all slides in sequence
  - Collect errors per slide (partial success)
  - Return `PptxFullExtractionResponse`
- [ ] **Blob Integration**:
  - Download source PPTX from `blob_url`
  - Upload PNG artifacts to Blob Storage
- [ ] LibreOffice Headless in Docker container
- [ ] Dockerfile with LibreOffice + python-pptx + Tesseract (prepared)
- [ ] Docker Compose entry (port 8090:8000, debug 5678)
- [ ] Unit tests: sample PPTX files with various layouts
- [ ] Edge cases: empty slides, merged cells, SmartArt, charts

**AC:**
- [ ] Returns structured JSON + artifact URLs – never inline binary
- [ ] Corrupt file → `422` with error detail, not `500`
- [ ] Atomizer downloads file from Blob itself (via URL in request)
- [ ] MetaTable confidence < 0.85 → `low_confidence` flag set
