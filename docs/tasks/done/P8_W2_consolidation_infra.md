# P8 – Wave 2: Microservice Consolidation – Infrastructure (Sonnet)

**Phase:** P8 – Microservice Consolidation Refactoring
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~12 MD
**Depends on:** P8-W1 (consolidation architecture decided)

> Docker Compose rewrite, Dapr reconfiguration, Nginx routing updates, Tilt reconfiguration.

---

## P8-W2-001: Docker Compose Rewrite

**Type:** Infrastructure
**Effort:** 4 MD

**Tasks:**
- [ ] **New `docker-compose.yml`** (`infra/docker/`):
  - Replace 29 service definitions with 8 consolidated units:
    - `engine-core` (port 8081, gRPC 50051)
    - `engine-ingestor` (port 8082, gRPC 50052)
    - `engine-orchestrator` (port 8095, gRPC 50053)
    - `engine-data` (port 8100, gRPC 50054)
    - `engine-reporting` (port 8105, gRPC 50055)
    - `engine-integrations` (port 8110, gRPC 50056)
    - `processor-atomizers` (port 8090, gRPC 50090)
    - `processor-generators` (port 8111, gRPC 50091)
  - Each with single Dapr sidecar (not 29 sidecars)
  - Infrastructure services unchanged: PostgreSQL, Redis, Azurite, ClamAV, Nginx
- [ ] **Observability overlay** (`docker-compose.observability.yml`):
  - Update OTEL env vars for 8 services instead of 29
  - Fewer Dapr sidecar scrape targets for Prometheus
- [ ] **Override file** (`docker-compose.override.yml`):
  - Development volume mounts for each consolidated unit
  - Debug ports per unit
- [ ] **Resource optimization**:
  - Memory limits: each Java unit ~512MB (was ~256MB × N services)
  - CPU limits adjusted for consolidated workload
  - Total memory savings: ~60% reduction in sidecar overhead
- [ ] **Health checks**: unified health check per unit (`/actuator/health`)
- [ ] **Network**: single docker network, simplified DNS

**AC:**
- [ ] `docker-compose up` starts 8 app containers + infra (was 29+ app containers)
- [ ] All services reachable via correct ports
- [ ] Total memory footprint reduced by ~40%+
- [ ] `docker-compose down && docker-compose up` clean restart works

---

## P8-W2-002: Dapr Configuration Consolidation

**Type:** Infrastructure
**Effort:** 3 MD

**Tasks:**
- [ ] **Component consolidation** (`infra/docker/dapr/`):
  - Merge 29 service-specific Dapr configs into 8:
    - `infra/docker/dapr/engine-core/` – pubsub, statestore, secrets
    - `infra/docker/dapr/engine-data/` – pubsub, statestore
    - `infra/docker/dapr/engine-reporting/` – pubsub, statestore
    - `infra/docker/dapr/engine-orchestrator/` – pubsub, statestore (unchanged)
    - `infra/docker/dapr/engine-ingestor/` – pubsub
    - `infra/docker/dapr/engine-integrations/` – pubsub, statestore
    - `infra/docker/dapr/processor-atomizers/` – minimal
    - `infra/docker/dapr/processor-generators/` – minimal
- [ ] **PubSub topic routing**:
  - Merge subscription configs for topics consumed by multiple former services now in same unit
  - e.g., `engine-reporting` subscribes to: `report.status_changed`, `notify`, `deadline.*`, `form.*`
  - Remove internal topics that are now in-process calls
- [ ] **App-ID mapping document**: Create migration reference table
- [ ] **Dapr config.yaml** (`infra/dapr/config.yaml`):
  - Update tracing targets for 8 app-ids
  - Update metrics labels

**AC:**
- [ ] Dapr sidecars start cleanly for all 8 units
- [ ] PubSub events route to correct consolidated service
- [ ] No orphaned subscriptions or dead-letter issues

---

## P8-W2-003: API Gateway (Nginx) Routing Update

**Type:** Infrastructure
**Effort:** 2 MD

**Tasks:**
- [ ] **Nginx config** (`infra/docker/nginx.conf`):
  - Update upstream definitions:
    ```
    upstream engine-core { server engine-core:8081; }
    upstream engine-ingestor { server engine-ingestor:8082; }
    upstream engine-data { server engine-data:8100; }
    upstream engine-reporting { server engine-reporting:8105; }
    upstream engine-integrations { server engine-integrations:8110; }
    ```
  - Route mapping:
    - `/api/auth/*` → engine-core
    - `/api/admin/*` → engine-core
    - `/api/batch/*` → engine-core
    - `/api/versions/*` → engine-core
    - `/api/audit/*` → engine-core
    - `/api/upload/*` → engine-ingestor
    - `/api/query/*` → engine-data
    - `/api/dashboards/*` → engine-data
    - `/api/search/*` → engine-data
    - `/api/templates/mapping/*` → engine-data
    - `/api/reports/*` → engine-reporting
    - `/api/periods/*` → engine-reporting
    - `/api/forms/*` → engine-reporting
    - `/api/templates/pptx/*` → engine-reporting
    - `/api/notifications/*` → engine-reporting
    - `/api/integrations/*` → engine-integrations
    - `/api/generate/*` → processor-generators (via REST)
  - ForwardAuth still pointing to engine-core (`/api/auth/validate`)
- [ ] **Rate limiting**: Update per-upstream limits
- [ ] **SSL/CORS**: No changes needed (same domain)
- [ ] Test all routes with curl/httpie

**AC:**
- [ ] All frontend API calls route to correct consolidated service
- [ ] ForwardAuth validation works
- [ ] Rate limiting applies correctly per upstream

---

## P8-W2-004: Tilt / Local Dev Reconfiguration

**Type:** Infrastructure
**Effort:** 1.5 MD

**Tasks:**
- [ ] **Tiltfile** update:
  - 8 service resources instead of 29
  - Hot-reload paths updated to consolidated source directories
  - Build contexts for multi-module Gradle projects
  - Python hot-reload for consolidated processor packages
- [ ] **`scripts/dev-start.sh`**: Update service count checks and startup messages
- [ ] **`scripts/dev-stop.sh`**: Update for new service names
- [ ] **Port allocation table**: Update docs with new port assignments

**AC:**
- [ ] `tilt up` starts consolidated topology with hot-reload
- [ ] Code changes in any module trigger correct rebuild

---

## P8-W2-005: CI/CD Pipeline Updates

**Type:** Infrastructure
**Effort:** 1.5 MD

**Tasks:**
- [ ] **GitHub Actions workflows**:
  - `.github/workflows/ci-java.yml`: Update to build 6 consolidated Java units (was 20)
  - `.github/workflows/ci-python.yml`: Update to build 2 consolidated Python units (was 8)
  - Path filters updated for new directory structure
  - Build times should decrease (fewer Docker image builds)
- [ ] **Dockerfile updates**:
  - Multi-module Gradle build: `COPY --from=build` for specific module JARs
  - Python: single Dockerfile per processor unit with all dependencies
- [ ] **Deploy workflows**: Update service names in staging/production deploy
- [ ] **GraalVM pipeline**: Update for consolidated builds (optional, may need adjustment)

**AC:**
- [ ] CI pipeline builds and tests all 8 units
- [ ] Path-based triggers correctly detect changes in consolidated structure
- [ ] Docker images push to registry with new naming convention

---
