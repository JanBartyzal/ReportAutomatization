# P10-W1-004: Communication Contract Audit Report

**Date:** 2026-03-13
**Auditor:** Claude Opus (automated)
**Scope:** Communication contracts per project_charter.md v4.0 §2.1

---

## Summary

| Category | Compliant | Violations | Total |
|----------|-----------|------------|-------|
| Protocol compliance | 14 | 2 | 16 |
| Proto/gRPC consistency | 15 | 3 | 18 |
| OpenAPI consistency | 12 | 0 | 12 |
| TypeScript types vs API | 2 | 6 | 8+(partial) |
| Dapr PubSub topics | 5 | 13 | 18 |
| Frontend communication | 5 | 0 | 5 |
| **Overall** | **53** | **24** | **77** |

---

## 1. Protocol Compliance Matrix

### Charter Rules
- **Internal (service-to-service):** Dapr gRPC ONLY
- **Edge (frontend-facing):** REST ONLY via API Gateway
- **engine-orchestrator:** Internal only, no REST exposed externally
- **Atomizers/Sinks:** gRPC only, called exclusively via engine-orchestrator
- **Frontend:** Communicates ONLY with API Gateway

### Communication Path Audit

| Route | Expected | Actual | Status |
|-------|----------|--------|--------|
| Frontend → API Gateway | REST/HTTPS | REST via axios + JWT Bearer | **COMPLIANT** |
| GW → engine-core (auth) | REST | REST via nginx proxy | **COMPLIANT** |
| GW → engine-ingestor | REST | REST via nginx proxy | **COMPLIANT** |
| GW → engine-data (query/dash/search) | REST | REST via nginx proxy | **COMPLIANT** |
| GW → engine-reporting | REST | REST via nginx proxy | **COMPLIANT** |
| GW → engine-integrations | REST | REST via nginx proxy | **COMPLIANT** |
| **GW → ms-orch** | **NONE (internal only)** | **REST via `/api/orch/` + `/api/` fallback** | **VIOLATION** |
| ms-orch → atomizers | Dapr gRPC | daprClient.invokeMethod() | **COMPLIANT** |
| ms-orch → sinks (write) | Dapr gRPC | daprClient.invokeMethod() | **COMPLIANT** |
| ms-orch → generators | Dapr gRPC | daprClient.invokeMethod() | **COMPLIANT** |
| ms-orch → reporting | Dapr gRPC | daprClient.invokeMethod() | **COMPLIANT** |
| Ingestor → ms-orch | Dapr PubSub | Dapr PubSub (file-uploaded) | **COMPLIANT** |
| ms-orch → notifications | Dapr PubSub | daprClient.publishEvent() | **COMPLIANT** |
| **ms-admin → ms-orch** | **Dapr gRPC (DaprClient)** | **RestTemplate via Dapr HTTP sidecar** | **MINOR VIOLATION** |
| Sinks (tbl/doc/log) | gRPC only | @GrpcService, no @RestController | **COMPLIANT** |
| Atomizers (all) | gRPC only | gRPC servicers, no REST | **COMPLIANT** |

### Violation Details

#### VIOLATION 1: Orchestrator exposed externally (CRITICAL)
- **Location:** `apps/engine/microservices/units/ms-gw/nginx.conf`
- **Line 209:** `location /api/orch/` → `engine_orchestrator`
- **Line 627:** `location /api/` catch-all → `engine_orchestrator`
- **Impact:** Any unauthenticated `/api/orch/*` request reaches the orchestrator. The catch-all `/api/` means ALL unmatched API paths reach the orchestrator.
- **Remediation:** Remove `/api/orch/` location block. Change `/api/` catch-all to return 404.

#### VIOLATION 2: ms-admin uses RestTemplate instead of DaprClient (MINOR)
- **Location:** `ms-admin/src/.../FailedJobService.java:49-56`
- **Impact:** Bypasses DaprClient SDK; uses raw HTTP through Dapr sidecar. Functionally works but inconsistent.
- **Remediation:** Replace RestTemplate call with `daprClient.invokeMethod()`.

