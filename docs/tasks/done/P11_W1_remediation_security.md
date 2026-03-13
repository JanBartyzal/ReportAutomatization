# P11 – Wave 1: Security Hardening & Critical Gap Remediation (Opus)

**Phase:** P11 – Audit Remediation
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~19 MD
**Depends on:** P10-W1 (audit findings)
**Source:** `docs/audit/AUDIT_SUMMARY.md`, `docs/tasks/P10_W2_remediation.md`

> Security-critical fixes, complex business logic changes, and protocol compliance remediation identified by P10-W1 audit.

---

## P11-W1-001: API Gateway ForwardAuth + Security Headers

**Type:** Security / Infrastructure
**Effort:** 2 MD
**Priority:** CRITICAL
**Audit Ref:** P10-W2-001, P10-W2-003, P10-W2-004, P10-W2-005, P10-W2-017

**Context:** The API gateway has NO authentication validation. Requests pass through to backend services without JWT check. Additionally, CORS reflects any origin, no CSP/security headers exist, orchestrator is exposed externally, and burst values don't match spec.

**Tasks:**
- [ ] Add `auth_request` directive pointing to `engine-core` auth verify endpoint
  - `auth_request /internal/auth/verify;` on all protected `location` blocks
  - Health/actuator endpoints remain unprotected
  - Map `auth_request` response: 401 (no token), 403 (insufficient perms)
- [ ] Remove `/api/orch/` location block (line 209)
- [ ] Change `/api/` catch-all (line 627) to `return 404`
- [ ] Replace `$http_origin` reflection with `map $http_origin` whitelist:
  ```
  map $http_origin $cors_origin {
      default "";
      "~^https://.*\.company\.cz$" $http_origin;
      "http://localhost:3000" $http_origin;
      "http://localhost:5173" $http_origin;
  }
  ```
- [ ] Add security headers to all responses:
  - `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'`
  - `X-Frame-Options: DENY`
  - `X-Content-Type-Options: nosniff`
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
  - `X-XSS-Protection: 1; mode=block`
- [ ] Fix burst values: upload burst → 20, API burst → 20 (per charter)
- [ ] Remove dead `processor_generators` upstream block

**Files:**
- `apps/engine/microservices/units/ms-gw/nginx.conf`

**AC:**
- [ ] Unauthenticated request returns 401
- [ ] Insufficient permissions returns 403
- [ ] `/api/orch/*` returns 404
- [ ] `/api/unknown-path` returns 404
- [ ] Non-whitelisted origin gets no CORS headers
- [ ] All 5 security headers present on every response
- [ ] Upload burst=20, API burst=20
- [ ] All legitimate API routes still work

---

## P11-W1-002: Fix ClamAV Scan Ordering + VBA Macro Removal

**Type:** Security / Pipeline
**Effort:** 3 MD
**Priority:** CRITICAL + HIGH
**Audit Ref:** P10-W2-002, P10-W2-015

**Context:** Files are uploaded to Blob Storage BEFORE antivirus scan. Additionally, VBA macro removal is a stub with a TODO comment.

**Tasks:**
- [ ] Restructure `UploadService` pipeline:
  1. MIME validation (existing)
  2. **ClamAV scan** (moved before blob upload)
  3. **VBA macro removal** (new — Apache POI for .xlsm/.pptm)
  4. Blob upload (moved after scan)
  5. Persist metadata + trigger orchestrator
- [ ] Implement actual VBA macro removal using Apache POI:
  - For `.xlsm`: Remove VBA project using `OPCPackage`, save as sanitized `.xlsx`
  - For `.pptm`: Remove VBA project, save as sanitized `.pptx`
  - Add `macro_removed: true` flag to file metadata
- [ ] Add EICAR test file to `tests/fixtures/` for scan verification
- [ ] Update both consolidated (`engine-ingestor`) and microservice (`ms-ing`) code

**Files:**
- `apps/engine/engine-ingestor/ingestor/src/main/java/.../UploadService.java`
- `apps/engine/engine-ingestor/scanner/src/main/java/.../SecurityScannerService.java`
- `apps/engine/microservices/units/ms-ing/src/main/java/.../UploadService.java`
- `apps/engine/microservices/units/ms-scan/src/main/java/.../SecurityScannerService.java`

