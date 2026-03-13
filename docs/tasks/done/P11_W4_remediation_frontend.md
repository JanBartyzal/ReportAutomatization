# P11 – Wave 4: Frontend Remediation (Frontend Agent)

**Phase:** P11 – Audit Remediation
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~13 MD
**Depends on:** P11-W2-003 (ESLint must be configured first)
**Source:** `docs/audit/fs-completeness-report.md`, `docs/audit/dod-compliance-report.md`

> Frontend test coverage, missing UI features, and API type alignment.

---

## P11-W4-001: Frontend Unit Tests (Vitest)

**Type:** Testing
**Effort:** 5 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-012

**Context:** Zero test files exist in `apps/frontend/`. DoD requires unit tests for all new logic.

**Tasks:**
- [ ] **Setup Vitest + React Testing Library:**
  - Add devDependencies: `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `jsdom`, `msw` (Mock Service Worker)
  - Create `apps/frontend/vitest.config.ts`:
    ```ts
    import { defineConfig } from 'vitest/config';
    import react from '@vitejs/plugin-react';
    export default defineConfig({
      plugins: [react()],
      test: {
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
        globals: true,
      },
      resolve: { alias: { '@': '/src' } },
    });
    ```
  - Create `apps/frontend/src/test/setup.ts` with jest-dom matchers
  - Add `"test": "vitest run"`, `"test:watch": "vitest"` to package.json scripts

- [ ] **Page component tests** (1 test file per critical page):
  - `src/pages/__tests__/FilesPage.test.tsx` — renders file list, filter interaction
  - `src/pages/__tests__/ReportsPage.test.tsx` — renders report list, status badges, filter
  - `src/pages/__tests__/DashboardListPage.test.tsx` — renders dashboards, public/private icons
  - `src/pages/__tests__/PeriodsPage.test.tsx` — renders period list, status colors
  - `src/pages/__tests__/TemplateListPage.test.tsx` — renders templates, scope badges

- [ ] **Hook tests** (core data hooks):
  - `src/hooks/__tests__/useFiles.test.ts` — useFiles, useUpload with mock API
  - `src/hooks/__tests__/useAuth.test.ts` — useAuth, role checking, org context
  - `src/hooks/__tests__/useGeneration.test.ts` — polling logic, status transitions

- [ ] **Component tests** (shared components):
  - `src/components/__tests__/ChartWrapper.test.tsx` — renders with data, handles empty
  - `src/components/__tests__/ErrorBoundary.test.tsx` — catches error, shows fallback
  - `src/components/__tests__/GenerateButton.test.tsx` — click triggers generation
  - `src/components/__tests__/GenerationProgress.test.tsx` — progress bar states

- [ ] **API mock setup with MSW:**
  - Create `src/test/mocks/handlers.ts` with default API mock responses
  - Create `src/test/mocks/server.ts` for test server setup
  - Mock MSAL token acquisition in test setup

**Files:**
- `apps/frontend/vitest.config.ts` (new)
- `apps/frontend/src/test/setup.ts` (new)
- `apps/frontend/src/test/mocks/` (new directory with handlers + server)
- ~12 new `*.test.tsx` / `*.test.ts` files

**AC:**
- [ ] `npm run test` passes
- [ ] 5 page tests + 3 hook tests + 4 component tests = 12 test files minimum
- [ ] MSW mocks API calls (no real HTTP requests in tests)
- [ ] MSAL is mocked (no real auth in tests)
- [ ] Coverage report generated

---

## P11-W4-002: Schema Mapping Editor UI

**Type:** Feature
**Effort:** 3 MD
**Priority:** HIGH
**Audit Ref:** P10-W2-016

**Context:** `SchemaMappingPage.tsx` is a placeholder ("implementation in progress"). `ExcelImportPage.tsx` has mock data for suggestions instead of connecting to the real backend API.

**Tasks:**
- [ ] **SchemaMappingPage.tsx** — implement full column mapping editor:
  - Left panel: source columns (from uploaded Excel file)
  - Right panel: target form fields (from selected form template)
  - Drag-and-drop or select-based mapping between source → target
  - Visual indicator for mapped/unmapped columns
  - Confidence score display for auto-suggested mappings
  - Save mapping as template for reuse
  - "Apply Mapping" button that calls backend `POST /api/templates/map/excel-to-form`

- [ ] **ExcelImportPage.tsx** — connect mock to real API:
  - Replace hardcoded `MappingSuggestion` data with API call to `GET /api/templates/suggest`
  - Pass `fileId` and `formId` to get real suggestions
  - Handle loading/error states

- [ ] **New components:**
  - `src/components/SchemaMapping/MappingEditor.tsx` — main mapping canvas
  - `src/components/SchemaMapping/ColumnCard.tsx` — draggable source/target column
  - `src/components/SchemaMapping/MappingLine.tsx` — visual connection between mapped columns
  - `src/components/SchemaMapping/SuggestionBadge.tsx` — shows confidence % for suggestions

- [ ] **New hook:**
  - `src/hooks/useSchemaMapping.ts` — manages mapping state, calls suggest/apply APIs

**Files:**
- `apps/frontend/src/pages/SchemaMappingPage.tsx` (rewrite)
- `apps/frontend/src/pages/ExcelImportPage.tsx` (update)
- `apps/frontend/src/components/SchemaMapping/` (new directory, 4 components)
- `apps/frontend/src/hooks/useSchemaMapping.ts` (new)

**AC:**
- [ ] User can view source columns from uploaded Excel
- [ ] User can map source columns to target form fields
- [ ] Auto-suggestions from backend displayed with confidence %
- [ ] Mapping can be saved and reused
- [ ] "Apply Mapping" calls backend API and shows result
- [ ] Handles loading, empty, and error states

---

## P11-W4-003: TypeScript Types vs OpenAPI Alignment

**Type:** Type Safety
**Effort:** 2 MD
**Priority:** MEDIUM
**Audit Ref:** P10-W2-023

**Context:** 6 of 9 TypeScript type files in `packages/types/` have structural mismatches with OpenAPI specs in `docs/api/`. The TS types have evolved beyond what specs document.

**Tasks:**
- [ ] **Audit and align each mismatched type file:**

  | Type File | Key Mismatches | Action |
  |-----------|---------------|--------|
  | `auth.ts` | Extra `COMPANY_ADMIN` role | Add COMPANY_ADMIN to OpenAPI spec (it's a real role) |
  | `files.ts` | `WorkflowStatus` schema differs | Align TS to match proto `WorkflowStatusResponse` |
  | `reports.ts` | Extra `COMPLETED` status, `scope`, `locked` fields | Update OpenAPI to include these fields (they exist in backend) |
  | `forms.ts` | `FormResponse.data` shape differs | Align TS `fields: FormFieldValue[]` vs spec `data: object` — standardize on typed array |
  | `admin.ts` | `id` vs `job_id` naming | Standardize to `id` in both spec and TS |
  | `query.ts` | `SlideContent`, `TableData` shapes differ | Align TS to match actual API response shapes |

- [ ] For each file:
  1. Read the corresponding OpenAPI spec
  2. Read the actual controller response DTOs in Java
  3. Update TS type OR OpenAPI spec to match the truth (Java controller)
  4. Verify frontend code still compiles with updated types

- [ ] **dashboard.ts** (partial match — TS superset): Add extra types to OpenAPI spec that exist in backend

**Files:**
- `packages/types/src/auth.ts`
- `packages/types/src/files.ts`
- `packages/types/src/reports.ts`
- `packages/types/src/forms.ts`
- `packages/types/src/admin.ts`
- `packages/types/src/query.ts`
- `packages/types/src/dashboard.ts`
- `docs/api/ms-auth-openapi.yaml`
- `docs/api/ms-ing-openapi.yaml`
- `docs/api/ms-lifecycle-openapi.yaml`
- `docs/api/ms-form-openapi.yaml`
- `docs/api/ms-admin-openapi.yaml`
- `docs/api/ms-qry-openapi.yaml`
- `docs/api/ms-dash-openapi.yaml`

**AC:**
- [ ] All 9 TS type files align with OpenAPI specs
- [ ] OpenAPI specs match actual Java controller DTOs
- [ ] Frontend compiles without type errors
- [ ] No runtime API response parsing failures

---

## P11-W4-004: Dashboard Date/Org Filters + Form Builder DnD

**Type:** Feature Enhancement
**Effort:** 3 MD
**Priority:** LOW
**Audit Ref:** P10-W2-030, P10-W2-032

**Context:** Dashboard editor lacks date/org filter dropdowns for widgets. Form builder has no drag-and-drop field reordering.

**Tasks:**
- [ ] **Dashboard editor filters (1 MD):**
  - Add date range picker to `DashboardEditorPage.tsx` widget configuration panel
  - Add organization selector dropdown (using existing `useOrganizations` hook)
  - Pass `date_from`, `date_to`, `org_id` as filter parameters to dashboard data API
  - Persist filter config in widget settings

- [ ] **Form builder drag & drop (2 MD):**
  - Add `@dnd-kit/core` + `@dnd-kit/sortable` to package.json
  - Create `src/components/FormBuilder/DraggableField.tsx` — sortable field wrapper
  - Create `src/components/FormBuilder/FieldPalette.tsx` — available field types to drag in
  - Create `src/components/FormBuilder/FormCanvas.tsx` — drop zone with current form layout
  - Integrate with existing form CRUD API:
    - On drag reorder: update field `order` values
    - On drag from palette: create new field with next order value
    - Auto-save after each drag operation (existing auto-save endpoint)

**Files:**
- `apps/frontend/src/pages/DashboardEditorPage.tsx` (update)
- `apps/frontend/package.json` (add @dnd-kit deps)
- `apps/frontend/src/components/FormBuilder/` (new directory, 3 components)
- `apps/frontend/src/pages/FormEditorPage.tsx` or equivalent (integrate DnD)

**AC:**
- [ ] Dashboard widgets can be filtered by date range and organization
- [ ] Form fields can be reordered by dragging
- [ ] New fields can be dragged from palette into form
- [ ] Drag changes trigger auto-save

---

## Summary

| Task | Effort | Priority |
|------|--------|----------|
| P11-W4-001: Frontend Unit Tests (Vitest) | 5 MD | HIGH |
| P11-W4-002: Schema Mapping Editor UI | 3 MD | HIGH |
| P11-W4-003: TypeScript Types Alignment | 2 MD | MEDIUM |
| P11-W4-004: Dashboard Filters + Form DnD | 3 MD | LOW |
| **Total** | **13 MD** | |
