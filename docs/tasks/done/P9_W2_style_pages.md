# P9 – Wave 2: Frontend Style Unification – Page Migration (Sonnet)

**Phase:** P9 – Frontend Style Unification
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~15 MD
**Depends on:** P9-W1 (shared components, theme system ready)

> Migrating all existing pages to use shared components and unified design tokens.

---

## P9-W2-001: Core Pages Migration (Auth, Files, Dashboard)

**Type:** Page Refactoring
**Effort:** 4 MD

**Pages:**
- [ ] **LoginPage.tsx** — MSAL login flow:
  - Hero gradient from JSON config
  - Brand logo with Crimson primary
  - Fluent Button with brand tokens
- [ ] **FilesPage.tsx** — File upload & list:
  - Replace inline DataGrid with shared `DataTable`
  - Replace inline status badges with shared `StatusBadge`
  - Upload dropzone styled with design tokens
  - PageHeader component
- [ ] **FileDetailPage.tsx** — File detail view:
  - Tab navigation with Fluent tokens
  - Slide viewer with consistent card styling
  - Metadata display with `ContentCard`
- [ ] **DashboardListPage.tsx** — Dashboard listing:
  - Shared `DataTable` for dashboard list
  - Card grid layout per design system (12-col)
- [ ] **DashboardViewerPage.tsx** — Dashboard rendering:
  - Charts wrapped in `ChartWrapper`
  - KPI cards using shared `KpiCard`
  - Filters styled with shared form components
- [ ] **DashboardEditorPage.tsx** — Dashboard editor:
  - Form controls using shared `FormField`, `FormSelect`
  - SQL editor with monospace font from tokens
  - Preview area with `ContentCard`
- [ ] Remove all inline `style={}` for colors/spacing (replace with `makeStyles`)
- [ ] Remove any Tailwind utility classes (replace with Fluent `makeStyles`)
- [ ] Remove any hardcoded hex colors

**AC:**
- [ ] All core pages use shared components exclusively
- [ ] No inline color/spacing styles remain
- [ ] Dark mode renders correctly on all pages

---

## P9-W2-002: Reporting Pages Migration

**Type:** Page Refactoring
**Effort:** 4 MD

**Pages:**
- [ ] **ReportsPage.tsx** — Report listing with status matrix:
  - Shared `StatusBadge` for all report states
  - Shared `DataTable` with sorting/pagination
  - Bulk action buttons with consistent button styles
- [ ] **ReportDetailPage.tsx** — Report detail:
  - Timeline view with status colors from JSON
  - Comment section with consistent card styling
  - Action buttons per role (brand color for primary CTA)
- [ ] **PeriodsPage.tsx** — Period management:
  - Period cards with deadline indicators
  - Completion percentage with design system progress bars
  - Status colors from JSON config
- [ ] **PeriodDetailPage.tsx** — Period detail:
  - Matrix dashboard with shared `StatusBadge`
  - Charts wrapped in `ChartWrapper`
  - Comparison view with consistent data display
- [ ] **MatrixDashboard.tsx** — Organization × Status matrix:
  - Cell colors from `statusColors.ts`
  - Table header from design system
  - Responsive layout via `useBreakpoint()`
- [ ] **HoldingAdminOverviewPage.tsx** — Holding admin view:
  - KPI cards using shared `KpiCard`
  - Action menu with Fluent tokens

**AC:**
- [ ] All reporting pages use unified status colors from JSON
- [ ] Matrix dashboard cells colored consistently
- [ ] Period comparison charts use project chart palette

---

## P9-W2-003: Form Pages Migration

**Type:** Page Refactoring
**Effort:** 3.5 MD

**Pages:**
- [ ] **FormsListPage.tsx** — Form listing:
  - Shared `DataTable` with status badges
  - Filter controls with shared form components
- [ ] **FormEditorPage.tsx** — Form builder:
  - Drag & drop area with design system borders/shadows
  - Field type palette with Fluent icons
  - Preview panel with `ContentCard`
  - All form controls using shared form components
- [ ] **FormFillerPage.tsx** — Form filling:
  - All inputs via shared `FormField` / `FormInput`
  - Auto-save indicator styled consistently
  - Section headers with typography tokens
  - Validation errors per design system (all shown at once)
- [ ] **FormAssignmentPage.tsx** — Form assignment:
  - Organization selector with consistent dropdown
  - Assignment grid with shared table
- [ ] **ExcelImportPage.tsx** — Excel import:
  - Column mapping UI with consistent card styling
  - Preview table with shared `DataTable`
  - Confidence indicators with semantic colors

**AC:**
- [ ] Form builder uses shared drag & drop styling
- [ ] Form filler uses shared form components for all field types
- [ ] Excel import preview table matches design system

---

## P9-W2-004: Admin & Settings Pages Migration

**Type:** Page Refactoring
**Effort:** 2 MD

**Pages:**
- [ ] **AdminPage.tsx** — Admin dashboard:
  - Tab navigation with Fluent tokens
  - Settings panels with `ContentCard`
  - Role management table with shared `DataTable`
- [ ] **SchemaMappingPage.tsx** — Schema mapping:
  - Mapping editor with consistent form styling
  - Learning suggestions with info semantic color
- [ ] **NotificationSettingsPage.tsx** — Notification preferences:
  - Toggle switches with Fluent tokens
  - Category grouping with `FormSection`
- [ ] **BatchGenerationPage.tsx** — Batch PPTX generation:
  - Progress indicators with brand color
  - Report list with shared components
- [ ] **GeneratedReportsListPage.tsx** — Generated reports:
  - Download links with consistent icon + text styling
  - Status badges from shared component
- [ ] **HealthDashboardPage.tsx** — System health:
  - Service status cards with semantic colors
  - Metric charts with `ChartWrapper`

**AC:**
- [ ] All admin pages use shared component library
- [ ] Consistent look and feel across admin section
- [ ] Health dashboard uses semantic colors for status indicators

---

## P9-W2-005: Local & Comparison Pages Migration

**Type:** Page Refactoring
**Effort:** 1.5 MD

**Pages:**
- [ ] **LocalDashboardPage.tsx** — Subsidiary dashboard:
  - KPI cards using shared `KpiCard`
  - Charts wrapped in `ChartWrapper`
  - Local vs central data toggle with consistent styling
- [ ] **ComparisonPage.tsx** — Period comparison:
  - Comparison table with delta values (green/red per semantic colors)
  - Charts with consistent chart palette
- [ ] **NotFoundPage.tsx** — 404 page:
  - Brand-colored illustration
  - Back to home button with brand CTA

**AC:**
- [ ] All remaining pages migrated to unified design system
- [ ] Zero pages with ad-hoc inline styling

---
