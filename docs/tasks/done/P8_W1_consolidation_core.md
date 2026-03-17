# P8 – Wave 1: Microservice Consolidation – Core Architecture (Opus)

**Phase:** P8 – Microservice Consolidation Refactoring
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~41 MD
**Depends on:** P7 (all features implemented), all services stable

> Consolidating 29+ individual microservices into ~8 deployment units (6 Java + 2 Python) while preserving all functionality, Dapr communication, and internal modularity.

---

## Consolidation Strategy

### Current State: 29+ Microservices
**Java (23 services):** engine-core:auth, engine-ingestor, engine-ingestor:scanner, engine-orchestrator, engine-data:sink-tbl, engine-data:sink-doc, engine-data:sink-log, engine-data:query, engine-data:dashboard, engine-data:search, engine-core:admin, engine-core:batch, engine-data:template, engine-reporting:notification, engine-reporting:lifecycle, engine-reporting:period, engine-reporting:form, engine-reporting:pptx-template, engine-core:versioning, engine-core:audit, engine-integrations:servicenow, router (Nginx)
**Python (8 services):** processor-atomizers:pptx, processor-atomizers:xls, processor-atomizers:pdf, processor-atomizers:csv, processor-atomizers:ai, processor-atomizers:cleanup, processor-generators:mcp, processor-generators:pptx, processor-generators:xls

### Target State: 8 Deployment Units

| # | Unit Name | Merged Services | Rationale |
|---|-----------|----------------|-----------|
| 1 | **engine-core** | engine-core:auth, engine-core:admin, engine-core:batch, engine-core:versioning, engine-core:audit | Core platform: auth, admin, batch management, versioning, audit |
| 2 | **engine-ingestor** | engine-ingestor, engine-ingestor:scanner | File ingestion pipeline (tightly coupled) |
| 3 | **engine-orchestrator** | engine-orchestrator | Stays separate — complex state machine, independent scaling |
| 4 | **engine-data** | engine-data:sink-tbl, engine-data:sink-doc, engine-data:sink-log, engine-data:query, engine-data:dashboard, engine-data:search, engine-data:template | Data layer: all sinks + read models + schema mapping |
| 5 | **engine-reporting** | engine-reporting:lifecycle, engine-reporting:period, engine-reporting:form, engine-reporting:pptx-template, engine-reporting:notification | Reporting lifecycle: forms, periods, templates, notifications |
| 6 | **engine-integrations** | engine-integrations:servicenow | External integrations (extensible for future connectors) |
| 7 | **processor-atomizers** | processor-atomizers:pptx, processor-atomizers:xls, processor-atomizers:pdf, processor-atomizers:csv, processor-atomizers:ai, processor-atomizers:cleanup | All Python extractors in one deployable unit |
| 8 | **processor-generators** | processor-generators:pptx, processor-generators:xls, processor-generators:mcp | All Python generators + AI agent |

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
**Status:** DONE

**Tasks:**
- [x] **Multi-module Maven project**:
  - Root: `apps/engine/engine-core/`
  - Submodules: `auth`, `admin`, `batch`, `versioning`, `audit`, `common`
  - Shared `common` module: DB config, auth filters, Dapr client, health check
  - Single `application.yml` with profile-based config
  - Single `Application.java` entry point
- [x] **Merge Spring contexts**:
  - Unified `@SpringBootApplication` with component scanning per package
  - Shared `SecurityConfig` with route-based authorization rules
  - Shared `DaprConfig` with single sidecar connection
  - Consolidated Flyway migrations (ordered by original service prefix)
- [x] **Port allocation**:
  - Single HTTP port (8081) for all REST endpoints
  - REST path prefixes preserved: `/api/auth/*`, `/api/admin/*`, `/api/batch/*`, `/api/versions/*`, `/api/audit/*`
  - Single gRPC port (50051) for all internal gRPC services
- [x] **Dapr consolidation**:
  - Single Dapr app-id: `engine-core`
  - PubSub subscriptions from ms-admin, ms-batch merged into one subscription config
  - State store shared
- [x] **Health & readiness**:
  - Unified `/actuator/health` with sub-indicators per module
  - Readiness probe checks all module dependencies
- [x] **Tests**:
  - Existing unit tests moved to respective submodules
  - Integration test verifying all endpoints work from single process
  - Verify RLS still enforced after DB connection consolidation

**AC:**
- [x] Single JAR serves all auth, admin, batch, versioning, audit endpoints
- [x] All existing unit tests pass without modification
- [x] Dapr gRPC calls from engine-orchestrator reach correct service handlers
- [x] DB migrations run in correct order across all modules

