# P10 – Wave 2: Remediation Tasks

**Phase:** P10 – Technical Audit Remediation
**Generated from:** P10-W1 Technical Audit (2026-03-13)
**Total Effort:** ~50 MD
**Priority:** CRITICAL and HIGH items first

---

## CRITICAL Priority (blocks production)

### P10-W2-001: Add ForwardAuth to API Gateway
**Service:** ms-gw (nginx)
**Effort:** 1 MD
**Description:** Add `auth_request` directive to nginx.conf pointing to engine-core auth verify endpoint. Ensure 401 for missing token, 403 for insufficient permissions.
**Files:**
- `apps/engine/microservices/units/ms-gw/nginx.conf`
**AC:**
- [ ] `auth_request /api/auth/verify` present on all protected locations
- [ ] Unauthenticated request returns 401
- [ ] Insufficient permissions returns 403
- [ ] Health/actuator endpoints remain unprotected

### P10-W2-002: Fix ClamAV Scan Ordering
**Service:** engine-ingestor
**Effort:** 1 MD
**Description:** Restructure `UploadService` pipeline: MIME validate → ClamAV scan → blob upload. Currently file is uploaded to blob before scan.
**Files:**
- `apps/engine/engine-ingestor/ingestor/src/main/java/.../UploadService.java`
- `apps/engine/microservices/units/ms-ing/src/main/java/.../UploadService.java`
**AC:**
- [ ] ClamAV scan happens before blob upload
- [ ] EICAR test file returns 422
- [ ] Clean file proceeds to blob upload

### P10-W2-003: Remove Orchestrator External Exposure
**Service:** ms-gw (nginx)
**Effort:** 0.5 MD
**Description:** Remove `/api/orch/` location block and change `/api/` catch-all to return 404 instead of proxying to orchestrator.
**Files:**
- `apps/engine/microservices/units/ms-gw/nginx.conf` (lines 209, 627)
**AC:**
- [ ] `/api/orch/*` returns 404
- [ ] `/api/unknown-path` returns 404 (not proxied to orch)
- [ ] All legitimate API routes still work

---

## HIGH Priority (significant gaps)

### P10-W2-004: Add CORS Whitelist
**Service:** ms-gw (nginx)
**Effort:** 0.5 MD
**Description:** Replace `$http_origin` reflection with `map $http_origin` block allowing only `*.company.cz` and `localhost:3000/5173`.
**Files:**
- `apps/engine/microservices/units/ms-gw/nginx.conf`
**AC:**
- [ ] Only whitelisted origins get CORS headers
- [ ] Non-whitelisted origins get no CORS headers

### P10-W2-005: Add Security Headers
**Service:** ms-gw (nginx)
**Effort:** 0.5 MD
**Description:** Add `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security` headers.
**Files:**
- `apps/engine/microservices/units/ms-gw/nginx.conf`
**AC:**
- [ ] All 4 security headers present on responses

