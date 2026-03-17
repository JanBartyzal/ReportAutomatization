# P10 – Wave 1: Technical Audit – Compliance with Project Charter & DoD (Opus)

**Phase:** P10 – Technical Audit & Quality Gate
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~20 MD
**Depends on:** P8 (consolidation), P9 (style unification)

> Comprehensive audit verifying all implementations match project_charter.md specs, DoD criteria, and STANDARDS.md. Generates gap analysis with remediation tasks.

---

## P10-W1-001: Feature Set Completeness Audit (FS01–FS24)

**Type:** Audit
**Effort:** 6 MD

**Tasks:**
- [ ] **FS01 – Infrastructure & Core**:
  - [ ] API Gateway routing verified: `/api/auth`, `/api/upload`, `/api/query` correct
  - [ ] Rate limiting: 100 req/s API, 10 req/s Auth/Upload, burst 20
  - [ ] ForwardAuth returns 401 (no token), 403 (insufficient permissions)
  - [ ] CORS whitelist: `https://*.company.cz` + `localhost:3000` (dev)
  - [ ] RBAC roles implemented: Admin, Editor, Viewer, HoldingAdmin
  - [ ] Organizational hierarchy: Holding → Company → Division (3 levels)
  - [ ] KeyVault secrets accessible at startup
  - [ ] `tilt up` starts topology within 5 minutes
- [ ] **FS02 – File Ingestor**:
  - [ ] Streaming upload (not in-memory) to Blob Storage
  - [ ] MIME allowlist: `.pptx`, `.xlsx`, `.pdf`, `.csv` + magic number validation
  - [ ] ClamAV scan BEFORE blob storage (EICAR test → 422)
  - [ ] VBA macro removal from Office docs
  - [ ] Blob naming: `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}`
  - [ ] Max file size: 50 MB (PPTX/XLSX/CSV), 100 MB (PDF)
  - [ ] `upload_purpose: PARSE` vs `FORM_IMPORT` discrimination
  - [ ] Orchestrator event within 1s of successful upload
- [ ] **FS03 – Atomizers**:
  - [ ] PPTX: structure, content, slide image (PNG 1280×720 via LibreOffice Headless)
  - [ ] MetaTable confidence threshold > 0.85
  - [ ] Excel: per-sheet JSON, partial success state
  - [ ] PDF: text vs scanned detection, Tesseract OCR
  - [ ] CSV: auto-detect delimiter, encoding, header
  - [ ] AI: LiteLLM integration, token quota with 429
  - [ ] Cleanup: hourly cron for temp files >24h
  - [ ] All atomizers return structured JSON or artifact_url, never inline binary
  - [ ] Error → 422 with detail, never 500
- [ ] **FS04 – Orchestrator**:
  - [ ] Spring State Machine workflow engine
  - [ ] Type-Safe Contracts (no loose JSON objects)
  - [ ] Saga Pattern with compensating actions
  - [ ] Exponential backoff: 3 retry (1s, 5s, 30s) → failed_jobs
  - [ ] Idempotence: `file_id + step_hash` in Redis
  - [ ] Dead Letter Queue: `failed_jobs` table with admin UI
- [ ] **FS05 – Sinks**:
  - [ ] gRPC-only access (no REST endpoints)
  - [ ] BulkInsert + DeleteByFileId (compensating action)
  - [ ] Flyway migrations with RLS policies
  - [ ] `form_responses` table schema present
  - [ ] Document API: pgVector embeddings generated async
- [ ] **FS06 – Analytics & Query**:
  - [ ] engine-data:query: Redis caching TTL 5 min
  - [ ] engine-data:dashboard: Recharts + Nivo chart support
  - [ ] engine-data:dashboard: `source_type` flag (FORM/FILE)
  - [ ] engine-data:search: Full-text search + vector search
- [ ] **FS07 – Admin**:
  - [ ] Role management UI
  - [ ] API key management (bcrypt hashed)
  - [ ] Failed Jobs UI with "Reprocess" button
  - [ ] Form/template management admin section
- [ ] **FS08 – Batch Management**:
  - [ ] Batch grouping by period
  - [ ] RLS enforcement on PostgreSQL level
  - [ ] `period_id` used instead of generic `batch_id`
- [ ] **FS09 – Frontend SPA**:
  - [ ] MSAL Provider with silent token refresh
  - [ ] Drag & Drop upload with progress bar
  - [ ] React Query invalidation after upload
  - [ ] Real-time polling (3s interval)
- [ ] **FS10 – Excel Parsing**:
  - [ ] Per-sheet JSONB, partial success
  - [ ] JSONB records indistinguishable from PPTX data
- [ ] **FS11 – Dashboards**:
  - [ ] `is_public` flag, viewer restrictions
  - [ ] GROUP BY, ORDER BY, date/org filter from UI
  - [ ] Direct SQL editor for advanced users
- [ ] **FS12 – API & AI (MCP)**:
  - [ ] API key Bearer token access
  - [ ] On-Behalf-Of flow for AI
  - [ ] RLS enforced on AI queries
  - [ ] Monthly token quota, 429 on exceed
