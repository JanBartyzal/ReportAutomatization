# P6 – Wave 4: Local Scope & Comparison Frontend (Gemini Flash/MiniMax)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD
**Depends on:** P6-W1 (backend scope support), P6-W2 (multi-scope queries)

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
> **Specificky pro P6-W4:**
> - P6-W4-001 (Local Forms): Scope badge (CENTRAL/LOCAL/SHARED) — použít Badge atom z `03-figma-components.md`, odlišné barvy pro scope typy (Info/Success/Warning sémantické)
> - P6-W4-002 (Comparison UI): **Chart Palette** dle `00-project-color-overrides.md` sekce 4, heatmap barvy konzistentní s chart paletou
> - P6-W4-002 (Multi-Org Comparison): Horizontal bar chart — barvy dle chart palette, drill-down breadcrumb navigace

---

## P6-W4-001: Local Forms & Templates UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [x] **CompanyAdmin Dashboard**:
  - My local forms list
  - My local templates list
  - Create local form / template buttons
- [x] **Scope Indicator**: Badge on forms/templates showing CENTRAL / LOCAL / SHARED
- [ ] **Share Dialog**: Share local form/template with other companies
- [ ] **Release Dialog**: Release local data to holding
- [ ] **HoldingAdmin Overview**: List of all local/shared items across holding
- [ ] **Pull Released Data**: HoldingAdmin view of released data, import button

---

## P6-W4-002: Advanced Comparison UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [x] **Comparison Builder**:
  - Select metric, periods (multi-select), orgs (multi-select)
  - Chart type: bar (grouped), line, table
  - Normalization toggle (monthly/quarterly/annual)
- [x] **Multi-Org Comparison Dashboard**:
  - Horizontal bar chart: same metric, all subsidiaries
  - Heatmap: orgs × periods, color = metric value
- [x] **Drill-Down View**:
  - Click org → expand to divisions/cost centers
  - Breadcrumb navigation
- [ ] **Export Button**: Generate PPTX comparison report