---

## P8-W1-002: engine-data – Sinks, Query, Dashboard, Search, Template

**Type:** Consolidation
**Effort:** 8 MD
**Source:** ms-sink-tbl, ms-sink-doc, ms-sink-log, ms-qry, ms-dash, ms-srch, ms-tmpl
**Status:** DONE

**Tasks:**
- [x] **Multi-module Maven project**:
  - Root: `apps/engine/engine-data/`
  - Submodules: `sink-tbl`, `sink-doc`, `sink-log`, `query`, `dashboard`, `search`, `template`, `common`
  - Shared PostgreSQL connection pool (HikariCP, max 30 connections)
  - Shared Redis client for caching (engine-data:query cache, rate limit counters)
- [x] **gRPC service consolidation**:
  - Single gRPC server exposing all sink services + template mapping
  - Service names preserved: `TableSinkService`, `DocumentSinkService`, `LogSinkService`, `TemplateMappingService`
  - engine-orchestrator continues calling by service name (Dapr routes to engine-data)
- [x] **REST endpoint consolidation**:
  - Single HTTP port (8100) serving all read-model endpoints
  - Path prefixes: `/api/query/*`, `/api/dashboards/*`, `/api/search/*`, `/api/templates/*`
- [x] **CQRS preserved**:
  - Write side (sinks) accessed only via gRPC from engine-orchestrator
  - Read side (query, dashboard, search) accessed via REST from frontend
  - Separation maintained by package structure, not network boundary
- [x] **Flyway migrations merged**:
  - Prefix convention: `V{service}_{version}__description.sql`
  - Single migration history table
- [x] **Tests**: all existing tests + integration test for cross-module queries

**AC:**
- [x] Single process handles all data operations (write + read)
- [x] engine-orchestrator gRPC calls to sinks and template work unchanged
- [x] Frontend REST calls to query/dashboard/search work unchanged
- [x] Redis caching works for query module within consolidated service

---

## P8-W1-003: engine-reporting – Lifecycle, Period, Form, Templates, Notifications

**Type:** Consolidation
**Effort:** 7 MD
**Source:** ms-lifecycle, ms-period, ms-form, ms-tmpl-pptx, ms-notif
**Status:** DONE

**Tasks:**
- [x] **Multi-module Maven project**:
  - Root: `apps/engine/engine-reporting/`
  - Submodules: `lifecycle`, `period`, `form`, `pptx-template`, `notification`, `common`
- [x] **Event consolidation**:
  - Dapr PubSub subscriptions merged: `report.status_changed`, `notify`, `deadline.*`
  - Internal module-to-module calls become direct method invocations (no network hop)
  - e.g., engine-reporting:lifecycle publishing event → engine-reporting:notification consuming → now same process: direct Spring Event
- [x] **REST endpoints**:
  - Single HTTP port (8105) serving: `/api/reports/*`, `/api/periods/*`, `/api/forms/*`, `/api/templates/pptx/*`, `/api/notifications/*`
- [x] **gRPC services**:
  - engine-orchestrator calls to lifecycle and form services via single Dapr app-id `engine-reporting`
- [x] **Shared state**:
  - Period ↔ Form ↔ Lifecycle tight coupling benefits from shared DB connection
  - Cross-module validation (e.g., form deadline from period) becomes local call
- [x] **WebSocket/SSE**: Notification push remains in this unit
- [x] **Tests**: all existing + integration for lifecycle→notification flow in single process

**AC:**
- [x] Report lifecycle, forms, periods, notifications served from single process
- [x] PubSub events from external services (engine-orchestrator) still received correctly
- [x] Internal events (lifecycle → notification) handled via Spring ApplicationEvent
- [x] WebSocket/SSE push notifications still work

---

## P8-W1-004: processor-atomizers – All Python Extractors

**Type:** Consolidation
**Effort:** 6 MD
**Source:** ms-atm-pptx, ms-atm-xls, ms-atm-pdf, ms-atm-csv, ms-atm-ai, ms-atm-cln
**Status:** DONE

**Tasks:**
- [x] **Python package structure**:
  - Root: `apps/processor/processor-atomizers/`
  - Packages: `atomizers/pptx/`, `atomizers/xls/`, `atomizers/pdf/`, `atomizers/csv/`, `atomizers/ai/`, `atomizers/cleanup/`
  - Shared: `common/` (tracing, logging, blob client, gRPC base)
  - Single `main.py` entry point (FastAPI + gRPC server)
