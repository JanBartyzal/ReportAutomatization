# P8 – Wave 1: Microservice Consolidation – Core Architecture (Opus)

**Phase:** P8 – Microservice Consolidation Refactoring
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~35 MD
**Depends on:** P7 (all features implemented), all services stable

> Consolidating 29+ individual microservices into ~8 deployment units (6 Java + 2 Python) while preserving all functionality, Dapr communication, and internal modularity.

---

## Consolidation Strategy

### Current State: 29+ Microservices
**Java (23 services):** MS-AUTH, MS-ING, MS-SCAN, MS-ORCH, MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG, MS-QRY, MS-DASH, MS-SRCH, MS-ADMIN, MS-BATCH, MS-TMPL, MS-NOTIF, MS-LIFECYCLE, MS-PERIOD, MS-FORM, MS-TMPL-PPTX, MS-VER, MS-AUDIT, MS-EXT-SNOW, MS-GW (Nginx)
**Python (8 services):** MS-ATM-PPTX, MS-ATM-XLS, MS-ATM-PDF, MS-ATM-CSV, MS-ATM-AI, MS-ATM-CLN, MS-MCP, MS-GEN-PPTX, MS-GEN-XLS

### Target State: 8 Deployment Units

| # | Unit Name | Merged Services | Rationale |
|---|-----------|----------------|-----------|
| 1 | **engine-core** | MS-AUTH, MS-ADMIN, MS-BATCH, MS-VER, MS-AUDIT | Core platform: auth, admin, batch management, versioning, audit |
| 2 | **engine-ingestor** | MS-ING, MS-SCAN | File ingestion pipeline (tightly coupled) |
| 3 | **engine-orchestrator** | MS-ORCH | Stays separate — complex state machine, independent scaling |
| 4 | **engine-data** | MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG, MS-QRY, MS-DASH, MS-SRCH, MS-TMPL | Data layer: all sinks + read models + schema mapping |
| 5 | **engine-reporting** | MS-LIFECYCLE, MS-PERIOD, MS-FORM, MS-TMPL-PPTX, MS-NOTIF | Reporting lifecycle: forms, periods, templates, notifications |
| 6 | **engine-integrations** | MS-EXT-SNOW | External integrations (extensible for future connectors) |
| 7 | **processor-atomizers** | MS-ATM-PPTX, MS-ATM-XLS, MS-ATM-PDF, MS-ATM-CSV, MS-ATM-AI, MS-ATM-CLN | All Python extractors in one deployable unit |
| 8 | **processor-generators** | MS-GEN-PPTX, MS-GEN-XLS, MS-MCP | All Python generators + AI agent |

### Architecture Principles
- **Internal modularity preserved**: Each former service becomes a Spring Boot module/package within the consolidated unit
- **Dapr app-id mapping**: Each consolidated unit gets ONE Dapr app-id; internal routing via gRPC service names
- **Shared DB connections**: Services within same unit share connection pool
- **Independent scaling**: engine-orchestrator and processor-atomizers remain separately scalable
- **Zero functionality loss**: All gRPC/REST endpoints preserved, all Dapr PubSub topics unchanged

---

## P8-W1-001: engine-core – Auth, Admin, Batch, Versioning, Audit

**Type:** Consolidation
**Effort:** 8 MD
**Source:** ms-auth, ms-admin, ms-batch, ms-ver, ms-audit

**Tasks:**
- [ ] **Multi-module Gradle project**:
  - Root: `apps/engine/engine-core/`
  - Submodules: `auth`, `admin`, `batch`, `versioning`, `audit`, `common`
  - Shared `common` module: DB config, auth filters, Dapr client, health check
  - Single `application.yml` with profile-based config
  - Single `Application.java` entry point
- [ ] **Merge Spring contexts**:
  - Unified `@SpringBootApplication` with component scanning per package
  - Shared `SecurityConfig` with route-based authorization rules
  - Shared `DaprConfig` with single sidecar connection
  - Consolidated Flyway migrations (ordered by original service prefix)
- [ ] **Port allocation**:
  - Single HTTP port (8081) for all REST endpoints
  - REST path prefixes preserved: `/api/auth/*`, `/api/admin/*`, `/api/batch/*`, `/api/versions/*`, `/api/audit/*`
  - Single gRPC port (50051) for all internal gRPC services
