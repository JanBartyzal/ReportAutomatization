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
- [x] **OTEL Collector** deployment (Docker Compose overlay `docker-compose.observability.yml`)
  - Config: `infra/docker/otel-collector-config.yaml`
  - Receivers: OTLP (gRPC:4317, HTTP:4318) + Zipkin (9411, Dapr compat)
  - Exporters: Tempo (traces), Prometheus (metrics), Loki (logs)
- [x] **Java services** (Spring Boot):
  - `OtelTracingConfig.java` — OTEL tracer provider via Micrometer bridge
  - `CustomSpanHelper.java` — custom spans with file_id, user_id, org_id attributes
  - `DaprTracePropagationFilter.java` — W3C traceparent propagation + MDC enrichment
  - Added `micrometer-registry-prometheus` dependency
- [x] **Python services** (FastAPI):
  - `packages/python-base/python_base/tracing.py` — centralized OTEL setup
  - Updated `grpc_server.py` — auto-calls `setup_tracing()` on init
  - Updated `context.py` — bridges traceparent header to OTEL context
  - Updated `logging_config.py` — enriches logs with span_id from OTEL
- [x] **Frontend**:
  - `apps/frontend/src/telemetry/otel.ts` — WebTracerProvider + FetchInstrumentation
  - Added @opentelemetry/* packages to package.json
  - Updated `main.tsx` — calls `initTelemetry()` before render
  - Nginx proxy `/otel/` → otel-collector:4318 for browser spans
- [x] **Trace Backend**: Grafana Tempo (unified Grafana stack)
  - Config: `infra/docker/tempo-config.yaml` (local storage, 7d retention)
  - Loki for log aggregation: `infra/docker/loki-config.yaml`
  - Prometheus: `infra/docker/prometheus.yml`
  - Grafana with auto-provisioned datasources + dashboards
- [x] **End-to-end trace**: FE → GW → engine-core:auth → engine-ingestor → engine-orchestrator → processor-atomizers → engine-data (sink modules)
  - Dapr tracing redirected to OTEL Collector (`infra/dapr/config.yaml`)
  - OTEL env vars injected via docker-compose overlay for all 30 services
- [x] Trace correlation with structured logs (trace_id/span_id in log entries)

**AC:**
- [x] Single trace visible from frontend upload to database write
- [x] Each orchestrator step has its own span with duration (via CustomSpanHelper)
- [x] Trace searchable by file_id, user_id, org_id (Grafana Tempo dashboard with variables)

---

## P5-W1-002: CI/CD Pipelines

**Type:** Infrastructure
**Effort:** 6 MD

**Tasks:**
- [x] **Pipeline Stages** (GitHub Actions):
  1. Lint (ESLint, Ruff/MyPy, Checkstyle)
  2. Unit Tests (JUnit, Pytest)
  3. Integration Tests (with postgres, redis, azurite services)
  4. Docker Image Build
  5. Push to Container Registry (ACR)
  6. Deploy to Staging (ACA)
- [x] **Per-Service Pipelines**: Path-filter triggers only affected services
  - `.github/workflows/ci-java.yml` — 20 Java services
  - `.github/workflows/ci-python.yml` — 8 Python services
  - `.github/workflows/ci-frontend.yml` — Frontend
- [x] **Monorepo Support**: `.github/actions/detect-changes/action.yml` using dorny/paths-filter
  - Base package changes trigger all services of that type
  - Individual service paths trigger only that service
- [x] **GraalVM Native Image Pipeline**: `.github/workflows/ci-graalvm.yml` (on tag `v*`)
- [x] **Environment Promotion**: Staging → Production with manual approval gate
  - `.github/workflows/deploy-staging.yml` — auto-deploy on main CI success
  - `.github/workflows/deploy-production.yml` — manual dispatch + confirmation
- [x] **Secrets**: ACR credentials, Azure OIDC login via GitHub secrets
- [x] GitHub Actions YAML definitions (7 workflow files)
- [x] Branch protection: documented in deploy workflows (require CI status checks)

**AC:**
- [x] Push to main triggers full CI pipeline
- [x] Failed lint or test blocks merge
- [x] Docker images tagged with git SHA
- [x] Staging deployment automated, production requires approval

---

## P5-W1-003: Kubernetes/ACA Deployment Manifests

**Type:** Infrastructure
**Effort:** 4 MD

**Tasks:**
- [x] **Azure Container Apps** manifests for all services:
  - Reusable `infra/terraform/modules/microservice.bicep` module
  - Resource limits (CPU, memory per service — 3 tiers)
  - Autoscaling rules (HTTP concurrency + CPU utilization)
  - Health probes (liveness, readiness)
  - Dapr sidecar (appId, appPort, appProtocol)
  - OTEL env vars auto-injected
- [x] **Bicep IaC** (instead of Helm — Bicep already in use):
  - Extended `infra/terraform/main.bicep` with all 30 services
  - Always-on: ms-auth, ms-ing, ms-orch, ms-qry, ms-fe (minReplicas: 1)
  - Scale-to-zero: all atomizers, sinks, batch services (minReplicas: 0)
  - Dapr component scopes updated for all services
- [x] **Secrets**: Managed via KeyVault references in Bicep
- [x] **Networking**: VNet integration, private endpoints for DB/Redis/Blob
  - `infra/terraform/modules/vnet.bicep`
  - `infra/terraform/modules/private-endpoints.bicep`
- [x] **Infrastructure provisioning** (Bicep modules):
  - `keyvault.bicep` — Azure Key Vault with RBAC
  - `postgresql.bicep` — PostgreSQL Flexible Server + pgvector
  - `redis.bicep` — Azure Cache for Redis, TLS
  - `storage.bicep` — Blob Storage with containers
  - `acr.bicep` — Container Registry
  - `front-door.bicep` — Azure Front Door + WAF
  - `log-analytics.bicep` — Log Analytics + App Insights
- [x] Store in `infra/terraform/modules/`
