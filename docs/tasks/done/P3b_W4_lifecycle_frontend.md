# P3b – Wave 4: Frontend Lifecycle UI (Gemini Flash/MiniMax)

**Phase:** P3b – Reporting Lifecycle & Period Management
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~10 MD
**Depends on:** P3b-W1 (lifecycle, period endpoints)

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
> **Specificky pro P3b-W4:**
> - P3b-W4-001 (Report Lifecycle): **Status badges MUSÍ používat kontextové barvy** dle `00-project-color-overrides.md` sekce 5 (DRAFT=šedá, SUBMITTED=žlutá, APPROVED=zelená, REJECTED=červená, OVERDUE=oranžová)
> - P3b-W4-001 (HoldingAdmin Matrix): Buňky matice barevně dle status barev — použít `STATUS_COLORS` mapping (sekce 6.3)
> - P3b-W4-001 (Timeline): Časová osa stavových přechodů — ikony Fluent Icons v2, barvy dle sémantických tokenů
> - P3b-W4-002 (Period Management): Countdown widget — KPI karta dle `02-design-system.md` sekce 10.2, progress bar s brand barvou
> - Reports page wireframe v `04-figma-pages.md`

---

## P3b-W4-001: Report Lifecycle UI

**Type:** Frontend Feature
**Effort:** 6 MD

**Tasks:**
- [ ] **Report List Page**:
  - Table: report name, org, period, status badge, last updated
  - Filter by status, period, org
  - Quick action buttons: Submit, Review, Approve, Reject
- [ ] **Report Detail Page**:
  - Status badge with current state
  - Timeline view of all state transitions (who, when, comment)
  - Linked files and form responses
  - Submission checklist (green/red indicators)
  - Submit button (disabled until checklist 100%)
- [ ] **Rejection Dialog**:
  - Modal with mandatory comment field
  - Comment visible in timeline after rejection
- [ ] **Bulk Actions** (HoldingAdmin):
  - Multi-select reports in list
  - Bulk approve / bulk reject buttons
  - Confirmation dialog with count
- [ ] **HoldingAdmin Matrix Dashboard**:
  - Grid: rows = companies, columns = periods
  - Cell color: grey (DRAFT), yellow (SUBMITTED), green (APPROVED), red (REJECTED/overdue)
  - Click cell → navigate to report detail

---

## P3b-W4-002: Period Management UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Period List Page**:
  - Table: name, type, dates, status, completion %
  - Filter by type, status, year
- [ ] **Period Detail Page**:
  - Dates and deadlines display
  - Completion matrix (same as HoldingAdmin matrix, scoped to period)
  - Percentage progress bar
  - Export button (PDF/Excel)
- [ ] **Create/Clone Period**:
  - Form: name, type, start_date, submission_deadline, review_deadline
  - Clone button on existing period (pre-fills form with new dates)
- [ ] **Countdown Widget**: Days until deadline, shown on Editor dashboard
