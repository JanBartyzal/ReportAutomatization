# P5 – Wave 1: Observability Stack & CI/CD (Opus)

**Phase:** P5 – DevOps Maturity & Onboarding
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~15 MD
**Depends on:** P1-P4 (all core services deployed)

> Production infrastructure requiring deep understanding of distributed tracing and deployment pipelines.

---

## P5-W1-001: OpenTelemetry End-to-End Tracing

**Type:** Infrastructure
**Effort:** 5 MD

**Tasks:**
- [ ] **OTEL Collector** deployment (Docker Compose + K8s manifest)
- [ ] **Java services** (Spring Boot):
  - OTEL Java agent auto-instrumentation
  - Custom spans for: orchestrator steps, gRPC calls, DB queries
  - Propagate trace context through Dapr sidecars
- [ ] **Python services** (FastAPI):
  - opentelemetry-instrumentation-fastapi
  - Custom spans for: atomizer processing, Blob operations
  - gRPC metadata propagation for trace context
- [ ] **Frontend**:
  - OpenTelemetry JS SDK for browser spans
  - Trace upload requests, API calls, navigation
- [ ] **Trace Backend**: Jaeger or Grafana Tempo
- [ ] **End-to-end trace**: FE → GW → MS-AUTH → MS-ING → MS-ORCH → MS-ATM-* → MS-SINK-*
- [ ] Trace correlation with structured logs (trace_id in log entries)

**AC:**
- [ ] Single trace visible from frontend upload to database write
- [ ] Each orchestrator step has its own span with duration
- [ ] Trace searchable by file_id, user_id, org_id

---

## P5-W1-002: CI/CD Pipelines

**Type:** Infrastructure
**Effort:** 6 MD

**Tasks:**
- [ ] **Pipeline Stages**:
  1. Lint (ESLint, Black, Checkstyle)
  2. Unit Tests (JUnit, Pytest, Vitest)
  3. Integration Tests (Testcontainers, WireMock)
  4. Docker Image Build
  5. Push to Container Registry (ACR)
  6. Deploy to Staging (ACA)
- [ ] **Per-Service Pipelines**: Trigger only when service code changes
- [ ] **Monorepo Support**: Detect changed services, run only affected pipelines
- [ ] **GraalVM Native Image Pipeline**: Separate pipeline for release builds
- [ ] **Environment Promotion**: Staging → Production with manual approval gate
- [ ] **Secrets**: Pipeline secrets from KeyVault (not hardcoded)
- [ ] GitHub Actions or Azure DevOps YAML definitions
- [ ] Branch protection: main requires passing CI

**AC:**
- [ ] Push to main triggers full CI pipeline
- [ ] Failed lint or test blocks merge
- [ ] Docker images tagged with git SHA
- [ ] Staging deployment automated, production requires approval

---

## P5-W1-003: Kubernetes/ACA Deployment Manifests

**Type:** Infrastructure
**Effort:** 4 MD

**Tasks:**
- [ ] **Azure Container Apps** manifests for all services:
  - Resource limits (CPU, memory per service)
  - Autoscaling rules (min/max replicas, scale triggers)
  - Health probes (liveness, readiness)
  - Dapr sidecar annotations
- [ ] **Helm Charts** (alternative for K8s):
  - Base chart in `packages/charts/`
  - Per-service values files
  - Configurable: replicas, resources, env vars
- [ ] **Secrets**: ACA secrets from KeyVault reference
- [ ] **Networking**: VNet integration, private endpoints for DB/Redis/Blob
- [ ] **Terraform** for infrastructure provisioning:
  - ACA environment, PostgreSQL Flexible Server, Redis Cache, Blob Storage
  - KeyVault, Azure Front Door, DNS
- [ ] Store in `infra/terraform/` and `infra/aca/`
