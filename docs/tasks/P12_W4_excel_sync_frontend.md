# P12 – Wave 4: Live Excel Export & External Sync – Frontend (Flash/MiniMax)

**Phase:** P12 – Live Excel Export & External Sync
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD
**Depends on:** P12-W1 (APIs available), P12-W2 (types defined)
**Feature Set:** FS27

> React UI for Export Flow management, execution monitoring, and manual export.

---

## UX/UI Design System (povinne)

Vsechny komponenty MUSI dodrZovat:
- [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) — Crimson brand `#C4314B`
- [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) — layout, typografie, spacing, elevace
- [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) — atomicke komponenty
- Fluent UI v9 komponenty, `makeStyles()` pro styling, zadne hardcoded barvy
- Dark mode support pres FluentProvider

---

## P12-W4-001: Export Flows List Page

**Type:** Frontend Page
**Effort:** 3 MD
**File:** `apps/frontend/src/pages/ExportFlowsPage.tsx`

**Tasks:**
- [ ] **Route registration**: `/export-flows` in React Router
- [ ] **Navigation**: Add "Export Flows" item to sidebar navigation (icon: `ArrowExportRegular`)
- [ ] **Export Flow List View**:
  - FluentUI DataGrid with columns:
    - Name (link to detail)
    - Target Type (badge: SHAREPOINT / LOCAL_PATH)
    - Target Sheet
    - Trigger Type (badge: AUTO / MANUAL)
    - Last Export (relative time, e.g., "2 hours ago")
    - Last Status (StatusBadge: SUCCESS=green, FAILED=red, RUNNING=blue, PENDING=gray)
    - Actions (Edit, Execute, Delete)
  - Sorting by name, last export time, status
  - Filter by target type, trigger type, status
  - Empty state: illustration + "No export flows configured" + "Create Export Flow" button
- [ ] **Quick Actions**:
  - "Export Now" button per row → confirm dialog → `POST /api/export-flows/{id}/execute`
  - Loading spinner during execution, auto-refresh status
  - "Delete" button → confirm dialog → soft delete
- [ ] **Toolbar**:
  - "New Export Flow" button (opens create dialog)
  - Refresh button
  - Search input (filter by name)
- [ ] **Auto-refresh**: Poll execution status every 10s when any flow is RUNNING

**AC:**
- [ ] Export Flows page accessible from sidebar navigation
- [ ] List displays all flows for current org with correct status badges
- [ ] "Export Now" triggers execution and status updates in real-time
- [ ] Dark mode renders correctly
- [ ] Empty state shown when no flows exist

---

## P12-W4-002: Export Flow Create/Edit Dialog

**Type:** Frontend Component
**Effort:** 3 MD
**File:** `apps/frontend/src/components/ExportFlowDialog.tsx`