- [ ] **Dapr consolidation**:
  - Single Dapr app-id: `engine-core`
  - PubSub subscriptions from ms-admin, ms-batch merged into one subscription config
  - State store shared
- [ ] **Health & readiness**:
  - Unified `/actuator/health` with sub-indicators per module
  - Readiness probe checks all module dependencies
- [ ] **Tests**:
  - Existing unit tests moved to respective submodules
  - Integration test verifying all endpoints work from single process
  - Verify RLS still enforced after DB connection consolidation

**AC:**
- [ ] Single JAR serves all auth, admin, batch, versioning, audit endpoints
- [ ] All existing unit tests pass without modification
- [ ] Dapr gRPC calls from MS-ORCH reach correct service handlers
- [ ] DB migrations run in correct order across all modules

---

## P8-W1-002: engine-data – Sinks, Query, Dashboard, Search, Template

**Type:** Consolidation
**Effort:** 8 MD
**Source:** ms-sink-tbl, ms-sink-doc, ms-sink-log, ms-qry, ms-dash, ms-srch, ms-tmpl

**Tasks:**
- [ ] **Multi-module Gradle project**:
  - Root: `apps/engine/engine-data/`
  - Submodules: `sink-tbl`, `sink-doc`, `sink-log`, `query`, `dashboard`, `search`, `template`, `common`
  - Shared PostgreSQL connection pool (HikariCP, max 30 connections)
  - Shared Redis client for caching (MS-QRY cache, rate limit counters)
- [ ] **gRPC service consolidation**:
  - Single gRPC server exposing all sink services + template mapping
  - Service names preserved: `TableSinkService`, `DocumentSinkService`, `LogSinkService`, `TemplateMappingService`
  - MS-ORCH continues calling by service name (Dapr routes to engine-data)
- [ ] **REST endpoint consolidation**:
  - Single HTTP port (8100) serving all read-model endpoints
  - Path prefixes: `/api/query/*`, `/api/dashboards/*`, `/api/search/*`, `/api/templates/*`
- [ ] **CQRS preserved**:
  - Write side (sinks) accessed only via gRPC from MS-ORCH
  - Read side (query, dashboard, search) accessed via REST from frontend
  - Separation maintained by package structure, not network boundary
- [ ] **Flyway migrations merged**:
  - Prefix convention: `V{service}_{version}__description.sql`
  - Single migration history table
- [ ] **Tests**: all existing tests + integration test for cross-module queries

**AC:**
- [ ] Single process handles all data operations (write + read)
- [ ] MS-ORCH gRPC calls to sinks and template work unchanged
- [ ] Frontend REST calls to query/dashboard/search work unchanged
- [ ] Redis caching works for query module within consolidated service

---

## P8-W1-003: engine-reporting – Lifecycle, Period, Form, Templates, Notifications

**Type:** Consolidation
**Effort:** 7 MD
**Source:** ms-lifecycle, ms-period, ms-form, ms-tmpl-pptx, ms-notif

**Tasks:**
- [ ] **Multi-module Gradle project**:
  - Root: `apps/engine/engine-reporting/`
  - Submodules: `lifecycle`, `period`, `form`, `pptx-template`, `notification`, `common`
- [ ] **Event consolidation**:
  - Dapr PubSub subscriptions merged: `report.status_changed`, `notify`, `deadline.*`
  - Internal module-to-module calls become direct method invocations (no network hop)
  - e.g., MS-LIFECYCLE publishing event → MS-NOTIF consuming → now same process: direct Spring Event
- [ ] **REST endpoints**:
  - Single HTTP port (8105) serving: `/api/reports/*`, `/api/periods/*`, `/api/forms/*`, `/api/templates/pptx/*`, `/api/notifications/*`
- [ ] **gRPC services**:
  - MS-ORCH calls to lifecycle and form services via single Dapr app-id `engine-reporting`
- [ ] **Shared state**:
  - Period ↔ Form ↔ Lifecycle tight coupling benefits from shared DB connection
  - Cross-module validation (e.g., form deadline from period) becomes local call
- [ ] **WebSocket/SSE**: Notification push remains in this unit
- [ ] **Tests**: all existing + integration for lifecycle→notification flow in single process