---

## 2. Service Protocol Inventory

### REST Controllers (should expose REST)

| Service | Controllers | Charter Edge? | Status |
|---------|------------|---------------|--------|
| engine-core/auth | AuthController | YES | COMPLIANT |
| engine-core/admin | AdminController, PromotionController | YES | COMPLIANT |
| engine-core/batch | BatchController | YES | COMPLIANT |
| engine-core/versioning | VersionController | YES | COMPLIANT |
| engine-core/audit | AuditController | YES | COMPLIANT |
| engine-data/query | FileQueryController, FormQueryController, ComparisonQueryController | YES | COMPLIANT |
| engine-data/dashboard | DashboardController, ComparisonController, DashboardGenerateController | YES | COMPLIANT |
| engine-data/search | SearchController | YES | COMPLIANT |
| engine-ingestor | UploadController, FileController | YES | COMPLIANT |
| engine-reporting/* | ReportController, PeriodController, FormController, etc. | YES | COMPLIANT |
| engine-integrations | ScheduleController, IntegrationController, DistributionController | YES | COMPLIANT |

### REST Controllers (should NOT expose REST)

| Service | Controllers | Status |
|---------|------------|--------|
| ms-orch | FileUploadedSubscriber, ReportStatusSubscriber, PptxGenerationSubscriber | **ACCEPTABLE** — these are Dapr PubSub callback endpoints (HTTP POST from Dapr sidecar) |

### gRPC Services (should expose gRPC)

| Service | gRPC Service | Status |
|---------|-------------|--------|
| ms-orch | OrchestratorGrpcService | COMPLIANT |
| sink-tbl | TableSinkGrpcService | COMPLIANT |
| sink-doc | DocumentSinkGrpcService | COMPLIANT |
| sink-log | LogSinkGrpcService | COMPLIANT |
| query | ReportDataGrpcService | COMPLIANT |
| template | TemplateMappingGrpcService | COMPLIANT |
| scanner | ScannerGrpcService | COMPLIANT |
| All Python atomizers | gRPC servicers | COMPLIANT |
| processor-generators | PptxGeneratorService, ExcelGeneratorService | COMPLIANT |

---

## 3. Proto/gRPC Consistency

| Proto File | Service | RPCs | Implementation | Status |
|-----------|---------|------|---------------|--------|
| atomizer/v1/ai.proto | AiGatewayService | 3 | ms-atm-ai (Python) | **MATCH** |
| atomizer/v1/csv.proto | CsvAtomizerService | 1 | ms-atm-csv (Python) | **MATCH** |
| atomizer/v1/excel.proto | ExcelAtomizerService | 3 | ms-atm-xls (Python) | **MATCH** |
| atomizer/v1/pdf.proto | PdfAtomizerService | 1 | ms-atm-pdf (Python) | **MATCH** |
| atomizer/v1/pptx.proto | PptxAtomizerService | 4 | ms-atm-pptx (Python) | **MATCH** |
| generator/v1/pptx_generator.proto | PptxGeneratorService | 2 | ms-gen-pptx (Python) | **MATCH** |
| generator/v1/excel_generator.proto | ExcelGeneratorService | 2 | ms-gen-xls (Python) | **MISMATCH** — impl uses `GenerateReport`/`BatchGenerate` vs proto `GenerateExcel`/`BatchGenerateExcel` |
| generator/v1/report_data.proto | ReportDataService | 2 | ms-qry (Java) | **MATCH** |
| orchestrator/v1/orchestrator.proto | OrchestratorService | 6 | ms-orch (Java) | **MATCH** (note: `stepsJson` vs `repeated steps` field mismatch) |
| scanner/v1/scanner.proto | ScannerService | 2 | ms-scan (Java) | **MATCH** |
| sink/v1/table.proto | TableSinkService | 3 | ms-sink-tbl (Java) | **MATCH** |
| sink/v1/document.proto | DocumentSinkService | 2 | ms-sink-doc (Java) | **MATCH** |
| sink/v1/log.proto | LogSinkService | 2 | ms-sink-log (Java) | **MATCH** |
| template/v1/template.proto | TemplateMappingService | 3 | ms-tmpl (Java) | **MATCH** |
| integration/v1/servicenow.proto | ServiceNowIntegrationService | 4 | **MISSING** — no gRPC impl | **MISSING** |
| integration/v1/smart_persistence.proto | SmartPersistenceService | 4 | **MISSING** — no gRPC impl | **MISSING** |
| common/v1/*.proto | (messages only) | — | N/A | N/A |
| lifecycle/v1/lifecycle.proto | (events only) | — | N/A (PubSub) | N/A |
| notification/v1/notification.proto | (events only) | — | N/A (PubSub) | N/A |

### Issues

1. **MISSING: ServiceNow gRPC** — Proto defines 4 RPCs but ms-ext-snow uses REST only. Either implement gRPC or remove proto.
2. **MISSING: Smart Persistence gRPC** — Proto defines 4 RPCs but ms-admin uses REST only. Same choice needed.
3. **MISMATCH: Excel generator** — RPC method names in code don't match proto definitions.
4. **MISMATCH: Orchestrator** — `WorkflowStatusResponse` uses `stepsJson` string in code but proto has `repeated WorkflowStep steps`.

---

## 4. OpenAPI Spec Consistency

All 12 OpenAPI specs in `docs/api/` have matching controller implementations:

| Spec | Controllers | Status |
|------|-----------|--------|
| ms-admin-openapi.yaml | AdminController, PromotionController | **MATCH** |
| ms-audit-openapi.yaml | AuditController | **MATCH** |
| ms-auth-openapi.yaml | AuthController | **MATCH** |
| ms-dash-openapi.yaml | DashboardController, ComparisonController, DashboardGenerateController | **MATCH** |
| ms-form-openapi.yaml | FormController, FormResponseController, FormExcelController, FormAssignmentController | **MATCH** |
| ms-ing-openapi.yaml | UploadController, FileController | **MATCH** |
| ms-lifecycle-openapi.yaml | ReportController | **MATCH** |
| ms-notif-openapi.yaml | NotificationController, NotificationSettingsController | **MATCH** |
| ms-period-openapi.yaml | PeriodController | **MATCH** |
| ms-qry-openapi.yaml | FileQueryController, FormQueryController, ComparisonQueryController | **MATCH** |
| ms-srch-openapi.yaml | SearchController | **MATCH** |
| ms-ver-openapi.yaml | VersionController | **MATCH** |

---

## 5. TypeScript Types vs API Specs

| Type File | Status | Issues |
|-----------|--------|--------|
| auth.ts | **MISMATCH** | Extra `COMPANY_ADMIN` role not in OpenAPI |
| files.ts | **MISMATCH** | `WorkflowStatus` schema differs structurally |
| notifications.ts | **MATCH** | — |
| periods.ts | **MATCH** | — |
| reports.ts | **MISMATCH** | TS has extra `COMPLETED` status, `scope`, `locked` fields |
| dashboard.ts | **PARTIAL** | TS superset with extra comparison types |
| forms.ts | **MISMATCH** | `FormResponse.data` shape differs |
| admin.ts | **MISMATCH** | Field naming differences (`id` vs `job_id`) |
| query.ts | **MISMATCH** | `SlideContent`, `TableData` schemas differ |

**6/9 TypeScript type files have structural mismatches** with OpenAPI specs. The TS types appear to reflect actual implementation rather than the documented API spec. Either the specs or the types need updating.

---

## 6. Dapr PubSub Topic Audit

### Documented Topics (TOPICS.md) vs Actual

| Topic | TOPICS.md | Subscription YAML | Code Publisher | Code Subscriber | Status |
|-------|-----------|-------------------|---------------|-----------------|--------|
| `file-uploaded` | YES | YES | ms-ing | ms-orch | **MATCH** |
| `processing-completed` | YES | YES | ms-orch | ms-notif | **PARTIAL** (no @Topic annotation) |
| `report.status_changed` | YES | YES | ms-lifecycle | ms-orch, ms-notif | **MATCH** |
| `report.data_locked` | YES | YES | ms-lifecycle | ms-sink-tbl | **PARTIAL** (no @Topic) |
| `notify` | YES | YES | ms-orch, ms-period | ms-notif | **MATCH** |
| `version.created` | NO | NO | ms-ver | ? | **UNDOCUMENTED, NO SUB** |
| `version.edit_on_locked` | NO | NO | ms-ver | ? | **UNDOCUMENTED, NO SUB** |
| `data-stored` | NO | NO | ? | ms-qry | **UNDOCUMENTED, NO SUB** |
| `form.response.submitted` | NO | NO | ms-form | ? | **UNDOCUMENTED, NO SUB** |
| `report.local_released` | NO | NO | ms-lifecycle | ? | **UNDOCUMENTED, NO SUB** |
| `pptx.generation_requested` | NO | NO | ms-orch | ms-orch | **UNDOCUMENTED, NO SUB** |
| `pptx.generation_completed` | NO | NO | ms-orch | ? | **UNDOCUMENTED, NO SUB** |
| `snow.sync.*` | NO | YES | (commented out) | ms-notif | **DEAD CONFIG** |
| `promotion.candidate.detected` | NO | YES | (commented out) | ms-notif | **DEAD CONFIG** |

### Legacy Topics (from prior project — should be removed)

- `plan.created`, `plan.updated`, `approval.action`, `cost.recalculated`, `policy.changed`, `billing.synced` — in `cache-invalidation-subscription.yaml`
- `iac-parse-requested`, `iac-parse-completed` — in `iac-parser-subscription.yaml`

**TOPICS.md documents only 5 of 18+ actual topics.** 7 topics published in code have no subscription YAML. Legacy subscription files reference services from a prior project.

---

## 7. Frontend Communication Check

| Check | Status | Evidence |
|-------|--------|---------|
| All calls via centralized axios instance | **PASS** | `src/api/axios.ts` — single instance with interceptors |
| JWT attached to every request | **PASS** | Request interceptor calls `acquireTokenSilent` |
| No direct service URLs (all through gateway) | **PASS** | `VITE_API_BASE_URL ?? '/api/v1'` — relative path |
| SSE via gateway | **PASS** | `/api/v1/notifications/stream` |
| No hardcoded production URLs | **PASS** | Only dev fallbacks (`localhost:8000`, `localhost:3000`) |

---

## Critical Findings

| # | Severity | Finding | Remediation |
|---|----------|---------|-------------|
| 1 | **CRITICAL** | Orchestrator exposed externally via nginx `/api/orch/` and `/api/` catch-all | Remove both routes; return 404 for unmatched paths |
| 2 | **HIGH** | ServiceNow proto (4 RPCs) has no gRPC implementation | Implement gRPC servicer or remove proto |
| 3 | **HIGH** | Smart Persistence proto (4 RPCs) has no gRPC implementation | Implement gRPC servicer or remove proto |
| 4 | **HIGH** | 7 Dapr topics published without subscription YAML | Add declarative subscriptions or document programmatic pattern |
| 5 | **MEDIUM** | Excel generator RPC names mismatch proto | Align Python method names with proto |
| 6 | **MEDIUM** | 6/9 TS type files mismatch OpenAPI specs | Regenerate types from OpenAPI or update specs |
| 7 | **MEDIUM** | TOPICS.md documents only 5/18+ topics | Update documentation |
| 8 | **MEDIUM** | ms-admin uses RestTemplate instead of DaprClient | Replace with DaprClient.invokeMethod() |
| 9 | **LOW** | Legacy Dapr subscriptions from prior project | Remove `cache-invalidation-subscription.yaml` and `iac-parser-subscription.yaml` |
| 10 | **LOW** | Dead `processor_generators` upstream in nginx | Remove unused upstream block |
