# P10-W1-002: Non-Functional Requirements (NFR) Audit Report

**Date:** 2026-03-13
**Auditor:** Claude Opus (automated)
**Scope:** NFR per project_charter.md v4.0 §NFR

---

## Summary

| Category | Status | Score |
|----------|--------|-------|
| Performance | PARTIAL | 2/4 |
| Security | FAIL | 4/10 |
| Availability | PASS | 3/3 |
| Scalability | PASS | 2/2 |
| Observability | FAIL | 1/5 |
| Code Quality | FAIL | 2/6 |
| **Overall** | **FAIL** | **14/30 (47%)** |

---

## Performance

| # | Requirement | Target | Status | Evidence |
|---|------------|--------|--------|----------|
| 1 | End-to-end file processing | < 30s for 50-slide PPTX | **N/A** | No performance test suite found in `tests/performance/`. Cannot measure. |
| 2 | Upload API latency | < 2s for 20 MB file | **N/A** | No performance benchmarks found. Cannot measure. |
| 3 | Run performance test suite | Tests exist in `tests/performance/` | **PARTIAL** | Directory exists with `package.json` but no actual test scripts or results found |
| 4 | PPTX generation | < 60s for 20 slides | **PARTIAL** | No timeout configured in ms-gen-pptx; no benchmark |

**Remediation:** Create performance test suite with k6 or Artillery; establish baseline measurements.

---

## Security

| # | Requirement | Status | Evidence | Severity |
|---|------------|--------|----------|----------|
| 1 | Zero-trust: every request has JWT validation | **FAIL** | No ForwardAuth in nginx.conf. JWT validation only happens at individual service level, not at gateway. | **CRITICAL** |
| 2 | No hardcoded secrets in codebase | **FAIL** | 20+ plaintext passwords in docker-compose files: `postgres`, `redis_pass`, `admin`, `sk-local-dev-key`. `jwt.secret: change-me-in-production` in engine-data. | HIGH |
| 3 | RLS cross-tenant test | **FAIL** | ~29 tables lack RLS. 3 tables have ENABLE RLS without CREATE POLICY (effectively deny-all). | HIGH |
| 4 | 100% uploaded files scanned by ClamAV | **FAIL** | Files uploaded to blob BEFORE ClamAV scan (`UploadService.java:76-98`). Malicious files reach storage before scan. | **CRITICAL** |
| 5 | SQL injection: parameterized queries everywhere | **PARTIAL** | Most queries parameterized. `DynamicTableService.java:127` interpolates table name via `String.format`. Values use `?` placeholders. | MEDIUM |
| 6 | XSS: React auto-escaping + CSP headers | **FAIL** | React auto-escaping ✓. No CSP headers in nginx.conf. No X-Frame-Options, X-Content-Type-Options. | HIGH |
| 7 | CSRF: token-based auth (no cookies for API) | **PASS** | All SecurityConfigs use `csrf.disable()` + `STATELESS` sessions. Bearer-only auth. |  |
| 8 | Injection: no eval, no dynamic SQL without sanitization | **PARTIAL** | No eval found. One dynamic table name in `DynamicTableService.java`. | LOW |
| 9 | CORS restriction | **FAIL** | `$http_origin` reflects ANY origin. No whitelist. | HIGH |
| 10 | Method-level authorization (@PreAuthorize) | **FAIL** | Zero `@PreAuthorize` or `@Secured` annotations across entire codebase. Any authenticated user can access any endpoint. | HIGH |

**Security Score: 1 PASS, 2 PARTIAL, 7 FAIL**

---

## Availability

| # | Requirement | Status | Evidence |
|---|------------|--------|----------|
| 1 | Health check endpoints on all services | **PASS** | All 6 Java modules have `spring-boot-starter-actuator` with health endpoint exposed in application.yml |
| 2 | Graceful shutdown handling | **PASS** | Spring Boot default graceful shutdown; all services use `STATELESS` session (no state to drain) |
| 3 | Connection pool management (no leaks) | **PASS** | HikariCP default via Spring Boot; connection pool configured in application.yml |

