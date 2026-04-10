# P12 – Wave 2: Live Excel Export & External Sync – Configuration & DB (Sonnet)

**Phase:** P12 – Live Excel Export & External Sync
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~6 MD
**Depends on:** P12-W1 (core services established)
**Feature Set:** FS27

> Docker Compose, DB migrations, Dapr configuration, Nginx routing, and API types for FS27.

---

## P12-W2-001: Docker Compose & Dapr Configuration

**Type:** Infrastructure
**Effort:** 2 MD

**Tasks:**
- [ ] **Docker Compose** (`infra/docker/docker-compose.yml`):
  - Extend `engine-integrations` service with excel-sync module (same container – no new service needed)
  - Add environment variables:
    - `EXCEL_SYNC_ENABLED=true`
    - `EXCEL_SYNC_THREAD_POOL_SIZE=4`
    - `EXCEL_SYNC_ALLOWED_PATHS=/mnt/exports`
  - Add Docker volume mount for local/network export path:
    - `- ${EXCEL_EXPORT_HOST_PATH:-./data/exports}:/mnt/exports`
  - Verify processor-generators service already includes xls module
- [ ] **Dapr Components** (`infra/docker/dapr/engine-integrations/`):
  - PubSub subscription for `data-imported` topic:
    ```yaml
    apiVersion: dapr.io/v1alpha1
    kind: Subscription
    metadata:
      name: excel-sync-data-imported
    spec:
      pubsubname: pubsub
      topic: data-imported
      route: /api/events/data-imported
    ```
  - State store for Redis distributed lock:
    ```yaml
    apiVersion: dapr.io/v1alpha1
    kind: Component
    metadata:
      name: excel-sync-lock
    spec:
      type: lock.redis
      metadata:
        - name: redisHost
          value: redis:6379
    ```
- [ ] **Nginx Routing** (`infra/docker/nginx/`):
  - Add route: `/api/export-flows/*` → engine-integrations:8106
  - ForwardAuth enabled (requires valid JWT)
- [ ] **Dapr topic registration** in engine-orchestrator:
  - Add `data-imported` to PubSub publish configuration
  - Update `TOPICS.md` with new topic schema

**AC:**
- [ ] `docker-compose up` starts engine-integrations with excel-sync module active
- [ ] `/api/export-flows` reachable via API Gateway
- [ ] Dapr subscription for `data-imported` registered
- [ ] Export volume mounted at `/mnt/exports` inside container

---

## P12-W2-002: DB Migrations (Flyway)

**Type:** Database
**Effort:** 2 MD

**Tasks:**
- [ ] **Migration: `V{next}__create_export_flow_definitions.sql`**:
  ```sql
  CREATE TABLE export_flow_definitions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id            UUID NOT NULL,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    sql_query         TEXT NOT NULL,
    target_type       VARCHAR(20) NOT NULL CHECK (target_type IN ('SHAREPOINT', 'LOCAL_PATH')),
    target_path       TEXT NOT NULL,
    target_sheet      VARCHAR(255) NOT NULL,
    file_naming       VARCHAR(20) DEFAULT 'CUSTOM' CHECK (file_naming IN ('CUSTOM', 'BATCH_NAME')),
    custom_file_name  VARCHAR(255),
    trigger_type      VARCHAR(20) DEFAULT 'AUTO' CHECK (trigger_type IN ('AUTO', 'MANUAL')),
    trigger_filter    JSONB DEFAULT '{}',
    sharepoint_config JSONB,
    is_active         BOOLEAN DEFAULT true,
    created_by        UUID NOT NULL,
    created_at        TIMESTAMPTZ DEFAULT now(),
    updated_at        TIMESTAMPTZ DEFAULT now()
  );

  CREATE INDEX idx_export_flow_org ON export_flow_definitions(org_id);
  CREATE INDEX idx_export_flow_active ON export_flow_definitions(org_id, is_active);
  ```
- [ ] **Migration: `V{next}__create_export_flow_executions.sql`**:
  ```sql
  CREATE TABLE export_flow_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_id           UUID NOT NULL REFERENCES export_flow_definitions(id),
    org_id            UUID NOT NULL,
    trigger_source    VARCHAR(20) CHECK (trigger_source IN ('AUTO', 'MANUAL')),
    trigger_event_id  UUID,
    status            VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    rows_exported     INT,
    target_path_used  TEXT,
    error_message     TEXT,
    started_at        TIMESTAMPTZ DEFAULT now(),
    completed_at      TIMESTAMPTZ
  );

  CREATE INDEX idx_export_exec_flow ON export_flow_executions(flow_id);
  CREATE INDEX idx_export_exec_org ON export_flow_executions(org_id);
  CREATE INDEX idx_export_exec_status ON export_flow_executions(flow_id, status);
  ```
