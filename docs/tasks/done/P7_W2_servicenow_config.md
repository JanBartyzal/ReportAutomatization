# P7 – Wave 2: Service-Now & Smart Persistence – Configuration & Infra (Sonnet)

**Phase:** P7 – External Integrations & Data Optimization
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~8 MD
**Depends on:** P7-W1 (core services established)

> Configuration, Docker Compose, DB migrations, and Dapr setup for FS23/FS24 services.

---

## P7-W2-001: MS-EXT-SNOW – Docker & Dapr Configuration

**Type:** Infrastructure
**Effort:** 2 MD

**Tasks:**
- [ ] **Docker Compose** (`infra/docker/docker-compose.yml`):
  - Add `ms-ext-snow` service definition (Java 21, port 8110)
  - Add `ms-gen-xls` service definition (Python, port 8111)
  - Dapr sidecar containers for both services
  - Health check endpoints
  - Environment variables for KeyVault, Service-Now defaults
- [ ] **Dapr Components**:
  - `infra/docker/dapr/ms-ext-snow/` component configs
  - PubSub subscription for sync events
  - State store for scheduler distributed lock
- [ ] **Nginx routing** (MS-GW):
  - `/api/admin/integrations/*` → ms-ext-snow:8110
  - `/api/reports/excel/*` → ms-gen-xls:8111 (via MS-GW)
- [ ] **Tilt configuration** (`tilt/`) for local dev hot-reload

**AC:**
- [ ] `docker-compose up` starts both new services with Dapr sidecars
- [ ] Services reachable via API Gateway routes
- [ ] `tilt up` includes new services with hot-reload

---

## P7-W2-002: DB Migrations for FS23 & FS24

**Type:** Database
**Effort:** 3 MD

**Tasks:**
- [ ] **MS-EXT-SNOW migrations** (Flyway):
  - `V1__create_integration_configs.sql`:
    ```
    integration_configs (id, instance_url, auth_type, credentials_ref, org_id, created_at, updated_at)
    integration_tables (id, integration_id, table_name, mapping_template_id, sync_mode)
    ```
  - `V2__create_sync_schedules.sql`:
    ```
    sync_schedules (id, integration_id, cron_expression, enabled, last_run, next_run, status)
    sync_job_history (id, schedule_id, start_time, end_time, records_fetched, records_stored, status, error_detail)
    ```
  - `V3__create_distribution_rules.sql`:
    ```
    distribution_rules (id, schedule_id, report_template_id, recipients, format, enabled)
    distribution_history (id, rule_id, recipients, timestamp, status, error_detail)
    ```
  - RLS policies on all tables (org_id based)
- [ ] **MS-TMPL migration** (FS24 – usage tracking):
  - `V{next}__add_mapping_usage_tracking.sql`:
    ```
    mapping_usage (id, mapping_id, usage_count, last_used, distinct_org_count, candidate_status)
    ```
- [ ] **MS-ADMIN migration** (FS24 – promotion):
  - `V{next}__create_promotion_candidates.sql`:
    ```
    promotion_candidates (id, mapping_id, proposed_ddl, proposed_indexes, status, admin_notes, approved_at, approved_by)
    promoted_tables (id, mapping_id, table_name, created_at, dual_write_until)
    ```
- [ ] Rollback scripts for all migrations
- [ ] Seed data for integration tests

**AC:**
- [ ] All migrations run cleanly on fresh DB
- [ ] Rollback scripts work without data loss
- [ ] RLS policies enforced on all new tables

---

## P7-W2-003: Proto & API Definitions for FS23/FS24

**Type:** Contracts
**Effort:** 1.5 MD

**Tasks:**
- [ ] **Proto definitions** (`packages/protos/`):
  - `generator/v1/excel_report.proto`:
    - `service ExcelGenerator { rpc GenerateExcel(GenerateExcelRequest) returns (GenerateExcelResponse); }`
  - `integration/v1/servicenow.proto`:
    - Internal gRPC contracts for MS-ORCH ↔ MS-EXT-SNOW communication
- [ ] **OpenAPI specs** (REST endpoints via MS-GW):
  - `docs/api/ms-ext-snow-openapi.yaml`: Admin integration endpoints
  - `docs/api/ms-gen-xls-openapi.yaml`: Excel generation endpoints
- [ ] **TypeScript types** (`packages/types/src/`):
  - `integration.ts`: `IntegrationConfig`, `SyncSchedule`, `SyncJobHistory`, `DistributionRule`
  - `promotion.ts`: `PromotionCandidate`, `PromotedTable`
- [ ] Run `scripts/proto-gen.sh` to generate Java & Python stubs

**AC:**
- [ ] Proto files compile without errors
- [ ] Generated stubs available in `gen/` directories
- [ ] TS types importable from `@reportautomatization/types`

---

## P7-W2-004: MS-DASH Extension – Service-Now Data Visualization

**Type:** Service Extension
**Effort:** 1.5 MD
**Service:** apps/engine/microservices/units/ms-dash (extension)

**Tasks:**
- [ ] **Data Source Registration**:
  - New `source_type: SERVICENOW` alongside existing `FILE` and `FORM`
  - Dashboard queries can filter/group by source type
- [ ] **Aggregation Endpoints**:
  - Service-Now data queryable via same MS-DASH REST API
  - Support for cross-source dashboards (Service-Now + Form + File data)
- [ ] Unit tests for new source type queries

**AC:**
- [ ] Service-Now data appears in BI dashboards alongside file/form data
- [ ] Source type filter works correctly

---
