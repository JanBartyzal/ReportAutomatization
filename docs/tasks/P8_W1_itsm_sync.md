# P8-W1: ITSM Resolver Groups & Sync Service

**Phase:** P8 – ServiceNow ITSM & Unified Reporting
**Module:** engine-integrations:servicenow
**Effort:** ~4 MD
**Priority:** HIGH
**Depends on:** existing ServiceNowClient, KeyVaultService

---

## Goal

Extend ServiceNow integration with support for **resolver groups** (SN `assignment_group`).
Load Incidents (`incident`) and Requests (`sc_request`, `sc_req_item`) filtered by
configured resolver groups. Calculate ITSM KPIs. Store to dedicated typed tables via sink-tbl.

---

## ServiceNow Tables

| SN Table | Data type | Key fields |
|---|---|---|
| `incident` | INCIDENT | number, short_description, state, priority, urgency, assignment_group, opened_at, resolved_at, sla_breach, sla_due |
| `sc_request` | REQUEST | number, short_description, state, approval, stage, requested_for, opened_at, due_date, assignment_group |
| `sc_req_item` | REQUEST_ITEM | number, short_description, state, request, cat_item, assignment_group, due_date |
| `sc_task` | TASK | number, short_description, state, assignment_group, due_date |

---

## New Files

### Entities / DTOs
- `model/entity/ResolverGroupConfigEntity.java`
  - Fields: id, connectionId, orgId, groupSysId, groupName, dataTypes (JSONB), syncEnabled
- `model/dto/ResolverGroupDto.java`
- `model/dto/CreateResolverGroupRequest.java`
- `model/dto/ItsmSummaryDto.java` – aggregated KPI response

### Repository
- `repository/ResolverGroupConfigRepository.java`

### Service
- `service/ItsmSyncService.java`
  - Fetches by `sysparm_query=assignment_group.sys_id=<id>`
  - Adds `sysparm_fields` param to request only needed columns
  - Calculates: `resolutionTimeHours`, `isSlaBbreached`, `ageDays`
  - Stores to `snow_incidents` / `snow_requests` via Dapr → ms-sink-tbl

### Controller extensions
- `controller/IntegrationController.java` – add resolver group CRUD + itsm-summary endpoint

### Migration
- `V2_0_1__snow_resolver_groups.sql` (engine-integrations migrations)

---

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/integrations/servicenow/{connId}/resolver-groups` | List groups |
| POST | `/api/integrations/servicenow/{connId}/resolver-groups` | Create group |
| DELETE | `/api/integrations/servicenow/{connId}/resolver-groups/{id}` | Remove group |
| POST | `/api/integrations/servicenow/{connId}/resolver-groups/{id}/sync` | Manual sync trigger |
| GET | `/api/integrations/servicenow/{connId}/itsm-summary` | KPI aggregation |

---

## Acceptance Criteria

- [ ] Admin configures resolver group with group_sys_id from SN → saved to DB
- [ ] Manual sync trigger → incidents/requests for that group fetched and stored
- [ ] ITSM summary endpoint returns: open_count, resolved_count, sla_breach_pct, avg_resolution_hours
- [ ] Incremental sync respects `sys_updated_on` filter
- [ ] Credentials never in DB plaintext (KeyVault refs only)