- [x] **gRPC server consolidation**:
  - Single gRPC server (port 50090) registering all atomizer services
  - Service names preserved: `PptxAtomizerService`, `ExcelAtomizerService`, etc.
  - Dapr app-id: `processor-atomizers`
- [x] **Dependency management**:
  - Single `requirements.txt` / `pyproject.toml`
  - Heavy dependencies (python-pptx, openpyxl, Tesseract, LiteLLM) all in one image
  - Optional: lazy imports for memory optimization
- [x] **Cleanup Worker**: Converted from CronJob to internal scheduled task (APScheduler)
- [x] **LibreOffice Headless**: Included in Docker image for PPTX slide rendering
- [x] **Tests**: all existing pytest tests + integration test for multi-format processing

**AC:**
- [x] Single Python process handles all file format extractions
- [x] engine-orchestrator gRPC calls route to correct atomizer handler
- [x] Cleanup worker runs on schedule within same process
- [x] Docker image builds successfully with all dependencies

---

## P8-W1-005: processor-generators – PPTX Generator, Excel Generator, MCP

**Type:** Consolidation
**Effort:** 4 MD
**Source:** ms-gen-pptx, ms-gen-xls, ms-mcp
**Status:** DONE

**Tasks:**
- [x] **Python package structure**:
  - Root: `apps/processor/processor-generators/`
  - Packages: `generators/pptx/`, `generators/xls/`, `mcp/`
  - Shared: `common/`
  - Single entry point
- [x] **gRPC + REST consolidation**:
  - gRPC (port 50091): `PptxGeneratorService`, `ExcelGeneratorService`
  - REST (port 8111): MCP Server endpoints (AI agent integration)
  - Dapr app-id: `processor-generators`
- [x] **Dependencies**: python-pptx, matplotlib, openpyxl, LiteLLM
- [x] **Tests**: all existing + integration test

**AC:**
- [x] PPTX and Excel generation + MCP from single process
- [x] engine-orchestrator gRPC calls route correctly
- [x] MCP Server REST endpoints accessible via API Gateway

---

## P8-W1-006: engine-orchestrator – Stays Independent

**Type:** Verification
**Effort:** 2 MD
**Source:** ms-orch (unchanged)
**Status:** DONE

**Tasks:**
- [x] **Dapr routing updates**:
  - Update all Dapr service invocation targets to new app-ids:
    - `ms-auth` → `engine-core`
    - `ms-sink-tbl`, `ms-sink-doc`, `ms-sink-log` → `engine-data`
    - `ms-tmpl` → `engine-data`
    - `ms-atm-*` → `processor-atomizers`
    - `ms-gen-pptx` → `processor-generators`
    - `ms-lifecycle` → `engine-reporting`
    - `ms-notif` → `engine-reporting`
    - `ms-ing` → `engine-ingestor`
    - `ms-ext-snow` → `engine-integrations`
  - gRPC service names unchanged (just Dapr app-id routing changes)
- [x] **Workflow definitions**: Update JSON workflow files with new service targets
- [x] **Integration tests**: Full pipeline test with consolidated services
- [x] **Backwards compatibility**: Verify all Saga compensating actions still work

**AC:**
- [x] engine-orchestrator routes to all consolidated services correctly
- [x] Full file processing pipeline works end-to-end
- [x] Saga rollback still functions across consolidated boundaries

---

## P8-W1-007: engine-ingestor – File Ingestor & Security Scanner

**Type:** Consolidation
**Effort:** 3 MD
**Source:** ms-ing, ms-scan
**Status:** DONE

**Tasks:**
- [x] **Multi-module Maven project**:
  - Root: `apps/engine/engine-ingestor/`
  - Submodules: `ingestor`, `scanner`, `common`
  - Shared `common` module: Blob client config, Dapr client, health check, auth filters
  - Single `application.yml` with profile-based config
  - Single `Application.java` entry point
- [x] **Merge Spring contexts**:
  - Unified `@SpringBootApplication` with component scanning per package
  - Shared `DaprConfig` with single sidecar connection
  - ClamAV scanner integrated as internal service (ICAP sidecar remains external container)
- [x] **Port allocation**:
  - Single HTTP port (8082) for REST endpoints
  - REST path prefixes preserved: `/api/ingest/*`, `/api/scan/*`
  - Single gRPC port (50052) for internal gRPC services
- [x] **Dapr consolidation**:
  - Single Dapr app-id: `engine-ingestor`
  - PubSub: `file.uploaded` event publishing merged
  - engine-orchestrator calls updated to target `engine-ingestor` instead of `ms-ing`
- [x] **Blob Storage integration**:
  - Shared Azure Blob client (streaming upload, MIME validation)
  - Scan triggered internally after upload (direct method call instead of network hop)