**Tasks:**
- [ ] **Dialog layout** (FluentUI Dialog, max-width 720px):
  - Step 1: Basic Info
    - Name (required, max 255 chars)
    - Description (optional, textarea)
  - Step 2: Data Source
    - SQL Query editor (reuse from Dashboard FS11 if available):
      - Monaco editor or textarea with syntax highlighting
      - "Test Query" button → `POST /api/export-flows/{id}/test` → preview table below
      - Preview: DataGrid showing first 100 rows, column count, total row count
    - Or UI query builder (GROUP BY, filters) – reuse dashboard components
  - Step 3: Target Configuration
    - Target Type: radio group (SharePoint / Local Path)
    - **SharePoint fields** (shown when SharePoint selected):
      - Site URL (text input with URL validation)
      - Drive Name (text input, default "Documents")
      - File Path within drive (text input)
      - Tenant ID, Client ID (text inputs)
      - Secret KeyVault Reference (text input, masked)
      - "Test Connection" button → validates SharePoint access
    - **Local Path fields** (shown when Local Path selected):
      - File path (text input with UNC path hint)
      - Info callout: "Ensure the path is accessible via Docker volume mount"
  - Step 4: Sheet & Naming
    - Target Sheet Name (required, max 31 chars, validation: no `[]:*?/\`)
    - File Naming: radio group (Custom Name / Auto from Batch Name)
    - Custom File Name (shown when Custom selected, with `.xlsx` suffix auto-added)
  - Step 5: Trigger
    - Trigger Type: radio group (Automatic on data import / Manual only)
    - Trigger Filter (shown when Auto selected):
      - Source Type filter (multi-select: PPTX, EXCEL, PDF, CSV)
      - Batch filter (optional batch name pattern)
- [ ] **Form validation**:
  - Required fields highlighted on submit
  - Sheet name character validation (inline error)
  - SQL syntax validation via dry-run API
  - SharePoint URL format validation
- [ ] **Edit mode**: Pre-populate form with existing flow data
- [ ] **Submit**: `POST /api/export-flows` (create) or `PUT /api/export-flows/{id}` (edit)
- [ ] **Success feedback**: Toast notification "Export Flow created/updated successfully"

**AC:**
- [ ] Create dialog produces valid Export Flow via API
- [ ] Edit dialog pre-fills all fields correctly
- [ ] SQL preview shows query results inline
- [ ] SharePoint connection test works from dialog
- [ ] Validation prevents invalid sheet names and empty required fields
- [ ] Dark mode compatible

---

## P12-W4-003: Export Execution History Panel

**Type:** Frontend Component
**Effort:** 1 MD
**File:** `apps/frontend/src/components/ExportExecutionHistory.tsx`

**Tasks:**
- [ ] **Execution History View** (accessed from Export Flow detail or list row expansion):
  - FluentUI DataGrid with columns:
    - Started At (datetime, sorted DESC)
    - Completed At (datetime or "—" if running)
    - Duration (calculated, e.g., "12s")
    - Trigger Source (badge: AUTO / MANUAL)
    - Rows Exported (number)
    - Status (StatusBadge)
    - Target Path Used
  - Expandable row detail:
    - Error message (shown for FAILED executions, red callout)
    - Trigger event ID (link to source batch/file if available)
  - Pagination (20 per page)
- [ ] **Live status updates**: Auto-refresh when any execution is RUNNING (poll every 5s)
- [ ] **Empty state**: "No exports executed yet" with "Run Now" button

**AC:**
- [ ] History shows all executions for selected flow
- [ ] FAILED executions display error message in expandable detail
- [ ] Running executions update status automatically
- [ ] Duration calculated correctly

---

## P12-W4-004: API Client & React Query Hooks

**Type:** Frontend Infrastructure
**Effort:** 1 MD
**Files:**
- `apps/frontend/src/api/exportFlows.ts`
- `apps/frontend/src/hooks/useExportFlows.ts`

**Tasks:**
- [ ] **API client** (`exportFlows.ts`):
  ```typescript
  getExportFlows(params?: ListParams): Promise<PaginatedResponse<ExportFlowDefinition>>
  getExportFlow(id: string): Promise<ExportFlowDefinition>
  createExportFlow(data: ExportFlowCreateRequest): Promise<ExportFlowDefinition>
  updateExportFlow(id: string, data: ExportFlowUpdateRequest): Promise<ExportFlowDefinition>
  deleteExportFlow(id: string): Promise<void>
  executeExportFlow(id: string): Promise<{ executionId: string }>
  testExportFlow(id: string): Promise<ExportFlowTestResponse>
  getExecutionHistory(flowId: string, params?: ListParams): Promise<PaginatedResponse<ExportFlowExecution>>
  ```
  - Axios instance with auth interceptor (reuse existing pattern)
  - Base URL: `${VITE_API_BASE}/api/export-flows`
- [ ] **React Query hooks** (`useExportFlows.ts`):
  - `useExportFlows(params)` – list with pagination
  - `useExportFlow(id)` – single flow detail
  - `useCreateExportFlow()` – mutation with cache invalidation
  - `useUpdateExportFlow()` – mutation with cache invalidation
  - `useDeleteExportFlow()` – mutation with cache invalidation
  - `useExecuteExportFlow()` – mutation (returns executionId)
  - `useTestExportFlow()` – mutation (returns preview data)
  - `useExecutionHistory(flowId, params)` – list with auto-refresh when RUNNING
  - Query keys: `['export-flows']`, `['export-flows', id]`, `['export-flows', id, 'executions']`
  - Stale time: 30s for list, 10s for detail (to catch status updates)
- [ ] **TypeScript types** (import from `packages/types/` or define locally):
  - `ExportFlowDefinition`, `ExportFlowExecution`
  - `ExportFlowCreateRequest`, `ExportFlowUpdateRequest`
  - `ExportFlowTestResponse`
  - `TargetType`, `FileNaming`, `TriggerType`, `ExecutionStatus` enums
- [ ] **Feature flag**: Gate export flows UI behind `VITE_EXPORT_FLOWS_ENABLED` env var

**AC:**
- [ ] All API calls work with correct auth headers
- [ ] React Query cache invalidated on mutations
- [ ] Auto-refresh works for running executions
- [ ] Feature flag hides navigation item when disabled
