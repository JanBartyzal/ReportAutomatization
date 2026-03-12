# P8 – Wave 4: Microservice Consolidation – Build/Deploy/Test Scripts (Flash/MiniMax)

**Phase:** P8 – Microservice Consolidation Refactoring
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend / Scripts
**Total Effort:** ~4 MD
**Depends on:** P8-W1 (consolidation), P8-W2 (Docker/infra)

> Updated build, deploy, and test scripts for the consolidated 8-service architecture.

---

## P8-W4-001: Build Script (`scripts/build.ps1` + `build.sh`)

**Type:** DevOps Script
**Effort:** 1 MD
**File:** `scripts/build.sh`

**Tasks:**
- [x] Build all or specific consolidated Docker images
- [x] Support flags: `--java`, `--python`, `--frontend`, `--no-cache`
- [x] Configurable registry and tag via `--registry` and `--tag`
- [x] Per-service build with name argument (e.g., `build.sh engine-core`)
- [x] Build summary with pass/fail count
- [x] Help text with `--help`

**AC:**
- [x] `./scripts/build.sh` builds all 9 images (6 Java + 2 Python + 1 Frontend)
- [x] `./scripts/build.sh engine-core` builds only engine-core
- [x] Failed builds listed in summary with exit code 1

---

## P8-W4-002: Deploy Script (`scripts/deploy.ps1` + `deploy.sh`)

**Type:** DevOps Script
**Effort:** 1 MD
**File:** `scripts/deploy.sh`

**Tasks:**
- [x] Docker Compose based deployment with consolidated services
- [x] Flags: `--up-only`, `--build-only`, `--observability`, `--clean`, `-d`
- [x] Auto-creates `.env` from `.env.example` if missing
- [x] Supports compose override and observability overlay files
- [x] Endpoint summary printed in detached mode
- [x] Specific service deployment (e.g., `deploy.sh engine-core`)

**AC:**
- [x] `./scripts/deploy.sh -d` starts all services in background
- [x] `./scripts/deploy.sh --observability -d` includes Grafana/Prometheus
- [x] `./scripts/deploy.sh --clean -d` fresh deploy with volume cleanup
- [x] Endpoint URLs displayed after startup

---

## P8-W4-003: Test Script (`scripts/test.ps1` + `test.sh`)

**Type:** DevOps Script
**Effort:** 1.5 MD
**File:** `scripts/test.sh`

**Tasks:**
- [x] Run unit tests for all modules: Java (Gradle), Python (Pytest), Frontend (Vitest)
- [x] Flags: `--java`, `--python`, `--frontend`, `--integration`, `--e2e`, `--coverage`
- [x] Per-module testing with name argument
- [x] Frontend: ESLint + TypeScript type check + Vitest
- [x] Integration tests with Testcontainers (requires Docker)
- [x] E2E tests with Playwright (requires running stack)
- [x] Test summary with pass/fail count and exit code

**AC:**
- [x] `./scripts/test.sh` runs all unit tests
- [x] `./scripts/test.sh --coverage` includes coverage reports
- [x] `./scripts/test.sh engine-core` runs tests for specific module
- [x] Exit code 1 if any test module fails

---

## P8-W4-004: Frontend Routing Updates for Consolidated APIs

**Type:** Frontend
**Effort:** 0.5 MD

**Tasks:**
- [ ] **Verify API base URLs**:
  - Frontend calls go through Nginx (port 8080) — path-based routing
  - No change needed in frontend API clients (same paths, Nginx handles routing)
  - Verify `apps/frontend/src/api/*.ts` files don't hardcode service ports
- [ ] **Health check page update**:
  - `HealthDashboardPage.tsx`: update service list to show 8 consolidated units
  - Health endpoint URLs updated to consolidated services
- [ ] Smoke test: frontend works with consolidated backend

**AC:**
- [ ] Frontend operates correctly against consolidated API Gateway
- [ ] Health dashboard shows 8 services instead of 29

---