- [x] **Health & readiness**:
  - Unified `/actuator/health` with sub-indicators for ingestor and ClamAV connectivity
  - Readiness probe checks Blob Storage + ClamAV availability
- [x] **Tests**:
  - Existing unit tests moved to respective submodules
  - Integration test: upload → scan → PubSub event published
  - Verify MIME validation and file sanitization still enforced

**AC:**
- [x] Single JAR handles file upload, MIME validation, and antivirus scanning
- [x] All existing unit tests pass without modification
- [x] engine-orchestrator receives `file.uploaded` event from `engine-ingestor`
- [x] ClamAV sidecar integration works from consolidated service
- [x] Streaming upload to Blob Storage works unchanged

---

## P8-W1-008: engine-integrations – External Connectors

**Type:** Consolidation
**Effort:** 2 MD
**Source:** ms-ext-snow
**Status:** DONE

**Tasks:**
- [x] **Multi-module Maven project**:
  - Root: `apps/engine/engine-integrations/`
  - Submodules: `servicenow`, `common`
  - `common` module: shared HTTP client config, retry policies, circuit breaker, auth filters
  - Extensible structure for future connectors (e.g., SAP, Jira)
  - Single `application.yml` with profile-based config
  - Single `Application.java` entry point
- [x] **REST endpoint consolidation**:
  - Single HTTP port (8106) serving: `/api/integrations/servicenow/*`
  - Future connectors will add: `/api/integrations/{connector}/*`
- [x] **gRPC services**:
  - Single gRPC port (50056) for internal calls from engine-orchestrator
  - Service name preserved: `ServiceNowIntegrationService`
- [x] **Dapr consolidation**:
  - Single Dapr app-id: `engine-integrations`
  - PubSub subscriptions from engine-integrations:servicenow merged
  - State store for sync status shared
- [x] **Resilience patterns**:
  - Circuit breaker for external API calls (Service-Now)
  - Retry with exponential backoff
  - Fallback caching for read operations
- [x] **Tests**:
  - Existing unit tests moved to `servicenow` submodule
  - Integration test with mocked Service-Now API
  - Verify circuit breaker triggers correctly

**AC:**
- [x] Single JAR serves all Service-Now integration endpoints
- [x] All existing unit tests pass without modification
- [x] engine-orchestrator gRPC calls route to `engine-integrations` correctly
- [x] Circuit breaker and retry patterns functional
- [x] Structure ready for adding future connectors without architectural changes

---

## P8-W1-009: API Gateway – Upstream Routing Update

**Type:** Configuration Update
**Effort:** 1 MD
**Source:** ms-gw (Nginx – stays standalone)
**Status:** DONE

**Tasks:**
- [x] **Upstream routing update**:
  - Replace individual microservice upstreams with consolidated unit targets:
    - `ms-auth` → `engine-core:8081`
    - `ms-admin` → `engine-core:8081`
    - `ms-ing` → `engine-ingestor:8082`
    - `ms-sink-tbl`, `ms-qry`, `ms-dash`, `ms-srch`, `ms-tmpl` → `engine-data:8100`
    - `ms-lifecycle`, `ms-period`, `ms-form`, `ms-tmpl-pptx`, `ms-notif` → `engine-reporting:8105`
    - `ms-ext-snow` → `engine-integrations:8106`
    - `ms-gen-pptx`, `ms-mcp` → `processor-generators:8111`
  - engine-orchestrator upstream unchanged (stays `engine-orchestrator`)
- [x] **Rate limiting review**:
  - Verify rate limit zones still apply correctly per endpoint group
  - Adjust connection pool sizes for fewer upstream targets
- [x] **ForwardAuth update**:
  - Auth validation endpoint updated to `engine-core:8081/api/auth/validate`
- [x] **Health check endpoints**:
  - Update Nginx health checks to target consolidated `/actuator/health` endpoints
  - Remove health checks for decommissioned individual services
- [x] **CORS & headers**:
  - Verify CORS configuration still correct with consolidated origins
  - Ensure `X-Forwarded-*` headers propagated correctly
- [x] **Tests**:
  - Nginx config validation (`nginx -t`)
  - Integration test: all frontend routes reach correct consolidated backend
  - Verify rate limiting still enforced per-endpoint

**AC:**
- [x] All frontend REST calls routed to correct consolidated units via Nginx
- [x] ForwardAuth works with `engine-core` auth endpoint
- [x] Rate limiting functional per endpoint group
- [x] No 502/504 errors from stale upstream references
- [x] Nginx config passes syntax validation

---
