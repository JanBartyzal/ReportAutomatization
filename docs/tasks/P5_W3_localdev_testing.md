# P5 – Wave 3: Local Dev & Testing (Haiku/Gemini)

**Phase:** P5 – DevOps Maturity & Onboarding
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~5 MD

---

## P5-W3-001: Tilt/Skaffold Local Dev Environment

**Type:** Infrastructure
**Effort:** 2 MD

**Tasks:**
- [ ] `Tiltfile` for local development:
  - Build and deploy all services to local K8s (Kind) or Docker Compose
  - Hot-reload: Java (Spring DevTools), Python (auto-restart), React (Vite HMR)
  - Service port-forwarding
  - Log streaming
- [ ] `tilt up` starts complete topology < 5 minutes
- [ ] Service dependency management (DB, Redis ready before app services)
- [ ] `README.md` with local development quickstart guide

---

## P5-W3-002: E2E Test Suite

**Type:** Testing
**Effort:** 2 MD

**Tasks:**
- [ ] Playwright test suite:
  - Login flow (MSAL mock for test)
  - File upload → processing → view results
  - Dashboard creation and viewing
  - Report lifecycle (submit → approve)
- [ ] API integration test suite:
  - Full pipeline test: upload → orchestrate → parse → store → query
  - Auth flow test: valid/invalid/expired tokens
  - RLS test: cross-tenant isolation verification
- [ ] Test data fixtures and setup/teardown scripts
- [ ] CI pipeline integration

---

## P5-W3-003: Performance & Load Testing

**Type:** Testing
**Effort:** 1 MD

**Tasks:**
- [ ] k6 or Artillery load test scripts:
  - Upload throughput test (concurrent uploads)
  - Query latency under load
  - Dashboard aggregation performance
- [ ] Baseline metrics: latency p50/p95/p99, throughput
- [ ] Report stored in `docs/performance/`
