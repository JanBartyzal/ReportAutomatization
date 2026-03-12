# P3a – Wave 4: Frontend Admin & AI UI (Gemini Flash/MiniMax)

**Phase:** P3a – Intelligence & Admin
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD
**Depends on:** P3a-W2 (admin endpoints)

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
> **Specificky pro P3a-W4:**
> - P3a-W4-001 (Admin Panel): Admin page wireframe v `04-figma-pages.md`, tree view org hierarchy, DataGrid pro user/key/job management dle sekce 10.1
> - P3a-W4-001 (Role badges): Použít Badge atom z `03-figma-components.md`, barvy dle sémantických tokenů
> - P3a-W4-002 (Schema Mapping): Confidence indikátory — Success/Warning/Danger sémantické barvy dle `00-project-color-overrides.md` sekce 3

---

## P3a-W4-001: Admin Panel UI

**Type:** Frontend Feature
**Effort:** 5 MD

**Tasks:**
- [ ] Admin section in navigation (visible only to Admin/HoldingAdmin)
- [ ] Organization management page:
  - Tree view of holding hierarchy
  - Create/edit/delete organizations
  - Drag & drop reordering
- [ ] User management page:
  - User list with role badges
  - Role assignment dialog
  - Search/filter users
- [ ] API key management page:
  - Key list with usage stats
  - Generate new key (show once)
  - Revoke key with confirmation
- [ ] Failed jobs page:
  - Table with error details
  - Reprocess button with confirmation
  - Status filter (failed, reprocessed)
- [ ] Batch management page:
  - Create/view batches
  - Consolidated file list per batch
  - Status summary

---

## P3a-W4-002: Schema Mapping UI

**Type:** Frontend Feature
**Effort:** 3 MD

**Tasks:**
- [ ] Mapping template editor:
  - Source column → target column drag & drop
  - Rule type selector (exact, synonym, regex, AI)
  - Confidence indicator for AI suggestions
  - Preview with sample data
- [ ] Mapping history view (successful mappings per org)
- [ ] Auto-suggestion display during upload processing
- [ ] Template versioning UI
