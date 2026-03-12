# P3c – Wave 4: Frontend Form UI (Gemini Flash/MiniMax)

**Phase:** P3c – Form Builder & Data Collection
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~19 MD
**Depends on:** P3c-W1 (form endpoints)

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
> **Specificky pro P3c-W4:**
> - P3c-W4-001 (Form Builder): **Formulářová pravidla striktně dle `02-design-system.md` sekce 9** — Fluent `<Field>` wrapper, stavy (default/hover/focus/error/disabled), labels nad inputem, spacing 16px
> - P3c-W4-002 (Form Filler): Validace — červený border + error message dle sekce 9.2 (Error stav), auto-save indikátor s brand barvou
> - P3c-W4-002 (Submission checklist): Progress indikátor — Success sémantická barva pro splněné položky
> - P3c-W4-003 (Excel Import): Confidence indikátory — Success/Warning/Danger sémantické barvy, mapping wizard dle Designer page wireframe v `04-figma-pages.md`
> - Drag & drop: Používat Fluent motion tokens (Medium 300ms, ease-in-out)

---

## P3c-W4-001: Form Builder UI

**Type:** Frontend Feature
**Effort:** 8 MD

**Tasks:**
- [x] **Form Editor Page** (Admin):
  - Drag & drop field ordering
  - Field type picker (text, number, percentage, date, dropdown, table, file)
  - Field property panel: label, required, validation rules
  - Section headers with description text
  - Cross-field dependency builder (simple if/then UI)
  - Preview mode (render form as user would see it)
  - Publish / Close buttons
- [x] **Form List Page**:
  - Table: form name, status, version, assigned orgs count, period
  - Filter by status (DRAFT, PUBLISHED, CLOSED)
- [ ] **Form Assignment UI**:
  - Org selector with holding hierarchy tree
  - Multi-select orgs for assignment
  - Assignment status per org (assigned, started, submitted)

---

## P3c-W4-002: Form Filling UI

**Type:** Frontend Feature
**Effort:** 7 MD

**Tasks:**
- [x] **Form Filler Page** (Editor):
  - Render form from JSON definition
  - Section navigation (sidebar or tabs)
  - Input components per field type:
    - Text: standard input
    - Number: input with currency/unit suffix
    - Percentage: input with % suffix
    - Date: date picker
    - Dropdown: select component with predefined values
    - Table: editable grid (add/remove rows)
    - File attachment: mini upload zone
  - **Real-time validation**: red border + error message on invalid fields
  - **Auto-save indicator**: "Saved 5 seconds ago" / "Saving..."
  - **Field comments**: click icon next to any field to add comment
  - **Submission checklist**: sidebar showing completeness %
  - **Submit button**: disabled until checklist 100%
- [x] **My Forms Page** (Editor):
  - List of forms to fill in current period
  - Status per form: Not started, In progress, Submitted
  - Quick link to fill form

---

## P3c-W4-003: Excel Import UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [x] **Import Dialog**:
  - Upload Excel file
  - Show parsing progress
  - Display mapping suggestion table:
    - Left column: Excel headers
    - Right column: Form fields (dropdown selector)
    - Confidence indicator (green/yellow/red)
    - Auto-mapped fields pre-selected
  - Confirm mapping button
  - After confirmation: show imported data in form for review
- [x] **Template Download**:
  - "Download Excel Template" button on form page
  - Template generation indicator
- [ ] **Arbitrary Excel Import**:
  - Upload non-template Excel
  - Column mapping wizard (multi-step)
  - Preview imported data before confirm
