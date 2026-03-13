# P9 – Wave 1: Frontend Style Unification – Token System & Architecture (Opus)

**Phase:** P9 – Frontend Style Unification
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~18 MD
**Depends on:** P8 (consolidation complete), docs/UX-UI/* (design system defined)

> Refactoring all frontend pages to use a unified design token system with JSON-configurable colors. Eliminates per-page ad-hoc styling in favor of the project design system.

---

## Design System References

| Document | Content |
|----------|---------|
| [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) | Brand palette (Crimson `#C4314B`), status colors, chart palette |
| [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) | Full design system: layout, typography, spacing, elevation, forms, dark mode, a11y |
| [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) | Component library spec |
| [`docs/UX-UI/04-figma-pages.md`](../UX-UI/04-figma-pages.md) | Page wireframes |

---

## P9-W1-001: JSON Theme Configuration System

**Type:** Architecture
**Effort:** 5 MD
**File:** `apps/frontend/src/theme/`

**Tasks:**
- [ ] **`theme-config.json`** — external JSON config for color overrides:
  ```json
  {
    "brand": {
      "10": "#0D0206", "20": "#23080F", "30": "#3A0D19",
      "40": "#521324", "50": "#6B1830", "60": "#841E3C",
      "70": "#9D2444", "80": "#B32A48", "90": "#C4314B",
      "100": "#CF4D62", "110": "#D96A79", "120": "#E38790",
      "130": "#EDA4A9", "140": "#F5BFC2", "150": "#F9D8DA",
      "160": "#FDF0F1"
    },
    "semantic": {
      "success": { "foreground": "#107C10", "background": "#DFF6DD" },
      "danger": { "foreground": "#D13438", "background": "#FDE7E9" },
      "warning": { "foreground": "#D83B01", "background": "#FDEBE2" },
      "info": { "foreground": "#C4314B", "background": "#FDF0F1" }
    },
    "status": {
      "DRAFT": { "bg": "#F3F2F1", "text": "#616161" },
      "SUBMITTED": { "bg": "#FEF3C7", "text": "#92400E" },
      "IN_REVIEW": { "bg": "#E0F2FE", "text": "#0369A1" },
      "APPROVED": { "bg": "#DFF6DD", "text": "#107C10" },
      "REJECTED": { "bg": "#FDE7E9", "text": "#D13438" },
      "OVERDUE": { "bg": "#FDEBE2", "text": "#D83B01" }
    },
    "chart": [
      "#C4314B", "#107C10", "#0078D4", "#D83B01",
      "#6D28D9", "#008272", "#E3008C", "#986F0B"
    ],
    "heroGradient": {
      "from": "#6B1830",
      "to": "#23080F",
      "angle": 135
    }
  }
  ```
- [ ] **`ThemeProvider.tsx`** — Fluent UI theme provider with JSON config:
  - Load `theme-config.json` at app startup (or embed as build-time import)
  - Convert JSON brand values to Fluent `BrandVariants`
  - `createLightTheme(brand)` / `createDarkTheme(brand)` from Fluent UI
  - Expose via React context: `useAppTheme()` hook
  - Allow runtime theme switching (light/dark/system)
- [ ] **`tokens.ts`** — design tokens derived from JSON config:
  - Spacing tokens (from design system: 2, 4, 8, 16, 24, 32, 40, 48 px)
  - Typography tokens (Inter font stack, 7-level scale)
  - Elevation tokens (5 shadow levels)
  - Border radius tokens (6 variants)
  - Motion tokens (4 timing levels)
  - All derived from Fluent theme + project overrides
- [ ] **`statusColors.ts`** — refactored to read from JSON config:
  - `getStatusColor(status: ReportStatus)` → `{ bg, text }`
  - Used by all status badges, timeline indicators, matrix cells
- [ ] **`chartColors.ts`** — chart palette from JSON config:
  - `getChartColor(index: number)` → hex color string
  - CSS custom properties (`--chart-1` through `--chart-8`) injected into `:root`
- [ ] **CSS custom properties injection**:
  - On theme load, inject all tokens as CSS variables for non-React consumers
  - `document.documentElement.style.setProperty('--brand-90', config.brand['90'])`
- [ ] Unit tests for theme config loading, token generation, dark mode switching

**AC:**
- [ ] Changing `theme-config.json` brand.90 from `#C4314B` to `#0078D4` recolors entire app
- [ ] Dark mode automatically derives correct colors from brand palette
- [ ] All status badges, charts, and semantic colors configurable via JSON
- [ ] No hardcoded hex values remain in theme system

---

## P9-W1-002: Shared Component Library – Atoms

**Type:** Component Refactoring
**Effort:** 5 MD
**File:** `apps/frontend/src/components/shared/`

**Tasks:**
- [ ] **StatusBadge** — unified status indicator:
  - Props: `status: ReportStatus`, `size: 'sm' | 'md'`
  - Colors from `statusColors.ts` (JSON-driven)
  - Used across: ReportsPage, MatrixDashboard, PeriodsPage, FormsList
  - Replace all inline badge implementations
- [ ] **KpiCard** — standardized KPI display:
  - Props: `title, value, trend, subtitle, sparklineData?`
  - Layout per design system: Title 3 header, Display value, trend icon, sparkline
  - Padding: `spacingL` (24px), Shadow: Level 1, Hover: Level 2
  - Used across: DashboardPage, LocalDashboardPage, HoldingAdminOverviewPage
- [ ] **DataTable** — wrapper around Fluent DataGrid:
  - Consistent zebra striping (`Background1` / `Background2`)
  - Header: `Background2`, Title 3 weight 600
  - Row height: 40px min, Cell padding per design system
  - Pagination: right-aligned Fluent Pagination
  - Sort icons: `ArrowSort24Regular`
  - Used across: all pages with data grids
- [ ] **PageHeader** — consistent page title area:
  - Breadcrumb + Title (Title 1) + optional action buttons
  - Spacing per design system: `spacingXXL` top, `spacingXL` bottom
- [ ] **EmptyState** — no-data placeholder:
  - Fluent icon + Title 3 message + Body 1 description + optional CTA button
- [ ] **LoadingSkeleton** — unified loading state:
  - `aria-busy="true"`, pulse animation (2s infinite)
  - Variants: card, table row, full page
- [ ] **ConfirmDialog** — standardized confirmation modal:
  - Fluent Dialog with Level 4 shadow, `radiusLg` border radius
  - Danger variant (red CTA) for destructive actions
- [ ] All components use `makeStyles()` from Fluent, no Tailwind, no inline styles
- [ ] All components support dark mode via FluentProvider tokens
- [ ] Storybook stories for each component (if Storybook is set up)

**AC:**
- [ ] Shared components used consistently across all pages
- [ ] No per-page reimplementation of badges, KPI cards, tables
- [ ] All components render correctly in light and dark mode
- [ ] All components pass WCAG 2.1 AA contrast checks

---

## P9-W1-003: Glassmorphism & Layout Components

**Type:** Component Refactoring
**Effort:** 3 MD
**File:** `apps/frontend/src/components/layout/`

**Tasks:**
- [ ] **TopNav** — glassmorphism navigation bar:
  - `rgba(20, 20, 20, 0.8)` + `backdrop-filter: blur(20px)`
  - Height: 64px, fixed position, Level 2 shadow
  - Logo, navigation items, theme toggle, user menu
- [ ] **Sidebar** — solid background navigation:
  - Expanded: 260px, Collapsed: 60px
  - Transition: 200ms ease-in-out
  - Fluent icons: `24Regular` for nav, `*Filled` for active
  - Section headers: Caption (size 100), uppercase, `0.05em` letter-spacing
  - Solid background (NOT glassmorphism per design system rules)
- [ ] **AppShell** — main layout wrapper:
  - Grid: 12 columns, gap 24px, max-width 1400px centered
  - Responsive via `useBreakpoint()` hook (not CSS media queries)
  - Breakpoints: xs (0-639), sm (640-767), md (768-1023), lg (1024-1279), xl (1280+)
- [ ] **ContentCard** — generic content card wrapper:
  - `radiusMd` (8px), `Stroke1` 1px border, `spacingL` padding
  - Shadow: Level 1, Hover: Level 2 (transition 200ms)
- [ ] **ModalOverlay** — glassmorphism overlay:
  - `rgba(0, 0, 0, 0.4)` + `blur(4px)` on backdrop
  - Content: Level 4 shadow, `radiusLg`
- [ ] **`useBreakpoint()` hook**:
  - Returns current breakpoint name and screen width
  - `matchMedia` based, SSR-safe
  - Replaces all scattered CSS media queries

**AC:**
- [ ] Consistent layout across all pages
- [ ] Glassmorphism applied only where design system allows
- [ ] Responsive behavior works at all breakpoints
- [ ] Sidebar collapse/expand animated correctly

---

## P9-W1-004: Form Components Standardization

**Type:** Component Refactoring
**Effort:** 3 MD
**File:** `apps/frontend/src/components/forms/`

**Tasks:**
- [ ] **FormField** — wrapper around Fluent `<Field>`:
  - Label always above input (per design system)
  - Required marker: `*` in Red
  - Hint text below input: Body 2, `Foreground3`
  - Error message below: Body 2, Red, with `ErrorCircle16Regular` icon
  - Field spacing: `spacingM` (16px) vertical
- [ ] **FormInput** — text/number input with consistent states:
  - Default border: `Stroke1`
  - Hover: `Brand80`, Focus: `Brand90` 2px
  - Error: Red border, Disabled: 50% opacity
- [ ] **FormSelect** — dropdown with consistent styling
- [ ] **FormDatePicker** — date picker with project tokens
- [ ] **FormTextarea** — multi-line with same state rules
- [ ] **FormSection** — visual grouping with title and description
- [ ] Refactor all existing form pages to use these components:
  - FormEditorPage, FormFillerPage, ExcelImportPage, NotificationSettingsPage
  - IntegrationPage config dialog, PromotionPage DDL editor
- [ ] Validation rendering: all errors shown at once (not one-by-one)

**AC:**
- [ ] All forms in the app use shared form components
- [ ] Consistent focus rings, error states, label positioning
- [ ] Dark mode correct for all form states
- [ ] Validation shows all errors simultaneously

---

## P9-W1-005: Chart & Data Visualization Standardization

**Type:** Component Refactoring
**Effort:** 2 MD
**File:** `apps/frontend/src/components/charts/`

**Tasks:**
- [ ] **ChartWrapper** — theme-aware chart container:
  - Injects chart palette from JSON config
  - Handles dark mode color adjustments
  - Consistent padding and border treatment
  - Fade-in + scale animation on mount (300ms)
- [ ] **Recharts theme config**:
  - Default colors from `chartColors.ts`
  - Tooltip styling from design tokens
  - Grid line colors from neutral scale
  - Font from typography tokens
- [ ] **Nivo theme config** (for complex viz):
  - Same color palette integration
  - Dark mode variant
- [ ] Refactor existing chart usages:
  - DashboardViewerPage, DashboardEditorPage, LocalDashboardPage
  - PeriodDetailPage comparison charts
  - HoldingAdminOverviewPage matrix
- [ ] Financial number formatting: tabular figures (`fontFamilyNumeric`)

**AC:**
- [ ] All charts use project chart palette from JSON
- [ ] Changing chart colors in JSON recolors all charts
- [ ] Charts readable in both light and dark mode
- [ ] Financial numbers use tabular figures

---
