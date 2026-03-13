# P11 – Wave 3: Configuration, Cleanup & Documentation (Haiku)

**Phase:** P11 – Audit Remediation
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~7 MD
**Depends on:** P11-W1 (security), P11-W2 (observability)
**Source:** `docs/audit/dod-compliance-report.md`, `docs/audit/communication-audit-report.md`

> Configuration fixes, legacy cleanup, documentation, and simple infrastructure tasks.

---

## P11-W3-001: Clean Legacy Dapr Configs + Update TOPICS.md

**Type:** Cleanup / Documentation
**Effort:** 1 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-021, P10-W2-022

**Tasks:**
- [ ] Delete legacy subscription files from prior CIM project:
  - `infra/dapr/components/cache-invalidation-subscription.yaml`
  - `infra/dapr/components/iac-parser-subscription.yaml`
- [ ] Delete legacy cron jobs not related to ReportPlatform:
  - `stripe-sync-cron.yaml`
  - `sandbox-cleanup-cron.yaml`
  - `sandbox-warning-cron.yaml`
  - `cron-price-scraping.yaml`
  - `price-monitoring-cron.yaml`
  - `telemetry-sync-cron.yaml`
  - `mv-refresh-cron.yaml`
  - `mv-refresh-cron-job.yaml`
- [ ] Keep only domain-relevant configs:
  - `retention-cron.yaml` (data retention — verify route exists)
  - `cron-monthly-snapshot.yaml` (verify route exists)
- [ ] Update `infra/dapr/TOPICS.md` with ALL active topics:

  ```markdown
  # Dapr PubSub Topics

  | Topic | Publisher | Subscriber(s) | Purpose |
  |-------|-----------|---------------|---------|
  | file-uploaded | engine-ingestor | ms-orch | Trigger file processing workflow |
  | processing-completed | ms-orch | engine-reporting/notif | Notify on workflow completion |
  | report.status_changed | engine-reporting/lifecycle | ms-orch, engine-reporting/notif | Report state transition |
  | report.data_locked | engine-reporting/lifecycle | engine-data/sink-tbl | Lock data after approval |
  | report.local_released | engine-reporting/lifecycle | engine-reporting/notif | Local scope data released |
  | notify | ms-orch, engine-reporting/period | engine-reporting/notif | Generic notification trigger |
  | version.created | engine-core/versioning | engine-reporting/notif | New version created |
  | version.edit_on_locked | engine-core/versioning | engine-reporting/lifecycle | Edit attempt on locked entity |
  | data-stored | engine-data/sink-tbl | engine-data/query | Cache invalidation on new data |
  | form.response.submitted | engine-reporting/form | engine-reporting/notif | Form response submitted |
  | pptx.generation_requested | ms-orch | ms-orch | Trigger PPTX generation workflow |
  | pptx.generation_completed | ms-orch | engine-reporting/notif | PPTX generation done |
  | snow.sync.completed | engine-integrations | engine-reporting/notif | ServiceNow sync done |
  | snow.sync.failed | engine-integrations | engine-reporting/notif | ServiceNow sync failed |
  | promotion.candidate.detected | engine-core/admin | engine-reporting/notif | Smart persistence candidate |
  | document-embedding | engine-data/sink-doc | (async processor) | Trigger embedding generation |
  ```

**Files:**
- `infra/dapr/components/` (delete 10 files, keep 2)
- `infra/dapr/TOPICS.md` (rewrite)

**AC:**
- [ ] No legacy CIM/Stripe/Sandbox/IaC configs remain
- [ ] TOPICS.md documents all 16+ active topics with publisher/subscriber
- [ ] `tilt up` still starts without errors (no missing config references)

---

## P11-W3-002: Fix Dapr app-id + Observability Compose Update

**Type:** Configuration
**Effort:** 0.75 MD
**Priority:** LOW
**Audit Ref:** P10-W2-035, P10-W2-033

