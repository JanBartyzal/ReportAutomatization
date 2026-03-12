# P3b – Wave 2: Notification Integration (Sonnet)

**Phase:** P3b – Reporting Lifecycle & Period Management
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~7 MD

---

## P3b-W2-001: MS-NOTIF Integration for Lifecycle Events

**Type:** Service Extension
**Effort:** 4 MD
**Service:** apps/engine/microservices/units/ms-notif (preparation)

**Tasks:**
- [ ] Prepare MS-NOTIF scaffolding (full impl in P4a)
- [ ] **Dapr Pub/Sub Subscriber**: Subscribe to `notify` topic
- [ ] **Notification Routing**:
  - `REPORT_SUBMITTED` → notify HoldingAdmin
  - `REPORT_APPROVED` → notify Editor
  - `REPORT_REJECTED` → notify Editor with rejection comment
  - `DEADLINE_APPROACHING` → notify all DRAFT users in period
  - `DEADLINE_MISSED` → notify HoldingAdmin with non-compliant list
- [ ] **In-App Notifications**: Store in DB, return via REST
- [ ] Basic WebSocket endpoint for real-time push (full SSE in P4a)

---

## P3b-W2-002: Deadline Scheduler (Cron)

**Type:** Scheduled Job
**Effort:** 3 MD

**Tasks:**
- [ ] Scheduled job (Dapr cron binding or Spring @Scheduled):
  - Run daily at 08:00 UTC
  - Check all OPEN/COLLECTING periods for upcoming deadlines
  - Trigger notifications at configured intervals (7, 3, 1 day)
- [ ] Auto-close logic:
  - After submission_deadline: close forms, update period status to REVIEWING
  - After review_deadline: escalation notification to HoldingAdmin
- [ ] Late submission handling:
  - Detect orgs with DRAFT/no-submission after deadline
  - Generate non-compliance list for HoldingAdmin
