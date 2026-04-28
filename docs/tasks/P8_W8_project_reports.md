# P8-W8: Project Report Templates

**Phase:** P8
**Module:** engine-reporting:text-template (seeds) + Named Queries seeds
**Effort:** ~4 MD
**Priority:** MEDIUM
**Depends on:** P8-W4, P8-W5, P8-W7

---

## Goal

Seed pre-built project report templates into the Text Template + Named Query catalog.
These replace ServiceNow's weak native reporting with polished, branded outputs.

---

## Report Templates

### 1. Project Portfolio Overview (PPTX, 6 slides)
**Audience:** Management, PMO
- Slide 1: Cover + portfolio health snapshot (total projects, RAG distribution)
- Slide 2: Project status matrix (table: project, %, budget_util, RAG, ETA)
- Slide 3: Budget utilization bar chart (all active projects)
- Slide 4: Schedule variance chart (planned vs. projected end dates)
- Slide 5: Milestone summary (upcoming milestones next 30 days)
- Slide 6: Projects at risk (RED + AMBER with details)

**Named Queries (seeds):**
- `project_portfolio_summary` – project list with RAG, budget_util, schedule_var
- `project_rag_distribution` – COUNT GROUP BY rag_status
- `project_budget_chart` – name, total_budget, actual_cost, projected_cost
- `project_upcoming_milestones` – milestones in next 30 days

### 2. Single Project Status Report (PPTX, 5 slides)
**Audience:** Project manager, sponsor
- Slide 1: Cover + project metadata
- Slide 2: Key metrics (% complete, budget util, schedule var, RAG, team size)
- Slide 3: Milestone timeline with completion indicators
- Slide 4: Budget waterfall chart (planned → actual → projected)
- Slide 5: Open tasks + risks

**Named Queries (seeds):**
- `project_single_detail` – parameterized by :projectId
- `project_tasks_open` – parameterized by :projectId
- `project_budget_waterfall` – planned/actual/projected by category

### 3. Budget Variance Report (Excel, 4 sheets)
**Audience:** Finance, PMO
- Sheet 1: Summary – all projects, budget variance, utilization %
- Sheet 2: Budget by category/phase (pivot-style)
- Sheet 3: Cost trend over time
- Sheet 4: Raw data

---

## Migration

- `V12_0_4__qry_seed_project_named_queries.sql` (engine-data – seeds named queries)
- `V6_0_003__seed_project_templates.sql` (engine-reporting – seeds text templates)

---

## Acceptance Criteria

- [ ] Portfolio Overview template visible in catalog, renderable with no params
- [ ] Single Project template renders with valid projectId param
- [ ] Budget Variance Excel generated with 4 sheets
- [ ] Named queries return correct data from snow_projects / snow_project_tasks / snow_project_budgets