**Tasks:**
- [ ] Add missing `dapr.app-id` to 3 service configs:
  - `apps/engine/engine-core/app/src/main/resources/application.yml` → `app-id: engine-core`
  - `apps/engine/engine-integrations/app/src/main/resources/application.yml` → `app-id: engine-integrations`
  - `apps/engine/microservices/units/ms-orch/src/main/resources/application.yml` → `app-id: ms-orch`

- [ ] Update `infra/docker/docker-compose.observability.yml`:
  - Replace old service name references with consolidated names
  - Ensure OTEL collector scrapes: engine-core, engine-data, engine-ingestor, engine-integrations, engine-reporting, ms-orch, processor-atomizers, processor-generators
  - Verify Grafana datasources point to correct Prometheus/Tempo/Loki

**Files:**
- 3 `application.yml` files
- `infra/docker/docker-compose.observability.yml`

**AC:**
- [ ] All 6 Java services have `dapr.app-id` in application.yml
- [ ] Observability stack uses correct consolidated service names
- [ ] `docker compose -f docker-compose.consolidated.yml -f docker-compose.observability.yml up` works

---

## P11-W3-003: Remove Hardcoded Passwords from Docker Compose

**Type:** Security / Configuration
**Effort:** 1 MD
**Priority:** LOW
**Audit Ref:** P10-W2-034

**Tasks:**
- [ ] Create `infra/docker/.env.example` with all required variables:
  ```
  # Database
  POSTGRES_PASSWORD=
  DB_PASSWORD=

  # Redis
  REDIS_PASSWORD=

  # Grafana
  GF_SECURITY_ADMIN_PASSWORD=

  # LiteLLM
  LITELLM_API_KEY=

  # JWT (dev only)
  JWT_SECRET=

  # Azure (optional for local dev)
  AZURE_OPENAI_API_KEY=
  ```
- [ ] Replace all plaintext passwords in `docker-compose.yml` with `${VAR}` references
- [ ] Replace all plaintext passwords in `docker-compose.consolidated.yml` with `${VAR}` references
- [ ] Replace all plaintext passwords in `docker-compose.observability.yml` with `${VAR}` references
- [ ] Create `infra/docker/.env` with dev defaults (add to `.gitignore`)
- [ ] Add `infra/docker/.env` to `.gitignore` if not already present

**Files:**
- `infra/docker/.env.example` (new)
- `infra/docker/.env` (new, gitignored)
- `infra/docker/docker-compose.yml`
- `infra/docker/docker-compose.consolidated.yml`
- `infra/docker/docker-compose.observability.yml`
- `.gitignore`

**AC:**
- [ ] Zero plaintext passwords in any docker-compose file
- [ ] `.env.example` documents all required variables
- [ ] `.env` with dev defaults is gitignored
- [ ] `docker compose up` still works with `.env` present

---

## P11-W3-004: Create READMEs for All Deployable Units

**Type:** Documentation
**Effort:** 3 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-013

**Tasks:**
Create README.md for each of 10 deployable units with this template:

```markdown
# {Service Name}

**Dapr App ID:** `{app-id}`
**Tech:** {Java 21 / Python 3.11 / React 18 / Nginx}
**Port:** {port}

## Purpose
{1-2 sentence description}

## Modules
{List of merged microservices, if consolidated}

## Architecture
```mermaid
{Sequence or flow diagram showing main business process}
```

## API
{REST endpoints if edge service, or gRPC services if internal}

## Configuration
{Key application.yml settings}

## Running
{How to start locally}
```

