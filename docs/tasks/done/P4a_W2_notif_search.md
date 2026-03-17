# P4a – Wave 2: Notifications & Search (Sonnet)

**Phase:** P4a – Enterprise Features
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~12 MD

---

## P4a-W2-001: engine-reporting:notification – Notification Center (Full Implementation)

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/engine/microservices/units/ms-notif

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Dapr Pub/Sub Subscriber**: Topic `notify`
- [ ] **In-App Notifications**:
  - Store in `notifications` table
  - REST: `GET /api/notifications` – list for current user
  - REST: `PUT /api/notifications/{id}/read` – mark as read
  - REST: `PUT /api/notifications/read-all` – mark all read
  - **SSE Endpoint**: `GET /api/notifications/stream` – real-time push
  - WebSocket alternative for browsers without SSE support
- [ ] **Email Notifications** (SMTP):
  - Send email for critical events (processing failed, deadline missed)
  - HTML email templates (Thymeleaf)
  - SMTP configuration via KeyVault
- [ ] **Granular Settings**:
  - Per-user opt-in/opt-out per notification type
  - Per-org default settings (Admin configurable)
  - REST: `GET/PUT /api/notifications/settings`
- [ ] **Notification Types**:
  - FILE_PROCESSED, FILE_FAILED
  - REPORT_SUBMITTED, REPORT_APPROVED, REPORT_REJECTED
  - DEADLINE_APPROACHING, DEADLINE_MISSED
  - BATCH_COMPLETED
- [ ] Flyway migrations: `notifications`, `notification_settings` tables
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/notifications/*` → engine-reporting:notification

**AC:**
- [ ] In-app notification appears in < 2s after event
- [ ] Email sent for critical events
- [ ] User can opt-out of specific notification types
- [ ] SSE stream works reliably

---

## P4a-W2-002: engine-data:search – Search Service

**Type:** Core Service
**Effort:** 4 MD
**Service:** apps/engine/microservices/units/ms-srch

**Tasks:**
- [ ] Spring Boot 3.x project using `packages/java-base`
- [ ] **Full-Text Search**: PostgreSQL FTS (tsvector/tsquery)
  - Index parsed text from documents and table data
  - Language support: Czech, English
- [ ] **Vector Search**: pgVector cosine similarity
  - Semantic search using embeddings from engine-data:sink-doc
  - Threshold-based filtering (relevance > 0.7)
- [ ] **REST Endpoints**:
  - `GET /api/search?q=query&type=text|semantic` – combined search
  - `GET /api/search/suggest?q=partial` – autocomplete suggestions
- [ ] Results ranked by relevance, scoped by org_id (RLS)
- [ ] Docker Compose entry + Dapr sidecar
- [ ] Nginx routing: `/api/search/*` → engine-data:search