**AC:**
- [ ] ClamAV scan happens BEFORE blob upload
- [ ] EICAR test file returns 422 and is never written to blob
- [ ] .xlsm file has VBA macros stripped → stored as sanitized file
- [ ] .pptm file has VBA macros stripped → stored as sanitized file
- [ ] Clean file proceeds normally through pipeline

---

## P11-W1-003: Method-Level Authorization (@PreAuthorize)

**Type:** Security / Authorization
**Effort:** 5 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-007

**Context:** Zero `@PreAuthorize` or `@Secured` annotations exist in the entire codebase. Any authenticated user can access any endpoint regardless of role.

**Tasks:**
- [ ] Add `@EnableMethodSecurity` to each consolidated module's `SecurityConfig`
- [ ] Define role hierarchy in a shared base config:
  - `HOLDING_ADMIN` > `ADMIN` > `EDITOR` > `VIEWER`
  - `COMPANY_ADMIN` > `EDITOR`
- [ ] Annotate ALL controller methods with `@PreAuthorize`:

  **engine-core/auth:**
  - `AuthController.verify()` → permitAll (authentication check only)
  - `AuthController.me()` → `@PreAuthorize("isAuthenticated()")`
  - `AuthController.switchOrg()` → `@PreAuthorize("isAuthenticated()")`

  **engine-core/admin:**
  - `AdminController.*` → `@PreAuthorize("hasRole('ADMIN')")`
  - `PromotionController.*` → `@PreAuthorize("hasRole('ADMIN')")`

  **engine-core/batch:**
  - `BatchController.list/get()` → `@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','HOLDING_ADMIN')")`
  - `BatchController.create/update/delete()` → `@PreAuthorize("hasAnyRole('EDITOR','ADMIN')")`

  **engine-core/versioning:**
  - `VersionController.list/get/diff()` → `@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','HOLDING_ADMIN')")`

  **engine-core/audit:**
  - `AuditController.*` → `@PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")`

  **engine-data/query+dashboard+search:**
  - All read endpoints → `@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','HOLDING_ADMIN')")`
  - Dashboard CRUD → `@PreAuthorize("hasAnyRole('EDITOR','ADMIN')")`

  **engine-ingestor:**
  - `UploadController.upload()` → `@PreAuthorize("hasAnyRole('EDITOR','ADMIN')")`
  - `FileController.list/get()` → `@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','HOLDING_ADMIN')")`

  **engine-reporting/lifecycle:**
  - `ReportController.list/get/matrix()` → `@PreAuthorize("hasAnyRole('VIEWER','EDITOR','ADMIN','HOLDING_ADMIN')")`
  - `ReportController.submit()` → `@PreAuthorize("hasAnyRole('EDITOR','ADMIN')")`
  - `ReportController.approve/reject/bulkApprove/bulkReject()` → `@PreAuthorize("hasAnyRole('ADMIN','HOLDING_ADMIN')")`

  **engine-reporting/period:**
  - Read endpoints → Viewer+
  - CRUD endpoints → Editor+
  - Clone → Admin+

  **engine-reporting/form:**
  - Read endpoints → Viewer+
  - Response CRUD → Editor+
  - Form definition CRUD → Admin+

  **engine-reporting/notification:**
  - Own notifications → authenticated
  - Settings → authenticated (own settings only)

  **engine-reporting/pptx-template:**
  - Read → Viewer+
  - Upload/manage → Admin+

  **engine-integrations:**
  - All → Admin+

- [ ] Ensure JWT token contains `roles` claim mapped from Azure Entra ID groups
- [ ] Add integration test verifying Viewer cannot POST

**Files:**
- All `SecurityConfig.java` (6 modules — add `@EnableMethodSecurity`)
- All `*Controller.java` (~30 controllers)
- `packages/java-base/java-base-security/` (shared role constants)