**AC:**
- [ ] Report lifecycle, forms, periods, notifications served from single process
- [ ] PubSub events from external services (MS-ORCH) still received correctly
- [ ] Internal events (lifecycle → notification) handled via Spring ApplicationEvent
- [ ] WebSocket/SSE push notifications still work

---

## P8-W1-004: processor-atomizers – All Python Extractors

**Type:** Consolidation
**Effort:** 6 MD
**Source:** ms-atm-pptx, ms-atm-xls, ms-atm-pdf, ms-atm-csv, ms-atm-ai, ms-atm-cln

**Tasks:**
- [ ] **Python package structure**:
  - Root: `apps/processor/processor-atomizers/`
  - Packages: `atomizers/pptx/`, `atomizers/xls/`, `atomizers/pdf/`, `atomizers/csv/`, `atomizers/ai/`, `atomizers/cleanup/`
  - Shared: `common/` (tracing, logging, blob client, gRPC base)
  - Single `main.py` entry point (FastAPI + gRPC server)
- [ ] **gRPC server consolidation**:
  - Single gRPC server (port 50090) registering all atomizer services
  - Service names preserved: `PptxAtomizerService`, `ExcelAtomizerService`, etc.
  - Dapr app-id: `processor-atomizers`
- [ ] **Dependency management**:
  - Single `requirements.txt` / `pyproject.toml`
  - Heavy dependencies (python-pptx, openpyxl, Tesseract, LiteLLM) all in one image
  - Optional: lazy imports for memory optimization
- [ ] **Cleanup Worker**: Converted from CronJob to internal scheduled task (APScheduler)
- [ ] **LibreOffice Headless**: Included in Docker image for PPTX slide rendering
- [ ] **Tests**: all existing pytest tests + integration test for multi-format processing

**AC:**
- [ ] Single Python process handles all file format extractions
- [ ] MS-ORCH gRPC calls route to correct atomizer handler
- [ ] Cleanup worker runs on schedule within same process
- [ ] Docker image builds successfully with all dependencies

---

## P8-W1-005: processor-generators – PPTX Generator, Excel Generator, MCP

**Type:** Consolidation
**Effort:** 4 MD
**Source:** ms-gen-pptx, ms-gen-xls, ms-mcp

**Tasks:**
- [ ] **Python package structure**:
  - Root: `apps/processor/processor-generators/`
  - Packages: `generators/pptx/`, `generators/xls/`, `mcp/`
  - Shared: `common/`
  - Single entry point
- [ ] **gRPC + REST consolidation**:
  - gRPC (port 50091): `PptxGeneratorService`, `ExcelGeneratorService`
  - REST (port 8111): MCP Server endpoints (AI agent integration)
  - Dapr app-id: `processor-generators`
- [ ] **Dependencies**: python-pptx, matplotlib, openpyxl, LiteLLM
- [ ] **Tests**: all existing + integration test

**AC:**
- [ ] PPTX and Excel generation + MCP from single process
- [ ] MS-ORCH gRPC calls route correctly
- [ ] MCP Server REST endpoints accessible via API Gateway

---

## P8-W1-006: engine-orchestrator – Stays Independent

**Type:** Verification
**Effort:** 2 MD
**Source:** ms-orch (unchanged)

**Tasks:**
- [ ] **Dapr routing updates**:
  - Update all Dapr service invocation targets to new app-ids:
    - `ms-auth` → `engine-core`
    - `ms-sink-tbl`, `ms-sink-doc`, `ms-sink-log` → `engine-data`
    - `ms-tmpl` → `engine-data`
    - `ms-atm-*` → `processor-atomizers`
    - `ms-gen-pptx` → `processor-generators`
    - `ms-lifecycle` → `engine-reporting`
    - `ms-notif` → `engine-reporting`
  - gRPC service names unchanged (just Dapr app-id routing changes)
- [ ] **Workflow definitions**: Update JSON workflow files with new service targets
- [ ] **Integration tests**: Full pipeline test with consolidated services
- [ ] **Backwards compatibility**: Verify all Saga compensating actions still work

**AC:**
- [ ] MS-ORCH routes to all consolidated services correctly
- [ ] Full file processing pipeline works end-to-end
- [ ] Saga rollback still functions across consolidated boundaries

---
