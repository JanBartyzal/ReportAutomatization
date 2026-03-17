# P3b – Wave 1: Lifecycle State Machine & Period Engine (Opus)

**Phase:** P3b – Reporting Lifecycle & Period Management
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~25 MD
**Depends on:** P1 (orchestrator, auth), P3a (admin, batch)

> Complex state machine with Saga integration, deadline automation, and cross-service orchestration.

---

## P3b-W1-001: engine-reporting:lifecycle – Report Lifecycle Service

**Type:** Core Service
**Effort:** 12 MD
**Service:** apps/engine/microservices/units/ms-lifecycle

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **State Machine** (Spring State Machine):
  ```
  DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                    ↘                ↘ REJECTED → DRAFT (resubmit)
  ```
  - Guards: validate permissions (only Editor can submit, only HoldingAdmin/Reviewer can approve/reject)
  - Actions: publish events, create audit entries, lock/unlock data
- [ ] **REST Endpoints** (frontend-facing via API Gateway):
  - `GET /api/reports` – list reports for org/period (paginated, filterable)
  - `POST /api/reports` – create new report (auto-DRAFT)
  - `GET /api/reports/{id}` – report detail with status + history
  - `POST /api/reports/{id}/submit` – DRAFT → SUBMITTED
  - `POST /api/reports/{id}/review` – SUBMITTED → UNDER_REVIEW
  - `POST /api/reports/{id}/approve` – UNDER_REVIEW → APPROVED
  - `POST /api/reports/{id}/reject` – → REJECTED (comment mandatory)
  - `GET /api/reports/{id}/history` – state transition timeline
  - `POST /api/reports/bulk-approve` – approve multiple
  - `POST /api/reports/bulk-reject` – reject multiple (comment required)
  - `GET /api/reports/matrix` – HoldingAdmin matrix [Company × Period × Status]
- [ ] **Report Entity**:
  - Bound to `(org_id, period_id, report_type)` – one report per org per period
  - Links to form responses (FS19) and uploaded files
- [ ] **Submission Checklist**:
  - Configurable pre-submit checks:
    - All required form fields filled?
    - All required sheets uploaded?
    - Validation rules pass?
  - Editor sees checklist before submit button becomes active
- [ ] **Rejection Flow**:
  - Mandatory comment from Reviewer
  - Comment visible to Editor in UI
  - Report returns to DRAFT for correction
- [ ] **Data Locking**:
  - APPROVED → data becomes read-only
  - Any edit requires new transition to DRAFT (creates new version via FS14)
- [ ] **Dapr Pub/Sub Events**:
  - Publish `report.status_changed` on every transition
  - Publish `report.data_locked` on APPROVED
  - engine-orchestrator subscribes for downstream workflows (notifications, PPTX generation)
- [ ] **Audit Integration**: Every transition logged with `user_id`, `from_state`, `to_state`, `timestamp`, `comment`
- [ ] Flyway migrations: `reports`, `report_status_history`, `submission_checklists` tables
- [ ] Nginx routing: `/api/reports/*` → engine-reporting:lifecycle
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Editor cannot submit until checklist shows 100% complete
- [ ] APPROVED triggers data locking + inclusion in central reporting
- [ ] REJECTED sends notification to Editor with comment
- [ ] Audit log contains full transition history
- [ ] Bulk approve/reject works for multiple reports at once

---

## P3b-W1-002: engine-reporting:period – Reporting Period Manager

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/engine/microservices/units/ms-period

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **REST Endpoints** (frontend-facing):
  - `GET/POST /api/periods` – list/create reporting periods
  - `GET/PUT /api/periods/{id}` – get/update period
  - `POST /api/periods/{id}/clone` – clone from previous period
  - `GET /api/periods/{id}/status` – completion tracking matrix
  - `GET /api/periods/{id}/export` – export as PDF/Excel
- [ ] **Period Model**:
  - `{ name, type: MONTHLY|QUARTERLY|ANNUAL, start_date, submission_deadline, review_deadline, period_code }`
  - State: `OPEN` → `COLLECTING` → `REVIEWING` → `CLOSED`
  - Assigned to holding, visible to all subsidiaries
- [ ] **Deadline Management**:
  - Submission deadline: auto-close forms after deadline
  - Review deadline: target for HoldingAdmin review
  - Late submission requires explicit HoldingAdmin override
- [ ] **Automatic Notifications** (via Dapr Pub/Sub → engine-reporting:notification):
  - X days before deadline → remind users with DRAFT/empty forms
  - Configurable: 7, 3, 1 day before (default)
  - After deadline: escalation to HoldingAdmin listing non-compliant orgs
- [ ] **Completion Tracking**:
  - Matrix: [Company × Status] with color coding
  - Percentage: APPROVED / total required reports
  - Export as PDF or Excel
- [ ] **Period Cloning**:
  - Clone from previous period: copy form assignments, template assignments
  - New dates, same structure
- [ ] **Historical Access**:
  - Closed periods archived, accessible for comparison
  - Link to engine-core:versioning for version history within period
- [ ] Flyway migrations: `periods`, `period_org_assignments` tables
- [ ] Nginx routing: `/api/periods/*` → engine-reporting:period
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Period clone from previous takes < 2 minutes
- [ ] Forms auto-close after submission deadline
- [ ] Notifications sent 7/3/1 day before deadline to DRAFT users
- [ ] Status matrix loads < 3s for 50+ companies
- [ ] PDF/Excel export works for 50+ companies

---

## P3b-W1-003: engine-orchestrator Extension – Lifecycle Workflows

**Type:** Service Extension
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-orch (extension)

**Tasks:**
- [ ] **Dapr Pub/Sub Subscriber**: Subscribe to `report.status_changed` topic
- [ ] **Workflow Definitions** (JSON) per report_type:
  - On SUBMITTED: validate data completeness, run automated checks
  - On APPROVED: trigger inclusion in central reporting, optional PPTX generation
  - On REJECTED: trigger notification to Editor
- [ ] **Orchestration Actions**:
  - Validate data completeness → check all required fields via engine-data:query
  - Notify → publish to `notify` topic
  - Lock data → publish `report.data_locked`
  - Generate PPTX (prepared for P4b) → trigger processor-generators:pptx workflow
- [ ] Integration tests: full lifecycle flow DRAFT → APPROVED