**AC:**
- [ ] `@EnableMethodSecurity` present in all 6 Java module SecurityConfigs
- [ ] Every public controller method has `@PreAuthorize`
- [ ] Viewer cannot POST/PUT/DELETE (returns 403)
- [ ] Editor cannot access other org's data
- [ ] HoldingAdmin can view cross-org + approve/reject
- [ ] Admin has full access to own org
- [ ] Unauthenticated user gets 401 (via ForwardAuth, not @PreAuthorize)

---

## P11-W1-004: RLS Policies for Unprotected Tables

**Type:** Security / Database
**Effort:** 3 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-006

**Context:** ~29 tables lack RLS policies. 3 tables (batches, batch_files, periods) have `ENABLE ROW LEVEL SECURITY` but no `CREATE POLICY`, effectively denying all access to non-superusers.

**Tasks:**
- [ ] Create Flyway migration for **engine-data** adding RLS to:
  - `parsed_tables` (org_id from joined files table)
  - `documents` (org_id from joined files table)
  - `document_embeddings` (via documents join)
  - `processing_logs` (org_id column)
  - `mapping_templates` (org_id or global)
  - `mapping_rules` (via mapping_templates)
  - `mapping_history` (org_id)
  - `mapping_usage_tracking` (org_id)
  - `promoted_tables_registry` (org_id)

- [ ] Create Flyway migration for **engine-reporting** adding RLS to:
  - `reports` (org_id)
  - `report_status_history` (via reports join)
  - `submission_checklists` (via reports join)
  - `period_org_assignments` (org_id)
  - `form_versions` (via forms join)
  - `form_fields` (via forms join)
  - `form_field_values` (via form_responses join)
  - `form_field_comments` (via form_responses join)
  - `form_assignments` (org_id)
  - `template_versions` (via pptx_templates join)
  - `template_placeholders` (via pptx_templates join)
  - `placeholder_mappings` (via pptx_templates join)
  - `notifications` (user_id + org_id)
  - `notification_settings` (user_id)
  - `feature_flags` (global — restrict to admin role)

- [ ] Create Flyway migration for **engine-core** adding CREATE POLICY to:
  - `batches` (org_id — currently ENABLE without POLICY)
  - `batch_files` (via batches join)
  - `periods` (org_id — currently ENABLE without POLICY)
  - `promotion_candidates` (org_id)
  - `role_audit_log` (admin-only access)

- [ ] Create Flyway migration for **ms-orch** adding RLS to:
  - `workflow_history` (org_id from file context)
  - `failed_jobs` (org_id from file context)

- [ ] All policies use pattern: `CREATE POLICY {table}_org_isolation ON {table} USING (org_id = current_setting('app.current_org_id')::text)`
- [ ] Add `FORCE ROW LEVEL SECURITY` on tables with application-user grants

**Files:**
- `apps/engine/engine-data/app/src/main/resources/db/migration/` (new V*__ file)
- `apps/engine/engine-reporting/app/src/main/resources/db/migration/` (new V*__ file)
- `apps/engine/engine-core/app/src/main/resources/db/migration/` (new V*__ file)
- `apps/engine/microservices/units/ms-orch/src/main/resources/db/migration/` (new V*__ file)

**AC:**
- [ ] All 29 previously unprotected tables now have RLS + policy
- [ ] `batches`, `batch_files`, `periods` have CREATE POLICY (not just ENABLE)
- [ ] Cross-tenant query test: user A cannot see user B's reports/notifications/documents
- [ ] Application still functions correctly with RLS active

---

## P11-W1-005: Exponential Backoff + Batch Model Refactor

**Type:** Business Logic
**Effort:** 3 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-018, P10-W2-019

**Context:** Orchestrator uses flat 1000ms retry (spec requires 1s/5s/30s exponential). Batch model still centers on `batch_id` instead of `period_id`.

**Tasks:**
- [ ] **Exponential backoff:**
  - Modify `WorkflowService` retry logic to use exponential delays: attempt 1 → 1s, attempt 2 → 5s, attempt 3 → 30s
  - Make delays configurable via `application.yml`: `orch.retry.delays: 1000,5000,30000`
  - Log retry attempt number and delay