- [ ] `apps/engine/engine-core/README.md` — Auth + Admin + Batch + Versioning + Audit
- [ ] `apps/engine/engine-data/README.md` — Sinks + Query + Dashboard + Search + Template
- [ ] `apps/engine/engine-ingestor/README.md` — File Ingestor + ClamAV Scanner
- [ ] `apps/engine/engine-integrations/README.md` — ServiceNow Integration
- [ ] `apps/engine/engine-reporting/README.md` — Lifecycle + Period + Form + PPTX Template + Notification
- [ ] `apps/engine/microservices/units/ms-orch/README.md` — Orchestrator (State Machine + Saga)
- [ ] `apps/engine/microservices/units/ms-gw/README.md` — API Gateway (Nginx)
- [ ] `apps/processor/processor-atomizers/README.md` — PPTX/XLS/PDF/CSV/AI/Cleanup Atomizers
- [ ] `apps/processor/processor-generators/README.md` — PPTX/Excel Generators + MCP Server
- [ ] `apps/frontend/README.md` — React SPA
- [ ] Each README must include at least one Mermaid diagram
- [ ] Each README must list Dapr app-id

**Files:**
- 10 new `README.md` files

**AC:**
- [ ] All 10 units have README.md
- [ ] Each contains: purpose, Dapr app-id, Mermaid diagram, API/module list
- [ ] Mermaid diagrams render correctly in GitHub

---

## P11-W3-005: Create test-result.md for All Units

**Type:** Documentation
**Effort:** 1 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-027

**Tasks:**
- [ ] Read `docs/template-test-result.md` for format reference
- [ ] Create `test-result.md` in each deployable unit root with:
  - Test framework used
  - Current test count
  - Current coverage percentage (0% if no tests exist yet)
  - Date of last run
  - Known gaps
- [ ] Units to cover: engine-core, engine-data, engine-ingestor, engine-integrations, engine-reporting, ms-orch, ms-gw, processor-atomizers, processor-generators, frontend

**Files:**
- 10 new `test-result.md` files

**AC:**
- [ ] All 10 units have `test-result.md`
- [ ] Format matches `docs/template-test-result.md`
- [ ] Coverage accurately reflects current state

---

## P11-W3-006: Auto-Close Period + PPTX Generation Timeout

**Type:** Business Logic (simple)
**Effort:** 1 MD
**Priority:** LOW
**Audit Ref:** P10-W2-029, P10-W2-031

**Tasks:**
- [ ] **Auto-close period (REVIEWING → CLOSED):**
  - Add `reviewDeadline` check to `DeadlineService.checkPastDeadlines()`
  - If `reviewDeadline` is past and period is in REVIEWING → transition to CLOSED
  - Publish `notify` event for auto-close

- [ ] **PPTX generation timeout guard:**
  - Add `generation.timeout-seconds: 60` to `ms-gen-pptx/src/config.py`
  - Wrap `GenerateReport` gRPC handler in `asyncio.wait_for(timeout=config.GENERATION_TIMEOUT)`
  - On timeout, return partial result with error flag

**Files:**
- `apps/engine/engine-reporting/period/src/main/java/.../DeadlineService.java`
- `apps/processor/microservices/units/ms-gen-pptx/src/config.py`
- `apps/processor/microservices/units/ms-gen-pptx/src/service/generator_service.py`

**AC:**
- [ ] Period in REVIEWING past reviewDeadline auto-transitions to CLOSED
- [ ] PPTX generation exceeding 60s returns timeout error
- [ ] Both changes are configurable via config

---

## Summary

| Task | Effort | Priority |
|------|--------|----------|
| P11-W3-001: Clean Legacy Dapr + TOPICS.md | 1 MD | MEDIUM |
| P11-W3-002: Fix Dapr app-id + Observability Compose | 0.75 MD | LOW |
| P11-W3-003: Remove Hardcoded Passwords | 1 MD | LOW |
| P11-W3-004: Create READMEs (10 units) | 3 MD | HIGH |
| P11-W3-005: Create test-result.md (10 units) | 1 MD | MEDIUM |
| P11-W3-006: Auto-Close Period + PPTX Timeout | 1 MD | LOW |
| **Total** | **~7.75 MD** | |
