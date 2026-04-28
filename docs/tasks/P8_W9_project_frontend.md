# P8-W9: Frontend – Project Pages

**Phase:** P8
**Module:** apps/frontend
**Effort:** ~3 MD
**Priority:** MEDIUM
**Depends on:** P8-W7, P8-W8, P8-W6

---

## Goal

New frontend pages for viewing ServiceNow project data and triggering project reports.

---

## New Pages

### 1. `ProjectsPage.tsx`
- Route: `/projects`
- Header: title + "Sync Projects" button (POST trigger)
- Filter bar: status, RAG, manager
- Project cards or table:
  - Name, status, % complete (progress bar), budget_util %, RAG badge, deadline
- Bulk action: "Generate Portfolio Report" → TextTemplateRenderPage with portfolio template pre-selected

### 2. `ProjectDetailPage.tsx`
- Route: `/projects/:projectId`
- Header: project name, RAG badge, manager, dates
- Metrics row: % complete, budget utilization, schedule variance, team size
- Tabs:
  - **Overview**: key metrics + phase timeline (Fluent UI ProgressIndicator)
  - **Tasks**: paginated task table (state, assigned_to, due_date, milestone flag)
  - **Budget**: bar chart (planned vs. actual vs. projected) + budget lines table
- Action buttons: "Status Report" (PPTX), "Budget Report" (Excel)
  → both navigate to TextTemplateRenderPage with project ID pre-filled

---

## Navigation

- Add "Projects" nav item in AppLayout under integrations section
- IntegrationPage: add "Projects" tab to ServiceNow integration section

---

## New API Client

### `apps/frontend/src/api/projects.ts`
- `getProjects(filters)` → `ProjectSummary[]`
- `getProjectDetail(id)` → `ProjectDetail`
- `triggerProjectSync(connectionId)` → void

---

## New Types (`packages/types/src/projects.ts`)

```typescript
export type RagStatus = 'GREEN' | 'AMBER' | 'RED';
export type ProjectStatus = 'OPEN' | 'IN_PROGRESS' | 'CLOSED' | 'CANCELLED';

export interface ProjectSummary {
  id: string;
  number: string;
  shortDescription: string;
  status: ProjectStatus;
  ragStatus: RagStatus;
  percentComplete: number;
  budgetUtilizationPct: number;
  scheduleVarianceDays: number;
  manager: string;
  startDate: string;
  endDate: string | null;
  totalBudget: number;
  actualCost: number;
  currency: string;
}

export interface ProjectTask {
  id: string;
  number: string;
  shortDescription: string;
  state: string;
  isMilestone: boolean;
  assignedTo: string | null;
  dueDate: string | null;
}

export interface ProjectBudgetLine {
  category: string;
  fiscalYear: string;
  plannedAmount: number;
  actualAmount: number;
}

export interface ProjectDetail extends ProjectSummary {
  tasks: ProjectTask[];
  budgetLines: ProjectBudgetLine[];
  phases: { name: string; status: string; startDate: string; endDate: string }[];
}
```

---

## Acceptance Criteria

- [ ] Projects page loads list with RAG color indicators
- [ ] Sync button triggers sync and refreshes list (React Query invalidation)
- [ ] Project detail shows timeline, tasks, and budget chart
- [ ] "Status Report" button → TextTemplateRenderPage with projectId pre-filled
- [ ] "Budget Report" → Excel download
