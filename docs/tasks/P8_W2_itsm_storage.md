# P8-W2: ITSM Dedicated Storage

**Phase:** P8
**Module:** engine-data (migrations)
**Effort:** ~2 MD
**Priority:** HIGH
**Depends on:** P8-W1

---

## Goal

Create dedicated PostgreSQL tables for ServiceNow ITSM data with proper typed columns,
indexes, and RLS policies. These tables replace the generic JSONB dump approach.

---

## New Tables

### `snow_incidents`
Stores ServiceNow incident records. Key columns:
- `sys_id`, `number`, `org_id`, `connection_id`, `resolver_group_id`
- `short_description`, `state`, `priority`, `urgency`, `impact`, `category`, `subcategory`
- `assignment_group_sys_id`, `assignment_group_name`
- `assigned_to`, `opened_by`
- `opened_at`, `resolved_at`, `closed_at`, `sla_due`
- `is_sla_breached` (boolean)
- `resolution_time_hours` (numeric, derived)
- `age_days` (numeric, derived)
- `raw_fields` (JSONB – full SN record for forward compatibility)
- `synced_at`, `created_at`

### `snow_requests`
Stores ServiceNow request records. Key columns:
- `sys_id`, `number`, `org_id`, `connection_id`, `resolver_group_id`
- `short_description`, `state`, `approval`, `stage`
- `assignment_group_sys_id`, `assignment_group_name`
- `requested_for`, `requested_by`
- `opened_at`, `due_date`, `closed_at`
- `raw_fields` (JSONB)
- `synced_at`, `created_at`

---

## Migration File

- `V12_0_2__snow_itsm_tables.sql` (engine-data migrations)

## Query Views / Endpoints (engine-data:query extension)

New controller `ItsmQueryController.java` with:
- `GET /api/data/snow/incidents` – paginated, filterable by group, state, priority
- `GET /api/data/snow/requests` – paginated, filterable
- `GET /api/data/snow/itsm-kpis` – aggregated KPI view

---

## Acceptance Criteria

- [ ] Tables created with proper column types and NOT NULL constraints
- [ ] RLS policies applied (org_id isolation)
- [ ] Indexes on: org_id, connection_id, resolver_group_id, opened_at, state
- [ ] Query endpoints return paginated results with correct filtering