- [ ] **FS13 – Notifications**:
  - [ ] WebSocket/SSE push
  - [ ] SMTP email for critical events
  - [ ] Granular opt-in/opt-out per event type
  - [ ] New notification types: REPORT_SUBMITTED/APPROVED/REJECTED, DEADLINE_*
- [ ] **FS14 – Data Versioning**:
  - [ ] Every change creates new version (v1→v2)
  - [ ] Diff tool in UI showing changes between versions
- [ ] **FS15 – Schema Mapping**:
  - [ ] Column mapping editor UI
  - [ ] Learning from history (auto-suggest)
  - [ ] `POST /map/excel-to-form` endpoint
  - [ ] engine-orchestrator integration via gRPC
- [ ] **FS16 – Audit & Compliance**:
  - [ ] Immutable logs (INSERT only, no UPDATE/DELETE for app user)
  - [ ] Read access logging
  - [ ] AI prompt/response logging
  - [ ] CSV/JSON export
  - [ ] State transition audit
- [ ] **FS17 – Report Lifecycle**:
  - [ ] State machine: DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED
  - [ ] Submission checklist (100% before SUBMITTED)
  - [ ] Rejection with mandatory comment
  - [ ] HoldingAdmin matrix dashboard
  - [ ] Bulk approve/reject
  - [ ] Data locked after APPROVED
- [ ] **FS18 – PPTX Generation**:
  - [ ] Template upload with placeholder extraction
  - [ ] `{{variable}}`, `{{TABLE:}}`, `{{CHART:}}`
  - [ ] Template versioning
  - [ ] Generation < 60s for 20 slides
  - [ ] Missing data → `DATA MISSING` marker, not failure
  - [ ] Batch generation for multiple reports
- [ ] **FS19 – Form Builder**:
  - [ ] Drag & drop editor
  - [ ] Field types: text, number, percentage, date, dropdown, table, file_attachment
  - [ ] Validation rules: min/max, regex, conditional
  - [ ] Auto-save every 30s
  - [ ] Form versioning
  - [ ] Excel template export/import with metadata sheet
  - [ ] Import arbitrary Excel with schema mapping
  - [ ] `scope` and `owner_org_id` in data model
- [ ] **FS20 – Reporting Periods**:
  - [ ] Period creation with deadlines
  - [ ] States: OPEN → COLLECTING → REVIEWING → CLOSED
  - [ ] Auto-close after deadline
  - [ ] Reminder notifications: 7, 3, 1 day before
  - [ ] Completion tracking matrix
  - [ ] Period cloning
  - [ ] Basic as-is period comparison
- [ ] **FS21 – Local Scope**:
  - [ ] Local forms (scope: LOCAL)
  - [ ] CompanyAdmin role
  - [ ] Data release to holding
  - [ ] Local PPTX templates
  - [ ] Shared within holding scope
- [ ] **FS23 – Service-Now** (if P7 completed):
  - [ ] REST API connector with OAuth2/Basic auth
  - [ ] Scheduled sync with incremental delta
  - [ ] Excel report generation and distribution
- [ ] **FS24 – Smart Persistence** (if P7 completed):
  - [ ] Usage tracking on schema mappings
  - [ ] Promotion candidate detection
  - [ ] SQL schema proposal
  - [ ] Admin approval workflow
  - [ ] Transparent routing after promotion

**Output:** `docs/audit/fs-completeness-report.md` — gap list with severity (CRITICAL/HIGH/MEDIUM/LOW)

**AC:**
- [ ] Every FS requirement checked and documented
- [ ] Gaps categorized by severity
- [ ] Remediation tasks created for CRITICAL and HIGH gaps

---

## P10-W1-002: Non-Functional Requirements (NFR) Audit

**Type:** Audit
**Effort:** 4 MD

**Tasks:**
- [ ] **Performance**:
  - [ ] End-to-end file processing < 30s for 50-slide PPTX
  - [ ] Upload API latency < 2s for 20 MB file
  - [ ] Run performance test suite (`tests/performance/`)
  - [ ] Document actual measurements vs targets
- [ ] **Security**:
  - [ ] Zero-trust: every request has JWT token validation
  - [ ] No hardcoded secrets in codebase (git secret scan)
  - [ ] RLS cross-tenant test: verify user A cannot access user B data
  - [ ] 100% uploaded files scanned by ClamAV
  - [ ] OWASP Top 10 review:
    - [ ] SQL injection: parameterized queries everywhere
    - [ ] XSS: React auto-escaping + CSP headers
    - [ ] CSRF: token-based auth (no cookies for API)
    - [ ] Injection: no eval, no dynamic SQL without sanitization
- [ ] **Availability**:
  - [ ] Health check endpoints on all services
  - [ ] Graceful shutdown handling
  - [ ] Connection pool management (no leaks)
- [ ] **Scalability**:
  - [ ] Atomizer layer: verify horizontal scaling works
  - [ ] DB connection pooling appropriate for load