### P10-W2-006: Add RLS Policies to Unprotected Tables
**Service:** engine-data, engine-reporting, ms-orch
**Effort:** 3 MD
**Description:** Add `CREATE POLICY` for ~29 tables lacking RLS. Priority: `reports`, `documents`, `processing_logs`, `notifications`, `workflow_history`, `failed_jobs`. Also add policies to `batches`, `batch_files`, `periods` which have ENABLE but no POLICY.
**Files:**
- New Flyway migrations in each affected engine module
**AC:**
- [ ] All org-scoped tables have `CREATE POLICY` with org isolation
- [ ] Cross-tenant query returns empty (not other org's data)

### P10-W2-007: Add @PreAuthorize Method-Level Authorization
**Service:** All Java consolidated modules
**Effort:** 5 MD
**Description:** Add `@EnableMethodSecurity` and `@PreAuthorize` annotations to all controller methods. Map roles: Admin (full), Editor (CRUD own org), Viewer (read-only), HoldingAdmin (cross-org read + approve).
**Files:**
- All `*Controller.java` files + SecurityConfig classes
**AC:**
- [ ] Viewer cannot POST/PUT/DELETE
- [ ] Editor cannot access other org's data
- [ ] HoldingAdmin can view cross-org + approve/reject

### P10-W2-008: Add OpenTelemetry Distributed Tracing
**Service:** All Java modules + Python modules
**Effort:** 3 MD
**Description:** Add `opentelemetry-javaagent` to all Java services. Configure OTEL collector endpoint. Add `opentelemetry-api` to Python services. Ensure trace propagation through Dapr.
**Files:**
- All `pom.xml` files (add OTEL dependencies)
- All `application.yml` (add OTEL config)
- All `pyproject.toml` (add OTEL packages)
- `infra/docker/docker-compose.observability.yml` (update service names)
**AC:**
- [ ] End-to-end trace visible: FE → GW → service → ORCH → ATM → Sink
- [ ] Traces searchable by file_id, user_id in Tempo/Grafana

### P10-W2-009: Add ESLint Configuration
**Service:** frontend
**Effort:** 2 MD
**Description:** Add ESLint with TypeScript/React plugins. Configure rules per STANDARDS.md. Fix blocking violations.
**Files:**
- `apps/frontend/eslint.config.js` (new)
- `apps/frontend/package.json` (add devDependencies)
**AC:**
- [ ] `npm run lint` passes without errors
- [ ] TypeScript strict rules enforced

### P10-W2-010: Add Checkstyle Configuration
**Service:** All Java modules
**Effort:** 1 MD
**Description:** Add `checkstyle.xml` with Google Java Style (customized per STANDARDS.md). Configure Maven plugin.
**Files:**
- `packages/java-base/checkstyle.xml` (new)
- All `pom.xml` (add maven-checkstyle-plugin)
**AC:**
- [ ] `mvn checkstyle:check` passes without errors

### P10-W2-011: Write Unit Tests for Consolidated Java Modules
**Service:** engine-core, engine-data, engine-ingestor, engine-integrations, engine-reporting, ms-orch
**Effort:** 8 MD
**Description:** Each consolidated module currently has 0 test files. Write tests covering happy path + 2 edge cases per service. Mock Dapr and external dependencies.
**Files:**
- New `*Test.java` files in each module's `src/test/java/`
**AC:**
- [ ] Each module has at least 3 test files
- [ ] Happy path + null input + error case covered
- [ ] All tests pass

### P10-W2-012: Write Frontend Tests
**Service:** frontend
**Effort:** 5 MD
**Description:** Set up Vitest + React Testing Library. Write component tests for critical pages: UploadPage, DashboardEditorPage, ReportsPage, FormsPage. Write hook tests for useFiles, useAuth, useGeneration.
**Files:**
- `apps/frontend/vitest.config.ts` (new)
- New `*.test.tsx` files per component
**AC:**
- [ ] `npm run test` passes
- [ ] Key pages and hooks have tests

### P10-W2-013: Create READMEs for All Deployable Units
**Service:** All 10 units
**Effort:** 3 MD
**Description:** Each consolidated unit needs README.md with: purpose, Dapr app-id, Mermaid sequence diagram, API overview.
**Files:**
- 10 new `README.md` files at unit roots
**AC:**
- [ ] Each README has purpose, Dapr app-id, Mermaid diagram

### P10-W2-014: Implement Atomizer Error Handling (422)
**Service:** processor-atomizers
**Effort:** 1 MD
**Description:** Wrap validation errors in gRPC `INVALID_ARGUMENT` (equivalent to 422). Ensure uncaught exceptions don't propagate as `INTERNAL` (500).
**Files:**
- All `*_service.py` in processor-atomizers
**AC:**
- [ ] Validation errors return INVALID_ARGUMENT with detail message
- [ ] No unhandled INTERNAL errors for known error conditions

### P10-W2-015: Implement VBA Macro Removal
**Service:** engine-ingestor (scanner)
**Effort:** 2 MD
**Description:** Replace stub in `SecurityScannerService.java` with actual Apache POI macro removal for .xlsm and .pptm files.
**Files:**
- `apps/engine/engine-ingestor/scanner/src/main/java/.../SecurityScannerService.java`
**AC:**
- [ ] .xlsm files have macros stripped before storage
- [ ] .pptm files have macros stripped before storage

### P10-W2-016: Schema Mapping Editor UI
**Service:** frontend
**Effort:** 3 MD
**Description:** Replace placeholder in `SchemaMappingPage.tsx` with functional column mapping editor. Connect to backend `suggestMapping` API.
**Files:**
- `apps/frontend/src/pages/SchemaMappingPage.tsx`
- `apps/frontend/src/pages/ExcelImportPage.tsx` (connect mock to real API)
**AC:**
- [ ] User can map source columns to target fields
- [ ] Auto-suggest shows backend suggestions with confidence
- [ ] Mapping saved and usable in import flow

---

## MEDIUM Priority (should fix)

### P10-W2-017: Fix Rate Limit Burst Values
**Service:** ms-gw
**Effort:** 0.25 MD
**Description:** Change upload burst from 10 to 20; change API burst from 50 to 20 per charter spec.
**File:** `apps/engine/microservices/units/ms-gw/nginx.conf`

### P10-W2-018: Implement Exponential Backoff in Orchestrator
**Service:** ms-orch
**Effort:** 1 MD
**Description:** Replace flat 1000ms retry delay with exponential backoff (1s, 5s, 30s) as per FS04 spec.
**File:** `ms-orch/src/main/java/.../WorkflowService.java`, `application.yml`

### P10-W2-019: Refactor Batch Model to Use period_id
**Service:** engine-core/batch
**Effort:** 2 MD
**Description:** Replace `batch_id` FK in `batch_files` with `period_id`. Align model with FS08 spec.

### P10-W2-020: Add Flyway Undo Migrations
**Service:** All Java modules
**Effort:** 3 MD
**Description:** Create `U*__` undo migration for each `V*__` migration. At minimum, cover latest 5 migrations per module.

### P10-W2-021: Update TOPICS.md
**Service:** infra/dapr
**Effort:** 0.5 MD
**Description:** Document all 18+ active PubSub topics with publisher/subscriber info.

### P10-W2-022: Clean Legacy Dapr Configs
**Service:** infra/dapr
**Effort:** 0.5 MD
**Description:** Remove `cache-invalidation-subscription.yaml`, `iac-parser-subscription.yaml`, and 8+ legacy cron jobs from prior project.

### P10-W2-023: Fix TypeScript Types vs OpenAPI Specs
**Service:** frontend, packages/types
**Effort:** 2 MD
**Description:** Align 6 mismatched TypeScript type files with current OpenAPI specs or update specs to match.

### P10-W2-024: Fix Proto/Code Mismatches
**Service:** ms-gen-xls, ms-orch
**Effort:** 1 MD
**Description:** Align Excel generator RPC names with proto (`GenerateExcel`/`BatchGenerateExcel`). Fix orchestrator `stepsJson` vs `repeated steps` field.

### P10-W2-025: Add Missing gRPC Services or Remove Protos
**Service:** engine-integrations, engine-core/admin
**Effort:** 2 MD
**Description:** Either implement gRPC servicers for `servicenow.proto` (4 RPCs) and `smart_persistence.proto` (4 RPCs), or remove the proto files.

### P10-W2-026: Add Structured JSON Logging
**Service:** All Java modules
**Effort:** 1 MD
**Description:** Add `logback-spring.xml` with JSON encoder (Logstash Logback Encoder). Ensure structured output for all services.

### P10-W2-027: Create test-result.md Template
**Service:** All modules
**Effort:** 1 MD
**Description:** Add `test-result.md` to each deployable unit using `docs/template-test-result.md` format. Populate with current coverage.

### P10-W2-028: Add Dapr Subscription YAMLs for Undocumented Topics
**Service:** infra/dapr
**Effort:** 1 MD
**Description:** Add declarative subscription YAMLs for 7 topics that only have programmatic `@Topic` annotations: `version.created`, `version.edit_on_locked`, `data-stored`, `form.response.submitted`, `report.local_released`, `pptx.generation_requested`, `pptx.generation_completed`.

---

## LOW Priority (nice to have)

### P10-W2-029: Auto-Close Period (REVIEWING → CLOSED)
**Service:** engine-reporting/period
**Effort:** 0.5 MD

### P10-W2-030: Dashboard Date/Org Filters in Editor
**Service:** frontend
**Effort:** 1 MD

### P10-W2-031: PPTX Generation Timeout Guard
**Service:** processor-generators
**Effort:** 0.5 MD

### P10-W2-032: Form Builder Drag & Drop UI
**Service:** frontend
**Effort:** 2 MD

### P10-W2-033: Update Observability Compose for Consolidated Topology
**Service:** infra/docker
**Effort:** 0.5 MD

### P10-W2-034: Remove Hardcoded Passwords from Docker Compose
**Service:** infra/docker
**Effort:** 1 MD
**Description:** Replace all plaintext passwords with `${VAR}` references. Create `.env.example` for infra.

### P10-W2-035: Fix Missing Dapr app-id in 3 Services
**Service:** engine-core, engine-integrations, ms-orch
**Effort:** 0.25 MD
**Description:** Add `dapr.app-id` to `application.yml` for consistency.

---

## Effort Summary

| Priority | Count | Effort |
|----------|-------|--------|
| CRITICAL | 3 | 2.5 MD |
| HIGH | 13 | 34.5 MD |
| MEDIUM | 12 | 12.25 MD |
| LOW | 7 | 5.75 MD |
| **Total** | **35** | **~55 MD** |

## Suggested Sprint Plan

| Sprint | Focus | Tasks | Effort |
|--------|-------|-------|--------|
| **P10-W2** (week 1) | Security & Gateway | P10-W2-001 through -007 | ~11 MD |
| **P10-W2** (week 2) | Observability & Quality | P10-W2-008 through -010, -014, -022 | ~8 MD |
| **P10-W3** (week 1) | Testing | P10-W2-011, -012 | 13 MD |
| **P10-W3** (week 2) | Documentation & Cleanup | P10-W2-013, -021, -023, -024, -027 | ~8 MD |
| **P11** | Frontend & Remaining | P10-W2-015, -016, -025, -028, LOW items | ~15 MD |
