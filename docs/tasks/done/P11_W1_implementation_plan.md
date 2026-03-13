# P11-W1 Implementation Plan: Security Hardening & Critical Gap Remediation

## Executive Summary

This document outlines the implementation plan for **P11-W1: Security Hardening & Critical Gap Remediation**, addressing audit findings from P10-W1. The task involves 6 major work items totaling ~19 MD effort.

---

## Current Project State Analysis

Based on codebase analysis:

| Task | Status | Notes |
|------|--------|-------|
| P11-W1-001 | **NOT STARTED** | nginx.conf missing auth_request, CORS whitelist, security headers |
| P11-W1-002 | **PARTIALLY DONE** | engine-ingestor has correct pipeline; need to verify ms-ing |
| P11-W1-003 | **MOSTLY DONE** | @EnableMethodSecurity in 5 configs; @PreAuthorize in many controllers |
| P11-W1-004 | **PARTIALLY DONE** | Many RLS policies exist; ~29 tables need checking |
| P11-W1-005 | **NOT STARTED** | Need exponential backoff; batch model uses batch_id |
| P11-W1-006 | **NOT STARTED** | Missing gRPC services |

---

## Implementation Details

### P11-W1-001: API Gateway ForwardAuth + Security Headers

**File:** `infra/docker/nginx.conf`

**Changes Required:**

1. **Add auth_request for ForwardAuth:**
   ```nginx
   # Add before proxy_pass in protected locations
   auth_request /api/auth/verify;
   auth_request_set $auth_status $upstream_status;
   
   # Error handling for 401/403
   error_page 401 = /auth-fail-401;
   error_page 403 = /auth-fail-403;
   ```

2. **Remove /api/orch/ block** (lines 213-238)

3. **Change /api/ catch-all to return 404** (line 670-697):
   ```nginx
   location /api/ {
       return 404 '{"error": "Not Found", "message": "API path not found"}';
   }
   ```

4. **Add CORS whitelist map** (in http block):
   ```nginx
   map $http_origin $cors_origin {
       default "";
       "~^https://.*\.company\.cz$" $http_origin;
       "http://localhost:3000" $http_origin;
       "http://localhost:5173" $http_origin;
   }
   ```

5. **Add security headers** (in server block):
   - `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'`
   - `X-Frame-Options: DENY`
   - `X-Content-Type-Options: nosniff`
   - `Strict-Transport-Security: max-age=31536000; includeSubDomains`
   - `X-XSS-Protection: 1; mode=block`

6. **Fix burst values:**
   - Upload: `burst=20` (currently 10)
   - API: `burst=20` (currently 50)

---

### P11-W1-002: Fix ClamAV Scan Ordering + VBA Macro Removal

**Status:** Already implemented in `engine-ingestor` ✓
- UploadService has correct pipeline: MIME → ClamAV → VBA removal → Blob
- SecurityScannerService has `removeVbaMacros()` implemented with Apache POI

**Remaining:**
- Verify `ms-ing` microservice has same pipeline
- Add EICAR test file to `tests/fixtures/`

---

### P11-W1-003: Method-Level Authorization (@PreAuthorize)

**Status:** Already implemented ✓
- @EnableMethodSecurity in 5 SecurityConfig files
- @PreAuthorize in all major controllers

**Remaining:**
- Verify AuthController.verify() has `permitAll`
- Add integration test for Viewer cannot POST

---

### P11-W1-004: RLS Policies for Unprotected Tables

**Files to create:**
- `apps/engine/engine-data/app/src/main/resources/db/migration/V8_0_1__qry_add_missing_rls.sql`
- `apps/engine/engine-reporting/app/src/main/resources/db/migration/V5_0_1__rpt_add_missing_rls.sql`
- `apps/engine/engine-core/app/src/main/resources/db/migration/V6_0_1__core_add_batch_rls.sql`

**Tables requiring RLS:**

**engine-data:**
- parsed_tables, documents, document_embeddings, processing_logs
- mapping_templates, mapping_rules, mapping_history, mapping_usage_tracking
- promoted_tables_registry

**engine-reporting:**
- reports, report_status_history, submission_checklists
- period_org_assignments
- form_versions, form_fields, form_field_values, form_field_comments
- form_assignments
- template_versions, template_placeholders, placeholder_mappings
- notifications, notification_settings, feature_flags

**engine-core:**
- batches, batch_files, periods (have ENABLE but need POLICY)
- promotion_candidates, role_audit_log

---

### P11-W1-005: Exponential Backoff + Batch Model Refactor

**Files:**
- `apps/engine/engine-orchestration/src/main/java/.../WorkflowService.java`
- `apps/engine/engine-orchestration/src/main/resources/application.yml`
- `apps/engine/engine-core/batch/src/main/java/.../BatchEntity.java`
- `apps/engine/engine-core/batch/src/main/java/.../BatchFileEntity.java`

**Changes:**

1. **Exponential backoff:**
   ```yaml
   orch:
     retry:
       delays: 1000,5000,30000  # 1s, 5s, 30s
   ```

2. **Batch model refactor:**
   - Add `period_id UUID REFERENCES periods(id)` to batch_files
   - Update BatchEntity and BatchFileEntity to use periodId
   - Update BatchController and BatchService APIs

---

### P11-W1-006: Missing gRPC Services for Integration Protos

**Files to create/modify:**
- `apps/engine/engine-integrations/servicenow/src/main/java/.../ServiceNowGrpcService.java` (new)
- `apps/engine/engine-core/admin/src/main/java/.../SmartPersistenceGrpcService.java` (new)
- `apps/processor/microservices/units/ms-gen-xls/src/service/generator_service.py`
- `apps/engine/engine-orchestration/src/main/java/.../OrchestratorGrpcService.java`

**ServiceNow gRPC RPCs:**
- FetchTableData
- TestConnection
- TriggerSync
- GetSyncStatus

**Smart Persistence gRPC RPCs:**
- GetPromotionCandidates
- ApprovePromotion
- GetRoutingInfo
- MigrateData

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| nginx config errors break gateway | HIGH | Test in dev environment first |
| RLS policies block legitimate access | MEDIUM | Use FORCE ROW LEVEL SECURITY only where appropriate |
| Exponential backoff increases latency | LOW | Expected behavior per spec |

---

## Testing Strategy

1. **P11-W1-001:** Manual curl tests for 401/403, browser CORS tests
2. **P11-W1-002:** Upload EICAR file, verify 422 response
3. **P11-W1-003:** Login as Viewer, attempt POST, verify 403
4. **P11-W1-004:** Cross-tenant query tests
5. **P11-W1-005:** Check logs for retry delays
6. **P11-W1-006:** gRPC client tests for each RPC