- [ ] **Observability**:
  - [ ] End-to-end trace: FE → GW → ORCH → ATM → Sink
  - [ ] Traces searchable by file_id, user_id, org_id
  - [ ] Structured JSON logging from all services
  - [ ] Prometheus metrics exposed from all services
  - [ ] Grafana dashboards for: error rate, queue depth, latency, DB connections
- [ ] **Code Quality**:
  - [ ] Linting passes: ESLint (TS), Black/Ruff (Python), Checkstyle (Java)
  - [ ] No dead code or commented-out blocks
  - [ ] Unit test coverage: happy path + 2 edge cases per new logic

**Output:** `docs/audit/nfr-compliance-report.md`

**AC:**
- [ ] All NFR measurements documented
- [ ] Security scan completed with zero CRITICAL findings
- [ ] Performance benchmarks match or documented deviation

---

## P10-W1-003: Definition of Done (DoD) Compliance Audit

**Type:** Audit
**Effort:** 4 MD

**Tasks:**
- [ ] **A. Code Quality & Integrity** — per service:
  - [ ] Linting passes without errors (all 3 linters)
  - [ ] No dead code (unused imports, commented blocks)
  - [ ] GraalVM Native Image compatibility (Java services)
  - [ ] No hardcoded secrets
  - [ ] Azure Entra ID token v2 in manifest
  - [ ] Axios interceptor silent token renewal
  - [ ] File upload triggers React Query invalidation
- [ ] **B. Documentation & Mermaid** — per service:
  - [ ] README.md with purpose and Dapr app-id
  - [ ] Mermaid sequence/flow diagrams in README
  - [ ] Complex logic has inline docs explaining "why"
- [ ] **C. Testing & Results** — per service:
  - [ ] Unit tests present with mocked Dapr/LiteLLM
  - [ ] `test-result.md` updated with latest coverage
  - [ ] Happy path + edge cases (null inputs, network failures) covered
- [ ] **Cross-service checks**:
  - [ ] All services have health check endpoints
  - [ ] All services emit OTEL traces
  - [ ] All services have Prometheus metrics
  - [ ] Flyway migrations have rollback scripts
  - [ ] OpenAPI specs match actual endpoints

**Output:** `docs/audit/dod-compliance-report.md` — service-by-service DoD checklist

**AC:**
- [ ] Every service checked against all DoD criteria
- [ ] Non-compliant items listed with remediation effort estimate
- [ ] Overall compliance percentage calculated

---

## P10-W1-004: Communication Contract Audit

**Type:** Audit
**Effort:** 3 MD

**Tasks:**
- [ ] **Protocol compliance** (per project charter §2.1):
  - [ ] Internal services use Dapr gRPC only (no internal REST)
  - [ ] Edge services expose REST only (router routes)
  - [ ] Binary data transferred as Blob URL references, never inline
  - [ ] Every request carries JWT in Authorization header
- [ ] **Proto/OpenAPI consistency**:
  - [ ] All `.proto` files in `packages/protos/` match implemented gRPC services
  - [ ] All REST endpoints documented in OpenAPI 3.0
  - [ ] TypeScript types in `packages/types/` match API responses
- [ ] **Dapr communication matrix** (per charter §11):
  - [ ] Verify each row in communication matrix is implemented
  - [ ] Verify no unauthorized direct service-to-service REST calls
  - [ ] Verify PubSub topic names match documentation
- [ ] **Frontend-to-backend**:
  - [ ] Frontend calls ONLY go through API Gateway (no direct service URLs)
  - [ ] All API calls use Axios with auth interceptor
  - [ ] React Query cache invalidation patterns correct

**Output:** `docs/audit/communication-audit-report.md`

**AC:**
- [ ] Every communication path verified against charter
- [ ] Protocol violations flagged
- [ ] Proto/API spec consistency confirmed

---

## P10-W1-005: Remediation Task Generation

**Type:** Planning
**Effort:** 3 MD

**Tasks:**
- [ ] **Aggregate audit findings**:
  - Collect all gaps from P10-W1-001 through P10-W1-004
  - Categorize: CRITICAL (blocks production), HIGH (significant gap), MEDIUM (should fix), LOW (nice to have)
- [ ] **Generate remediation tasks**:
  - Create task file `docs/tasks/P10_W2_remediation.md` with specific fix tasks
  - Each task: description, affected service, estimated effort, priority
  - Group by service/module for efficient execution
- [ ] **Summary dashboard**:
  - Feature Set completion: X/Y requirements met per FS
  - DoD compliance: X/Y criteria met per service
  - NFR compliance: pass/fail per category
  - Communication compliance: violations count
- [ ] **Executive summary**: `docs/audit/AUDIT_SUMMARY.md`
  - Overall platform readiness assessment
  - Top 10 risks
  - Recommended next steps

**Output:**
- `docs/audit/AUDIT_SUMMARY.md`
- `docs/tasks/P10_W2_remediation.md`

**AC:**
- [ ] All audit findings consolidated
- [ ] Remediation tasks prioritized and estimated
- [ ] Executive summary ready for stakeholder review

---
