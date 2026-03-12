# P4a – Wave 4: Frontend Enterprise UI (Gemini Flash/MiniMax)

**Phase:** P4a – Enterprise Features
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~12 MD

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
> **Specificky pro P4a-W4:**
> - P4a-W4-001 (Notifications): Bell icon + dropdown — glassmorphism povolený pro floating panel dle `02-design-system.md` sekce 1.2, shadow Level 3
> - P4a-W4-001 (Toast): Toast notifikace — sémantické barvy (Success/Danger/Warning/Info) dle `00-project-color-overrides.md` sekce 3
> - P4a-W4-002 (Diff Viewer): Barvy diffů — Success light bg pro přidané, Danger light bg pro odebrané, Warning light bg pro změněné
> - P4a-W4-003 (Audit Log): DataGrid dle sekce 10.1, filtrování
> - P4a-W4-004 (Search): Global search bar v TopNav — autocomplete dropdown shadow Level 3
> - P4a-W4-005 (Dashboard Extensions): Chart palette dle `00-project-color-overrides.md` sekce 4

---

## P4a-W4-001: Notification Center UI

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Notification bell icon in top bar with unread count badge
- [ ] Notification dropdown panel:
  - List of recent notifications (newest first)
  - Unread indicator (bold / dot)
  - Click → navigate to relevant page
  - "Mark all as read" button
- [ ] Notification settings page:
  - Toggle per notification type
  - Channel preference (in-app, email, both)
- [ ] SSE integration: real-time notification push
- [ ] Toast notification for high-priority events

---

## P4a-W4-002: Versioning & Diff UI

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Version history sidebar on data detail pages
- [ ] Version list: v1, v2, v3 with dates and authors
- [ ] Diff viewer:
  - Side-by-side comparison
  - Changed values highlighted (green = added, red = removed, yellow = modified)
  - For tables: row-level diff with cell highlighting
  - For OPEX data: monetary change summary ("IT costs: +500k CZK")
- [ ] Restore from version (creates new version, not overwrite)

---

## P4a-W4-003: Audit Log Viewer UI

**Type:** Frontend Feature
**Effort:** 2 MD

**Tasks:**
- [ ] Audit log page (Admin only):
  - Filterable table: user, action type, entity, date range
  - Detail view: full audit entry with JSONB details
  - Export button (CSV/JSON)
- [ ] Inline audit link on entity pages (file, report, form)
  - "View audit trail" → filtered audit log for that entity

---

## P4a-W4-004: Search UI

**Type:** Frontend Feature
**Effort:** 2 MD

**Tasks:**
- [ ] Global search bar in top navigation
- [ ] Search results page:
  - Categorized results: Files, Documents, Reports, Forms
  - Relevance score indicator
  - Snippet preview with highlighted matches
- [ ] Autocomplete dropdown (search-as-you-type)
- [ ] Toggle: text search vs semantic search

---

## P4a-W4-005: Dashboard Extensions

**Type:** Frontend Feature
**Effort:** 2 MD

**Tasks:**
- [ ] Dashboard SQL editor (advanced mode) for power users
- [ ] Dashboard sharing: public/private toggle
- [ ] Dashboard cloning
- [ ] Widget resize and reorder (grid layout)
