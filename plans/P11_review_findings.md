# P11 Implementation Review - Missing Items

## Summary of Review

| Task | Status | Notes |
|------|--------|-------|
| P11-W1-001: nginx auth_request + security | ✅ DONE | auth_request, CORS whitelist, security headers |
| P11-W1-002: ClamAV scan ordering | ✅ DONE | Verified in engine-ingestor |
| P11-W1-003: @PreAuthorize | ✅ DONE | Security configs have @EnableMethodSecurity |
| P11-W1-004: RLS policies | ✅ DONE | V8_0_1, V5_0_1, V6_0_1 migrations exist |
| P11-W1-005: Exponential backoff | ❌ NOT DONE | Not in application.yml |
| P11-W1-006: gRPC Services | ⚠️ PARTIAL | OrchestratorGrpcService exists, but ServiceNowGrpcService and SmartPersistenceGrpcService missing |

| Task | Status | Notes |
|------|--------|-------|
| P11-W2-001: OpenTelemetry | ⚠️ PARTIAL | Java agent in engine-core, Python deps exist, need verify all services |
| P11-W2-002: JSON Logging | ✅ DONE | logstash-logback-encoder in pom.xml, logback-spring.xml exists |
| P11-W2-003: Checkstyle + ESLint | ✅ DONE | checkstyle.xml and eslint.config.js exist |
| P11-W2-004: Java Unit Tests | ❌ NOT DONE | No test files in any Java module |
| P11-W2-005: Atomizer Error Handling | ✅ DONE | grpc_errors.py exists |
| P11-W2-006: Flyway Undo + Dapr | ✅ DONE | U* migrations and subscription YAMLs exist |

| Task | Status | Notes |
|------|--------|-------|
| P11-W3-001: Clean Legacy Dapr | ✅ DONE | TOPICS.md updated |
| P11-W3-002: Dapr app-id | ⚠️ PARTIAL | Need verify all services have dapr.app-id |
| P11-W3-003: Hardcoded Passwords | ✅ DONE | .env.example exists |
| P11-W3-004: READMEs | ✅ DONE | All 10 units have README.md |
| P11-W3-005: test-result.md | ✅ DONE | All 10 units have test-result.md |
| P11-W3-006: Auto-Close Period | ⚠️ PARTIAL | reviewDeadline field exists, need verify DeadlineService auto-close logic |

| Task | Status | Notes |
|------|--------|-------|
| P11-W4-001: Frontend Tests | ⚠️ PARTIAL | vitest.config.ts exists, tests exist, need verify all 12 required |
| P11-W4-002: Schema Mapping | ✅ DONE | SchemaMappingPage.tsx implemented |
| P11-W4-003: TypeScript Alignment | ❌ NOT DONE | Need verify type alignment |
| P11-W4-004: Dashboard Filters | ⚠️ PARTIAL | Need verify implementation |

---

## Missing Implementation Items

### HIGH PRIORITY

#### 1. P11-W1-005: Exponential Backoff
- **Files needed:**
  - `apps/engine/engine-orchestration/src/main/resources/application.yml` - Add retry delays
  - Update WorkflowService to use exponential backoff

#### 2. P11-W1-006: Missing gRPC Services
- **Files to create:**
  - `apps/engine/engine-integrations/servicenow/src/main/java/.../ServiceNowGrpcService.java`
  - `apps/engine/engine-core/admin/src/main/java/.../SmartPersistenceGrpcService.java`

#### 3. P11-W2-004: Java Unit Tests (23 test files needed)
- engine-core: 5 test files (AuthControllerTest, AdminServiceTest, BatchServiceTest, VersionServiceTest, AuditExportServiceTest)
- engine-data: 5 test files (TableSinkServiceTest, CacheServiceTest, DashboardAggregationTest, SearchServiceTest, TemplateMappingServiceTest)
- engine-ingestor: 3 test files (UploadServiceTest, MimeValidationServiceTest, ScannerServiceTest)
- engine-reporting: 5 test files (ReportServiceTest, PeriodServiceTest, FormValidationServiceTest, PptxTemplateServiceTest, NotificationServiceTest)
- engine-integrations: 2 test files (ServiceNowClientTest, SyncJobServiceTest)
- engine-orchestration: 3 test files (WorkflowServiceTest, SagaOrchestratorTest, IdempotencyServiceTest)

#### 4. P11-W4-003: TypeScript Types Alignment
- Audit and align TS types with OpenAPI specs for:
  - auth.ts
  - files.ts
  - reports.ts
  - forms.ts
  - admin.ts
  - query.ts
  - dashboard.ts

### MEDIUM PRIORITY

#### 5. P11-W2-001: OpenTelemetry Verification
- Verify all 6 Java Dockerfiles have OTEL agent
- Verify all application.yml have OTEL config

#### 6. P11-W3-002: Dapr app-id Verification
- Verify all services have dapr.app-id in application.yml

#### 7. P11-W3-006: Auto-Close Period Logic
- Verify DeadlineService.checkPastDeadlines() auto-closes REVIEWING periods

#### 8. P11-W4-004: Dashboard Filters + Form DnD
- Verify DashboardEditorPage has date/org filters
- Verify FormBuilder has drag-and-drop

### LOW PRIORITY

#### 9. Frontend Test Coverage Verification
- Verify all 12 required test files exist:
  - 5 page tests: FilesPage, ReportsPage, DashboardListPage, PeriodsPage, TemplateListPage
  - 3 hook tests: useFiles, useAuth, useGeneration
  - 4 component tests: ChartWrapper, ErrorBoundary, GenerateButton, GenerationProgress
