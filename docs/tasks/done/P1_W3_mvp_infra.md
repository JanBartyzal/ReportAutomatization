# P1 – Wave 3: Infrastructure & Configuration (Haiku/Gemini)

**Phase:** P1 – MVP Core
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~6 MD
**Depends on:** Can start in parallel with W1/W2

> Docker Compose, Dapr config, database setup, environment configuration.

---

## P1-W3-001: Docker Compose – Full P1 Topology

**Type:** Infrastructure
**Effort:** 3 MD

**Tasks:**
- [ ] `infra/docker/docker-compose.yml` with all P1 services:
  - MS-GW (Nginx), MS-AUTH, MS-ING, MS-SCAN, MS-ORCH
  - MS-ATM-PPTX, MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG
  - PostgreSQL 16, Redis, Azurite (Blob emulator)
  - MS-FE (Vite dev server)
- [ ] Dapr sidecar containers for each service
- [ ] Dapr component definitions (pubsub, statestore)
- [ ] `.env.example` with all configurable variables
- [ ] Shared Docker network
- [ ] Health checks and startup ordering
- [ ] Volume mounts for:
  - PostgreSQL data persistence
  - Azurite data persistence
  - Source code (hot-reload)
- [ ] `docker-compose.override.yml` for debug ports

**AC:**
- [ ] `docker compose up` starts full P1 topology
- [ ] All services healthy within 2 minutes
- [ ] Dapr dashboard accessible at `localhost:8080`

---

## P1-W3-002: Nginx API Gateway Configuration

**Type:** Configuration
**Effort:** 1 MD
**Service:** apps/engine/microservices/units/ms-gw

**Tasks:**
- [ ] `nginx.conf` with:
  - `/api/auth/*` → MS-AUTH (port 8081)
  - `/api/upload/*` → MS-ING (port 8082)
  - `/api/files/*` → MS-QRY (prepared, 404 until P2)
  - `/api/query/*` → MS-QRY (prepared)
  - `/api/dashboards/*` → MS-DASH (prepared)
  - `/api/admin/*` → MS-ADMIN (prepared)
  - `auth_request` → MS-AUTH verify endpoint
- [ ] Rate limiting zones (100r/s API, 10r/s Auth/Upload)
- [ ] CORS headers configuration
- [ ] Health check endpoint `/health`
- [ ] Docker Compose entry

---

## P1-W3-003: PostgreSQL Init Scripts & Base Migrations

**Type:** Database
**Effort:** 1 MD

**Tasks:**
- [ ] Init script: `CREATE EXTENSION IF NOT EXISTS vector;`
- [ ] Init script: create app user with limited privileges
- [ ] Init script: create separate schemas if needed
- [ ] Base Flyway migration: `V001__base_setup.sql`
  - RLS enable on all tables
  - `set_config('app.current_org_id', ...)` function
  - Audit trigger function template
- [ ] PostgreSQL connection pool settings per service

---

## P1-W3-004: Redis Configuration

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] Redis Docker Compose service with persistence
- [ ] Redis configuration for:
  - Dapr state store
  - Dapr pub/sub (Redis Streams)
  - Token cache (MS-AUTH)
  - Workflow state (MS-ORCH)
- [ ] Key naming conventions documented

---

## P1-W3-005: Azurite (Local Blob Storage) Setup

**Type:** Configuration
**Effort:** 0.5 MD

**Tasks:**
- [ ] Azurite Docker Compose service
- [ ] Default container: `files`
- [ ] CORS configuration for local dev
- [ ] Connection string in `.env`
- [ ] Verify upload/download works with both Java and Python SDKs
