# P11 – Wave 2: Observability, Quality & Testing (Sonnet)

**Phase:** P11 – Audit Remediation
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~18 MD
**Depends on:** P11-W1 (security fixes should be in place)
**Source:** `docs/audit/nfr-compliance-report.md`, `docs/audit/dod-compliance-report.md`

> Observability, code quality tooling, unit testing, and protocol consistency fixes.

---

## P11-W2-001: OpenTelemetry Distributed Tracing

**Type:** Observability
**Effort:** 3 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-008

**Context:** Zero OpenTelemetry dependencies in Java backend. Frontend has @opentelemetry packages but backend has none. Distributed tracing across microservices is impossible.

**Tasks:**
- [ ] Add `opentelemetry-javaagent` to all 6 Java modules:
  - Add OTEL Java agent as a JVM arg in Dockerfiles: `-javaagent:/app/opentelemetry-javaagent.jar`
  - Download agent in Dockerfile build stage
- [ ] Configure OTEL exporter in each `application.yml`:
  ```yaml
  otel:
    exporter:
      otlp:
        endpoint: http://otel-collector:4317
    resource:
      attributes:
        service.name: ${spring.application.name}
    traces:
      sampler: parentbased_always_on
  ```
- [ ] Add `opentelemetry-api` + `opentelemetry-sdk` to all Python `pyproject.toml`
- [ ] Configure Python OTEL in each processor's `main.py`:
  - `TracerProvider` with OTLP exporter
  - gRPC interceptor for automatic span creation
- [ ] Update `infra/docker/docker-compose.observability.yml`:
  - Fix stale service names (ms-auth → engine-core, etc.)
  - Ensure OTEL collector receives from all consolidated services
- [ ] Add custom spans for key operations:
  - Java: `@WithSpan` on `UploadService.upload()`, `WorkflowService.executeStep()`
  - Python: span around each atomizer `Extract*` and generator `Generate*` method
- [ ] Add `file_id`, `org_id`, `user_id` as span attributes for searchability

**Files:**
- All 6 Java `pom.xml` files (OTEL agent dependency)
- All 6 Java `Dockerfile` files (agent download + JVM arg)
- All 6 Java `application.yml` files (OTEL config)
- All Python `pyproject.toml` (2 files)
- All Python `main.py` (OTEL init)
- `infra/docker/docker-compose.observability.yml`

**AC:**
- [ ] End-to-end trace visible in Tempo: FE → GW → engine-core → ms-orch → atomizer → sink
- [ ] Traces searchable by `file_id`, `user_id`, `org_id`
- [ ] Each service appears as distinct service in trace view
- [ ] Python atomizers show spans for extraction operations

---

## P11-W2-002: Structured JSON Logging

**Type:** Observability
**Effort:** 1 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-026

**Context:** All Java services use default Spring Boot console logging. No structured JSON output for log aggregation (Loki/ELK).

**Tasks:**
- [ ] Add `logstash-logback-encoder` dependency to `packages/java-base/pom.xml`
- [ ] Create shared `logback-spring.xml` in `packages/java-base/`:
  ```xml
  <configuration>
    <springProfile name="!dev">
      <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
          <includeMdcKeyName>traceId</includeMdcKeyName>
          <includeMdcKeyName>spanId</includeMdcKeyName>
          <includeMdcKeyName>fileId</includeMdcKeyName>
          <includeMdcKeyName>orgId</includeMdcKeyName>
        </encoder>
      </appender>
      <root level="INFO"><appender-ref ref="JSON"/></root>
    </springProfile>
    <springProfile name="dev">
      <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
      </appender>
      <root level="INFO"><appender-ref ref="CONSOLE"/></root>
    </springProfile>
  </configuration>
  ```
- [ ] Copy or reference `logback-spring.xml` in each consolidated module's `src/main/resources/`
- [ ] Configure Python services to use `python-json-logger` for JSON output

**Files:**
- `packages/java-base/pom.xml` (add logstash-logback-encoder)
- `packages/java-base/src/main/resources/logback-spring.xml` (new)
- All 6 Java module `src/main/resources/` (copy or reference)
- Python `pyproject.toml` files (add python-json-logger)

**AC:**
- [ ] Non-dev Java logs are JSON with traceId, spanId
- [ ] Dev profile retains human-readable console output
- [ ] Python services output JSON logs

---

## P11-W2-003: Checkstyle + ESLint Configuration

**Type:** Code Quality
**Effort:** 3 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-009, P10-W2-010