- [ ] **Batch model refactor:**
  - Add `period_id UUID REFERENCES periods(id)` to `batch_files` table (new migration)
  - Update `BatchEntity` and `BatchFileEntity` to use `periodId` instead of generic `period` string
  - Deprecate `batch_id` as grouping key; use `period_id` as primary FK
  - Update `BatchController` and `BatchService` to accept/return `period_id`

**Files:**
- `apps/engine/microservices/units/ms-orch/src/main/java/.../WorkflowService.java`
- `apps/engine/microservices/units/ms-orch/src/main/resources/application.yml`
- `apps/engine/engine-core/batch/src/main/java/.../BatchEntity.java`
- `apps/engine/engine-core/batch/src/main/java/.../BatchFileEntity.java`
- `apps/engine/engine-core/batch/src/main/java/.../BatchService.java`
- `apps/engine/engine-core/batch/src/main/java/.../BatchController.java`
- New Flyway migration for batch_files period_id FK

**AC:**
- [ ] Retry delays are 1s, 5s, 30s (configurable)
- [ ] `batch_files` has `period_id` FK
- [ ] API accepts/returns `period_id` for batch operations

---

## P11-W1-006: Missing gRPC Services for Integration Protos

**Type:** Protocol Compliance
**Effort:** 3 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-025, P10-W2-024

**Context:** `servicenow.proto` (4 RPCs) and `smart_persistence.proto` (4 RPCs) have no gRPC implementations. Excel generator RPC names mismatch proto. Orchestrator `stepsJson` doesn't match proto `repeated steps`.

**Tasks:**
- [ ] **ServiceNow gRPC service:**
  - Create `ServiceNowGrpcService.java` in `engine-integrations/servicenow/`
  - Implement 4 RPCs: `FetchTableData`, `TestConnection`, `TriggerSync`, `GetSyncStatus`
  - Delegate to existing `ServiceNowClient` and `SyncJobService`
  - Register as `@GrpcService`

- [ ] **Smart Persistence gRPC service:**
  - Create `SmartPersistenceGrpcService.java` in `engine-core/admin/`
  - Implement 4 RPCs: `GetPromotionCandidates`, `ApprovePromotion`, `GetRoutingInfo`, `MigrateData`
  - Delegate to existing `PromotionDetectionService` and `PromotionApprovalService`

- [ ] **Excel generator RPC name fix:**
  - Rename `GenerateReport` → `GenerateExcel` in `ms-gen-xls/src/service/generator_service.py`
  - Rename `BatchGenerate` → `BatchGenerateExcel`
  - Regenerate proto stubs if needed

- [ ] **Orchestrator steps field fix:**
  - Replace `builder.setStepsJson(...)` with proper `repeated WorkflowStep steps` in `OrchestratorGrpcService.getWorkflowStatus()`

**Files:**
- `apps/engine/engine-integrations/servicenow/src/main/java/.../ServiceNowGrpcService.java` (new)
- `apps/engine/engine-core/admin/src/main/java/.../SmartPersistenceGrpcService.java` (new)
- `apps/processor/microservices/units/ms-gen-xls/src/service/generator_service.py`
- `apps/engine/microservices/units/ms-orch/src/main/java/.../OrchestratorGrpcService.java`

**AC:**
- [ ] `ServiceNowGrpcService` implements all 4 proto RPCs
- [ ] `SmartPersistenceGrpcService` implements all 4 proto RPCs
- [ ] Excel generator method names match proto exactly
- [ ] Orchestrator `getWorkflowStatus` returns `repeated WorkflowStep` not JSON string
- [ ] All gRPC services callable via Dapr service invocation

---

## Summary

| Task | Effort | Priority |
|------|--------|----------|
| P11-W1-001: Gateway ForwardAuth + Security Headers | 2 MD | CRITICAL |
| P11-W1-002: ClamAV Ordering + VBA Macro Removal | 3 MD | CRITICAL |
| P11-W1-003: @PreAuthorize Method-Level Auth | 5 MD | HIGH |
| P11-W1-004: RLS Policies (29 tables) | 3 MD | HIGH |
| P11-W1-005: Exponential Backoff + Batch Refactor | 3 MD | MEDIUM |
| P11-W1-006: Missing gRPC Services + Proto Fixes | 3 MD | MEDIUM |
| **Total** | **19 MD** | |
