# P4a – Wave 1: Versioning & Audit (Opus)

**Phase:** P4a – Enterprise Features
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~17 MD
**Depends on:** P1-P3 (core pipeline, lifecycle, forms)

> Immutable audit logs and data versioning require careful design to prevent data loss.

---

## P4a-W1-001: engine-core:versioning – Versioning Service

**Type:** Core Service
**Effort:** 7 MD
**Service:** apps/engine/microservices/units/ms-ver

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Versioning Engine**:
  - Every data change creates new version (v1 → v2)
  - Original always preserved (never overwritten)
  - Version chain: `entity_id` → `[v1, v2, v3, ...]`
  - Supports: table records, form responses, documents
- [ ] **REST Endpoints** (frontend-facing):
  - `GET /api/versions/{entity_type}/{entity_id}` – list all versions
  - `GET /api/versions/{entity_type}/{entity_id}/diff?v1=1&v2=2` – diff between versions
- [ ] **Diff Engine**:
  - Field-by-field comparison
  - Detect: changed values, added/removed rows, added/removed fields
  - For OPEX data: highlight monetary changes (e.g., "+500k in IT costs v2 vs v1")
  - Output: structured diff with change type indicators
- [ ] **Integration with Lifecycle**:
  - APPROVED → data locked; edit creates new version and returns report to DRAFT
  - Version metadata: `created_by`, `created_at`, `reason`
- [ ] Flyway migrations: `versions`, `version_diffs` tables
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Edit after APPROVED → new version created, original preserved
- [ ] Diff shows field-level changes with direction indicators
- [ ] Version history queryable by entity

---

## P4a-W1-002: engine-core:audit – Audit & Compliance

**Type:** Core Service
**Effort:** 10 MD
**Service:** apps/engine/microservices/units/ms-audit

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Immutable Audit Log**:
  - Append-only table (INSERT only, no UPDATE/DELETE for app user)
  - Entry: `who` (user_id), `when` (timestamp), `what` (action), `where` (entity_type + entity_id), `details` (JSONB)
  - Covers: file uploads, data changes, role changes, report transitions, form submissions
- [ ] **Read Access Log**:
  - Log every view of sensitive report/data
  - Fields: user_id, document_id, IP address, timestamp, user_agent
- [ ] **AI Audit** (prepared for FS12):
  - Log every AI prompt and response
  - Fields: user_id, prompt_text, response_text, model, tokens_used, org_id
  - Enable future review for hallucinations and data leaks
- [ ] **REST Endpoints** (frontend-facing):
  - `GET /api/audit/logs` – paginated, filterable (by user, action, date range, entity)
  - `GET /api/audit/export` – CSV/JSON download for security audit
- [ ] **State Transition Audit** (FS17 integration):
  - Subscribe to `report.status_changed` events
  - Record: from_state, to_state, user_id, comment, timestamp
- [ ] **Form Action Audit** (FS19 integration):
  - Field changed, comment added, import confirmed
  - Granular field-level tracking
- [ ] Flyway migrations: `audit_logs`, `read_access_logs`, `ai_audit_logs` tables
- [ ] RLS: HoldingAdmin sees all orgs, Admin sees own org only
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/audit/*` → engine-core:audit

**AC:**
- [ ] Audit log entries are truly immutable (DB constraints verified)
- [ ] Export produces valid CSV/JSON with all fields
- [ ] AI audit captures full prompt and response
- [ ] Read access log captures every sensitive data view