- [ ] **Migration: `V{next}__rls_export_flows.sql`**:
  ```sql
  ALTER TABLE export_flow_definitions ENABLE ROW LEVEL SECURITY;
  CREATE POLICY export_flow_definitions_org_isolation ON export_flow_definitions
    USING (org_id = current_setting('app.current_org_id')::UUID);

  ALTER TABLE export_flow_executions ENABLE ROW LEVEL SECURITY;
  CREATE POLICY export_flow_executions_org_isolation ON export_flow_executions
    USING (org_id = current_setting('app.current_org_id')::UUID);
  ```
- [ ] **Rollback scripts** for each migration
- [ ] **Seed data** for integration tests:
  - Sample Export Flow definition (LOCAL_PATH type)
  - Sample execution records (SUCCESS + FAILED)
- [ ] Verify migrations run cleanly on fresh DB and on existing DB

**AC:**
- [ ] Flyway migrations execute without errors on `docker-compose up`
- [ ] Tables created with correct constraints and indexes
- [ ] RLS policies enforce org isolation
- [ ] Rollback migrations drop tables cleanly

---

## P12-W2-003: Proto Definition & gRPC Types

**Type:** API Contract
**Effort:** 1 MD

**Tasks:**
- [ ] **Proto file** (`packages/protos/generator/v1/excel_service.proto`):
  - `UpdateSheetRequest`, `UpdateSheetResponse` messages
  - `ExcelGeneratorService` with `rpc UpdateSheet`
  - Follow existing proto conventions from `packages/protos/`
- [ ] **Generated code**:
  - Python gRPC stubs for processor-generators (run `grpcio-tools` codegen)
  - Java gRPC stubs for engine-integrations (run `protoc` via Maven plugin)
- [ ] **TypeScript types** (`packages/types/`):
  - `ExportFlowDefinition`, `ExportFlowExecution` interfaces
  - `ExportFlowCreateRequest`, `ExportFlowUpdateRequest` request types
  - `ExportFlowTestResponse` (preview data)
  - Enums: `TargetType`, `FileNaming`, `TriggerType`, `ExecutionStatus`
- [ ] **OpenAPI spec update** for `/api/export-flows/*` endpoints
- [ ] Validate proto compatibility with existing service definitions

**AC:**
- [ ] Proto compiles without errors in both Python and Java
- [ ] TypeScript types match REST API response shapes
- [ ] OpenAPI spec documents all 8 endpoints with request/response schemas

---

## P12-W2-004: Application Configuration & Properties

**Type:** Configuration
**Effort:** 1 MD

**Tasks:**
- [ ] **Spring application properties** (`apps/engine/engine-integrations/app/src/main/resources/`):
  ```yaml
  excel-sync:
    enabled: true
    thread-pool-size: 4
    allowed-paths:
      - /mnt/exports
    lock-ttl-seconds: 300
    max-retry: 3
    retry-delay-seconds: 5
    max-excel-size-mb: 50
  sharepoint:
    token-cache-ttl-seconds: 3300  # 55 min (tokens valid 60 min)
  ```
- [ ] **Environment variable mapping**:
  - `EXCEL_SYNC_ENABLED` → `excel-sync.enabled`
  - `EXCEL_SYNC_THREAD_POOL_SIZE` → `excel-sync.thread-pool-size`
  - `EXCEL_SYNC_ALLOWED_PATHS` → `excel-sync.allowed-paths`
  - `SHAREPOINT_TOKEN_CACHE_TTL` → `sharepoint.token-cache-ttl-seconds`
- [ ] **Update `.env.example`** with all new environment variables and descriptions
- [ ] **processor-generators config** (`apps/processor/processor-generators/`):
  - Add `EXCEL_MAX_SIZE_MB=50` to configuration
  - Register `UpdateSheet` gRPC endpoint in service startup
- [ ] **Health check endpoint** for excel-sync module:
  - `GET /api/export-flows/health` → `{ status: UP, sharepoint: UP/DOWN, localPath: UP/DOWN }`

**AC:**
- [ ] All config properties documented in `.env.example`
- [ ] Service starts with default config without errors
- [ ] Health endpoint reports module status
