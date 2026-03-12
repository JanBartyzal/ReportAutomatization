# P4b – Wave 4: Frontend Generator UI (Gemini Flash/MiniMax)

**Phase:** P4b – PPTX Report Generation
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD

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
> **Specificky pro P4b-W4:**
> - P4b-W4-001 (Template Management): DataGrid pro seznam šablon dle sekce 10.1, slide thumbnaily v card layoutu dle Widget karty sekce 10.3
> - P4b-W4-001 (Placeholder Mapping): Dropdown selectory — formulářová pravidla dle sekce 9
> - P4b-W4-002 (Report Generation): Progress indikátor s brand barvou, status badges dle kontextových barev `00-project-color-overrides.md` sekce 5

---

## P4b-W4-001: Template Management UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Template List Page** (HoldingAdmin):
  - Table: name, version, assigned period, placeholder count
  - Upload new template button
- [ ] **Template Detail Page**:
  - Uploaded PPTX preview (slide thumbnails)
  - Extracted placeholder list
  - Version history
- [ ] **Placeholder Mapping Editor**:
  - Table: placeholder → data source (dropdown)
  - Data source options: form fields, uploaded file columns, dashboard metrics
  - Preview button (generate with sample data)

---

## P4b-W4-002: Report Generation UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Generate Button** on report detail page (visible when APPROVED):
  - Click → trigger generation workflow
  - Progress indicator (polling or SSE)
  - Download link when complete
- [ ] **Batch Generation UI** (HoldingAdmin):
  - Select period → show APPROVED reports
  - "Generate All" button
  - Progress: X/Y completed
  - Download individual or ZIP of all
- [ ] **Generated Reports List**:
  - Table: report, template, generated_at, download link
  - Re-generate button (with current data)
