# P5 – Wave 3: Local Dev & Testing (Haiku/Gemini)

**Phase:** P5 – DevOps Maturity & Onboarding
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~5 MD
**Status:** ✅ COMPLETED (2026-03-12)

---

## P5-W3-001: Tilt/Skaffold Local Dev Environment

**Type:** Infrastructure
**Effort:** 2 MD
**Status:** ✅ Done

**Tasks:**
- [x] `Tiltfile` for local development:
  - Build and deploy all services to local K8s (Kind) or Docker Compose
  - Hot-reload: Java (Spring DevTools), Python (auto-restart), React (Vite HMR)
  - Service port-forwarding
  - Log streaming
- [x] `tilt up` starts complete topology < 5 minutes
- [x] Service dependency management (DB, Redis ready before app services)
- [x] `README.md` with local development quickstart guide

**Deliverables:**
- `Tiltfile` — main orchestration (docker_compose backend)
- `tilt/Tiltfile.infra` — infra services (postgres, redis, azurite, clamav, dapr, litellm)
- `tilt/Tiltfile.java` — 20 Java microservices with live_update templates
- `tilt/Tiltfile.python` — 8 Python microservices with auto-restart
- `tilt/Tiltfile.frontend` — React frontend with Vite HMR
- `tilt/config.yaml` — default groups, timeouts, startup ordering
- `scripts/dev-start.sh` — pre-flight checks + tilt up
- `scripts/dev-stop.sh` — tilt down with --clean option
- `docs/LOCAL_DEV.md` — quickstart guide with all ports, debug, observability

---

## P5-W3-002: E2E Test Suite

**Type:** Testing
**Effort:** 2 MD
**Status:** ✅ Done

**Tasks:**
- [x] Playwright test suite:
  - Login flow (MSAL mock for test)
  - File upload → processing → view results
  - Dashboard creation and viewing
  - Report lifecycle (submit → approve)
- [x] API integration test suite:
  - Full pipeline test: upload → orchestrate → parse → store → query
  - Auth flow test: valid/invalid/expired tokens
  - RLS test: cross-tenant isolation verification
- [x] Test data fixtures and setup/teardown scripts
- [x] CI pipeline integration

**Deliverables:**
- `tests/e2e/` — 14 files: Playwright config, 4 page objects, 4 test specs (16 tests total), MSAL auth mock, fixtures
- `tests/integration/` — 11 files: vitest config, API client, auth/DB helpers, 3 test suites (pipeline, auth, RLS)
- `.github/workflows/ci-e2e.yml` — full E2E CI (Playwright + API integration + k6 smoke)
- `.github/workflows/ci-frontend.yml` — updated with e2e-smoke job

---

## P5-W3-003: Performance & Load Testing

**Type:** Testing
**Effort:** 1 MD
**Status:** ✅ Done

**Tasks:**
- [x] k6 or Artillery load test scripts:
  - Upload throughput test (concurrent uploads)
  - Query latency under load
  - Dashboard aggregation performance
- [x] Baseline metrics: latency p50/p95/p99, throughput
- [x] Report stored in `docs/performance/`

**Deliverables:**
- `tests/performance/scripts/upload-throughput.js` — 10 VUs, multipart upload, p95 < 5s
- `tests/performance/scripts/query-latency.js` — 20 VUs, p50 < 200ms, p95 < 1s, p99 < 2s
- `tests/performance/scripts/dashboard-aggregation.js` — 15 VUs, p95 < 3s
- `tests/performance/scripts/helpers/` — shared auth + config
- `tests/performance/config/thresholds.json` — SLA thresholds
- `tests/performance/run-all.sh` — orchestrator with JSON result capture
- `docs/performance/README.md` — baseline template + instructions
