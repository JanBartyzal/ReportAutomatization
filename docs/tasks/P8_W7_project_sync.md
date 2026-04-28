# P8-W7: ServiceNow Project Sync

**Phase:** P8
**Module:** engine-integrations:servicenow (extension) + engine-data (migrations)
**Effort:** ~5 MD
**Priority:** MEDIUM
**Depends on:** P8-W1 (ServiceNow client patterns)

---

## Goal

Extend ServiceNow integration to load project data: projects, tasks, phases, budget plans.
Store to dedicated typed tables in engine-data. Calculate derived metrics (RAG status,
budget utilization, schedule variance).

---

## ServiceNow Tables

| SN Table | Entity | Key fields |
|---|---|---|
| `pm_project` | Project | sys_id, number, short_description, status, phase, manager, start_date, end_date, percent_complete, total_budget, actual_cost, projected_cost, department |
| `pm_project_task` | Task | sys_id, number, short_description, state, milestone, due_date, assigned_to, parent, project |
| `pm_project_budget_plan` | Budget | sys_id, project, planned_amount, actual_amount, category, fiscal_year |
| `pm_project_phase` | Phase | sys_id, project, name, status, start_date, end_date |

---

## Derived Metrics

Calculated and stored alongside raw data:

```
budget_utilization_pct   = actual_cost / total_budget * 100
schedule_variance_days   = DATEDIFF(projected_end, planned_end)
rag_status               = GREEN / AMBER / RED (based on budget + schedule)
milestone_completion_rate = completed_milestones / total_milestones * 100
cost_forecast_accuracy   = projected_cost / total_budget
```

RAG thresholds (configurable in ProjectSyncConfigEntity):
- GREEN: budget_util < 80% AND schedule_var <= 0
- AMBER: budget_util 80-95% OR schedule_var 1-14 days
- RED: budget_util > 95% OR schedule_var > 14 days

---

## New Entities

### `ProjectSyncConfigEntity.java`
```
id, connectionId, orgId,
syncScope (ALL / ACTIVE_ONLY / BY_MANAGER),
filterManagerEmails (TEXT – comma-separated),
budgetCurrency (VARCHAR 3),
ragAmberBudgetThreshold (NUMERIC, default 80),
ragRedBudgetThreshold (NUMERIC, default 95),
ragAmberScheduleThreshold (int days, default 1),
ragRedScheduleThreshold (int days, default 14),
syncEnabled, lastSyncAt, createdAt, updatedAt
```

---

## New Service

### `ProjectFetchService.java`
1. Load `ProjectSyncConfigEntity` by connectionId
2. Fetch `pm_project` with filter (status=active or manager in list)
3. For each project: fetch tasks, phases, budget plans
4. Calculate derived metrics
5. Upsert to `snow_projects`, `snow_project_tasks`, `snow_project_budgets`

---

## Migrations

- `V2_0_2__snow_project_sync.sql` – engine-integrations: project_sync_config table
- `V12_0_3__snow_project_tables.sql` – engine-data: snow_projects, snow_project_tasks, snow_project_budgets

---

## REST Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/integrations/servicenow/{connId}/project-sync` | Get config |
| POST | `/api/integrations/servicenow/{connId}/project-sync` | Create/update config |
| POST | `/api/integrations/servicenow/{connId}/project-sync/trigger` | Manual sync |
| GET | `/api/data/snow/projects` | List projects (filterable by rag, status, manager) |
| GET | `/api/data/snow/projects/{id}` | Project detail with tasks + budget |

---

## Acceptance Criteria

- [ ] Project sync config created with custom RAG thresholds
- [ ] Manual sync trigger fetches projects + tasks + budgets
- [ ] Derived metrics (RAG, budget_util, schedule_var) calculated and stored
- [ ] Project list endpoint returns paginated results with RAG indicators
- [ ] Project detail endpoint returns full tree (project + tasks + budget lines)