**Context:** No Checkstyle for Java (0 config). No ESLint for TypeScript/React (0 config). Only Python Ruff is configured.

**Tasks:**
- [ ] **Checkstyle (Java):**
  - Create `packages/java-base/checkstyle.xml` based on Google Java Style with project customizations:
    - Line length: 120 chars
    - Indentation: 4 spaces
    - Javadoc: not required on private methods
  - Add `maven-checkstyle-plugin` to `packages/java-base/pom.xml` (inherited by all modules)
  - Run `mvn checkstyle:check` and fix blocking violations (if any)

- [ ] **ESLint (Frontend):**
  - Create `apps/frontend/eslint.config.js` with flat config:
    - `@typescript-eslint/recommended`
    - `eslint-plugin-react-hooks`
    - `eslint-plugin-react-refresh`
  - Add devDependencies to `package.json`
  - Add `"lint": "eslint src/"` script
  - Run and fix blocking violations

**Files:**
- `packages/java-base/checkstyle.xml` (new)
- `packages/java-base/pom.xml` (add plugin)
- `apps/frontend/eslint.config.js` (new)
- `apps/frontend/package.json` (add deps + script)

**AC:**
- [ ] `mvn checkstyle:check` passes on all Java modules
- [ ] `npm run lint` passes on frontend
- [ ] CI can gate on both linters

---

## P11-W2-004: Unit Tests for Consolidated Java Modules

**Type:** Testing
**Effort:** 8 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-011

**Context:** 5 consolidated engine modules + ms-orch have 0 test files. Existing tests are only in legacy microservice dirs (18 files across ms-dash, ms-qry, ms-tmpl, ms-ver, ms-audit, ms-tmpl-pptx, ms-ext-snow, ms-sink-tbl).

**Tasks:**
- [ ] **engine-core** (auth, admin, batch, versioning, audit) — 5 test files:
  - `AuthControllerTest.java` — verify, me, switchOrg + invalid token
  - `AdminServiceTest.java` — assignRole, revokeRole + duplicate role
  - `BatchServiceTest.java` — create, list by period + empty period
  - `VersionServiceTest.java` — createVersion, getDiff + null entity
  - `AuditExportServiceTest.java` — exportCSV, exportJSON + empty logs

- [ ] **engine-data** (sinks, query, dashboard, search, template) — 5 test files:
  - `TableSinkServiceTest.java` — bulkInsert, deleteByFileId + empty batch
  - `CacheServiceTest.java` — get, set, evict + TTL expiry (mock Redis)
  - `DashboardAggregationTest.java` — aggregate by type + empty data
  - `SearchServiceTest.java` — fullText, vector + no results
  - `TemplateMappingServiceTest.java` — applyMapping, suggestMapping + unknown template

- [ ] **engine-ingestor** (ingestor, scanner) — 3 test files:
  - `UploadServiceTest.java` — upload flow + oversized file + invalid MIME
  - `MimeValidationServiceTest.java` — valid types + magic byte mismatch
  - `ScannerServiceTest.java` — clean file + infected file (mock ClamAV)

- [ ] **engine-reporting** (lifecycle, period, form, pptx-tmpl, notification) — 5 test files:
  - `ReportServiceTest.java` — submit, approve, reject + incomplete checklist
  - `PeriodServiceTest.java` — create, clone, deadlineCheck + past deadline
  - `FormValidationServiceTest.java` — validate number, text, conditional + invalid regex
  - `PptxTemplateServiceTest.java` — upload, extractPlaceholders + invalid template
  - `NotificationServiceTest.java` — send, pushSSE + disabled notification type

- [ ] **engine-integrations** (servicenow) — 2 test files:
  - `ServiceNowClientTest.java` — fetchData, OAuth2 token + connection failure
  - `SyncJobServiceTest.java` — sync, incrementalDelta + no new records

- [ ] **ms-orch** — 3 test files:
  - `WorkflowServiceTest.java` — startWorkflow, executeStep + retry exhaustion
  - `SagaOrchestratorTest.java` — execute + compensate on failure
  - `IdempotencyServiceTest.java` — check + duplicate request (mock Redis)

- [ ] Mock external dependencies:
  - Dapr: `@MockBean DaprClient`
  - Redis: `@MockBean StringRedisTemplate`
  - Blob Storage: mock `BlobStorageService`
  - ClamAV: mock `SecurityScanService`

**Files:**
- 23 new `*Test.java` files across 6 modules in `src/test/java/`