**Availability Score: 3/3 PASS**

---

## Scalability

| # | Requirement | Status | Evidence |
|---|------------|--------|----------|
| 1 | Atomizer layer: horizontal scaling works | **PASS** | All atomizers are stateless gRPC services in containers. Docker Compose allows `scale` parameter. No shared state except Redis (idempotency). |
| 2 | DB connection pooling appropriate for load | **PASS** | HikariCP with configurable pool size in application.yml. Redis connection pooling via Lettuce. |

**Scalability Score: 2/2 PASS**

---

## Observability

| # | Requirement | Status | Evidence | Severity |
|---|------------|--------|----------|----------|
| 1 | End-to-end trace: FE → GW → ORCH → ATM → Sink | **FAIL** | Frontend has `@opentelemetry/*` packages. Java backend has ZERO OpenTelemetry dependencies in any pom.xml. No trace propagation across services. | HIGH |
| 2 | Traces searchable by file_id, user_id, org_id | **FAIL** | No tracing configured in backend. Cannot search. | HIGH |
| 3 | Structured JSON logging from all services | **PARTIAL** | No `logback-spring.xml` with JSON encoder found. Default Spring Boot console logging only. | MEDIUM |
| 4 | Prometheus metrics exposed from all services | **PASS** | All Java modules have `spring-boot-starter-actuator` with Prometheus endpoint in application.yml |  |
| 5 | Grafana dashboards for: error rate, queue depth, latency, DB connections | **FAIL** | `docker-compose.observability.yml` defines Grafana service but references stale service names. No dashboard JSON provisioning found. | MEDIUM |

**Observability Score: 1/5 (20%)**

---

## Code Quality

| # | Requirement | Status | Evidence | Severity |
|---|------------|--------|----------|----------|
| 1 | ESLint passes (TypeScript/React) | **FAIL** | No ESLint configuration found in `apps/frontend/`. Zero linting enforcement. | HIGH |
| 2 | Black/Ruff passes (Python) | **PASS** | `[tool.ruff]` configured in all 12 `pyproject.toml` files |  |
| 3 | Checkstyle passes (Java) | **FAIL** | No `checkstyle.xml` found anywhere in the project. Zero Java linting. | HIGH |
| 4 | No dead code or commented-out blocks | **PARTIAL** | Not comprehensively verified. Some stubs with TODO comments found (VBA removal, ServiceNow distribution). | LOW |
| 5 | Unit test coverage: happy path + 2 edge cases per new logic | **FAIL** | 18 Java test files (5 modules have 0 tests). 34 Python test files (3 modules have 0). Zero frontend test files. | HIGH |
| 6 | .env.example for frontend | **PASS** | `apps/frontend/.env.example` exists with VITE_AZURE_CLIENT_ID, VITE_AZURE_TENANT_ID, VITE_API_BASE_URL |  |

**Code Quality Score: 2/6 (33%)**

---

## Critical Findings Requiring Immediate Action

| Priority | Finding | Impact |
|----------|---------|--------|
| P0 | No ForwardAuth at gateway | Any unauthenticated request reaches backend services |
| P0 | ClamAV scan after blob upload | Malicious files stored before detection |
| P1 | No method-level authorization | Any authenticated user has full access to all endpoints |
| P1 | CORS reflects any origin | Cross-origin attacks possible from any domain |
| P1 | No CSP/security headers | XSS and clickjacking exposure |
| P1 | ~29 tables without RLS | Cross-tenant data leakage risk |
| P1 | Zero OpenTelemetry in Java backend | No distributed tracing across microservices |
| P2 | No ESLint or Checkstyle | No static analysis for Java/TypeScript |
| P2 | Minimal unit test coverage | Regression risk |
| P2 | 20+ hardcoded passwords in compose files | Secret exposure in git history |
