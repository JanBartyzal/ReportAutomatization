# P3b – Wave 3: Configuration (Haiku/Gemini)

**Phase:** P3b – Reporting Lifecycle & Period Management
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1 MD

---

## P3b-W3-001: Docker Compose – P3b Services

**Type:** Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] Add MS-LIFECYCLE, MS-PERIOD to docker-compose.yml
- [ ] Dapr sidecar configs
- [ ] Nginx routing: `/api/reports/*`, `/api/periods/*`
- [ ] Dapr Pub/Sub topic subscriptions for lifecycle events

---

## P3b-W3-002: Seed Data – Periods & Reports

**Type:** Test Data
**Effort:** 0.5 MD

**Tasks:**
- [ ] Sample periods: Q1/2026, Q2/2026 with deadlines
- [ ] Sample reports per test organization
- [ ] Sample status transitions for testing timeline view
- [ ] Flyway seed migration for dev environment