**AC:**
- [ ] Each consolidated module has ≥3 test files
- [ ] Each test covers happy path + at least 2 edge cases (null input, error scenario)
- [ ] `mvn test` passes on all modules
- [ ] External services (Dapr, Redis, Blob, ClamAV) are mocked

---

## P11-W2-005: Atomizer Error Handling (422 Equivalent)

**Type:** Error Handling
**Effort:** 1 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-014

**Context:** Python atomizer errors propagate as gRPC `INTERNAL` (500 equivalent). Spec requires `INVALID_ARGUMENT` (422 equivalent) for validation errors.

**Tasks:**
- [ ] Create shared error handler decorator in `packages/python-base/`:
  ```python
  def grpc_error_handler(func):
      @wraps(func)
      def wrapper(self, request, context):
          try:
              return func(self, request, context)
          except ValidationError as e:
              context.abort(grpc.StatusCode.INVALID_ARGUMENT, str(e))
          except FileNotFoundError as e:
              context.abort(grpc.StatusCode.NOT_FOUND, str(e))
          except Exception as e:
              logger.exception("Unexpected error")
              context.abort(grpc.StatusCode.INTERNAL, "Internal processing error")
      return wrapper
  ```
- [ ] Apply decorator to all atomizer gRPC service methods (PPTX, XLS, PDF, CSV, AI, CLN)
- [ ] Define `ValidationError` for known input validation failures
- [ ] Add specific error codes for: unsupported format, corrupted file, empty content, encoding failure

**Files:**
- `packages/python-base/src/common/grpc_errors.py` (new)
- All `*_service.py` files in processor-atomizers (6 files)

**AC:**
- [ ] Validation errors return `INVALID_ARGUMENT` with descriptive message
- [ ] File-not-found returns `NOT_FOUND`
- [ ] Unknown errors return `INTERNAL` with generic message (no stack trace leak)
- [ ] Each atomizer's known error paths return appropriate status codes

---

## P11-W2-006: Flyway Undo Migrations + Dapr Topic Subscriptions

**Type:** Infrastructure / Config
**Effort:** 2 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-020, P10-W2-028

**Context:** Zero Flyway undo migrations exist (42+ version migrations). 7 Dapr topics published in code lack declarative subscription YAMLs.

**Tasks:**
- [ ] **Flyway undo migrations** — create `U*__` for the latest 5 V-migrations per module:
  - engine-core: U5_0_2, U5_0_1, U4_0_1, U3_0_1, U2_0_2
  - engine-data: U7_0_2, U7_0_1, U6_0_1, U5_0_2, U5_0_1
  - engine-ingestor: U1_0_1
  - engine-integrations: U1_0_3, U1_0_2, U1_0_1
  - engine-reporting: U5_0_002, U5_0_001, U4_0_003, U4_0_002, U4_0_001
  - Each undo migration should `DROP TABLE IF EXISTS` or reverse `ALTER TABLE`

- [ ] **Dapr subscription YAMLs** for undocumented topics:
  - `version-subscription.yaml`: `version.created` → ms-notif, `version.edit_on_locked` → ms-lifecycle
  - `data-subscription.yaml`: `data-stored` → ms-qry
  - `form-subscription.yaml`: `form.response.submitted` → ms-notif
  - `lifecycle-subscription.yaml`: `report.local_released` → ms-notif
  - `pptx-subscription.yaml`: `pptx.generation_requested` → ms-orch, `pptx.generation_completed` → ms-notif

**Files:**
- New `U*__*.sql` files in each module's `db/migration/` dir (~20 files)
- `infra/dapr/components/` (5 new subscription YAMLs)

**AC:**
- [ ] Each module's latest 5 migrations have corresponding undo scripts
- [ ] `flyway undo` successfully rolls back latest migration per module
- [ ] All 7 undocumented topics have declarative subscription YAMLs
- [ ] Subscription routes match actual `@Topic` annotations in code

---

## Summary

| Task | Effort | Priority |
|------|--------|----------|
| P11-W2-001: OpenTelemetry Distributed Tracing | 3 MD | HIGH |
| P11-W2-002: Structured JSON Logging | 1 MD | MEDIUM |
| P11-W2-003: Checkstyle + ESLint Configuration | 3 MD | HIGH |
| P11-W2-004: Unit Tests for Java Modules | 8 MD | HIGH |
| P11-W2-005: Atomizer Error Handling (422) | 1 MD | HIGH |
| P11-W2-006: Flyway Undo + Dapr Subscriptions | 2 MD | MEDIUM |
| **Total** | **18 MD** | |
