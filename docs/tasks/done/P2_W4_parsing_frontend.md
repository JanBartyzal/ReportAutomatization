# P2 – Wave 4: Frontend Dashboards & Viewer (Gemini Flash/MiniMax)

**Phase:** P2 – Extended Parsing & Visualization
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~9 MD
**Depends on:** P2-W1 (engine-data:query, engine-data:dashboard endpoints)

---

## UX/UI Design System (povinné)

> Veškerý frontend kód MUSÍ dodržovat projektový design system. Nepoužívat žádné ad-hoc styly.
>
> | Dokument | Obsah |
> |----------|-------|
> | [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) | Layout, typografie, spacing, elevace, formuláře, dark mode, a11y |
> | [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) | Atomické a kompozitní komponenty |
> | [`docs/UX-UI/04-figma-pages.md`](../UX-UI/04-figma-pages.md) | Wireframy stránek, responsive breakpoints |
> | [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) | **Projektově specifické barvy** (liší se od CIM!) |
> | [`docs/UX-UI/Figma/Components/tokens.json`](../UX-UI/Figma/Components/tokens.json) | Implementační design tokeny |
>
> **Klíčová pravidla:**
> - Brand barva: **Crimson `#C4314B`** (NE Azure Blue z CIM)
> - Styling: Fluent UI `makeStyles` + tokeny. Žádné hardcoded hex barvy, raw CSS, `!important`
> - Dark mode: Vše přes `FluentProvider` theme
> - A11y: WCAG 2.1 AA minimum
>
> **Specificky pro P2-W4:**
> - P2-W4-001 (File Viewer): DataGrid dle `02-design-system.md` sekce 10.1, tabulky s alternating rows
> - P2-W4-002 (Dashboard Builder): **Chart Palette** dle `00-project-color-overrides.md` sekce 4, KPI karty dle sekce 10.2, Widget karty dle sekce 10.3. Dashboard page wireframe v `04-figma-pages.md`
> - P2-W4-002 (Dashboard Viewer): Recharts/Nivo musí používat projektové chart barvy (CSS custom properties `--chart-1` až `--chart-8`)

---

## P2-W4-001: Enhanced File Viewer

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Excel viewer: sheet tabs, table rendering with data types
- [ ] PDF viewer: page-by-page text display, OCR confidence indicator
- [ ] CSV viewer: auto-formatted table with detected headers
- [ ] Unified table view: all extracted tables from any format
- [ ] File processing status polling (3s interval)

---

## P2-W4-002: Dashboard Builder UI

**Type:** Frontend Feature
**Effort:** 5 MD

**Tasks:**
- [ ] Dashboard list page (public + own dashboards)
- [ ] Dashboard editor:
  - Data source selector (OPEX tables, parsed files)
  - GROUP BY / ORDER BY configurator (dropdown-based)
  - Filter builder (date range, org, custom JSONB field)
  - Chart type selector (bar, line, pie)
- [ ] Dashboard viewer:
  - Recharts integration for standard charts
  - Nivo integration for heatmaps
  - Full-screen mode
  - Refresh button
- [ ] Period comparison view:
  - Select two periods, same metric
  - Side-by-side bar chart + delta table
- [ ] Responsive grid layout for dashboard widgets

**AC:**
- [ ] Admin creates dashboard in < 5 minutes using UI (no SQL needed)
- [ ] Viewer sees only public dashboards
- [ ] Charts render correctly with real data

---

## P2-W4-003: File List Enhancements

**Type:** Frontend Feature
**Effort:** 1 MD

**Tasks:**
- [ ] Filter by file type (PPTX, XLSX, PDF, CSV)
- [ ] Filter by processing status
- [ ] Sort by upload date, filename, size
- [ ] Batch selection (select multiple files)
- [ ] File type icons
