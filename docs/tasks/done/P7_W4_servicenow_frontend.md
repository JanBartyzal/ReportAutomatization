# P7 – Wave 4: Service-Now & Smart Persistence – Frontend (Flash/MiniMax)

**Phase:** P7 – External Integrations & Data Optimization
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~10 MD
**Depends on:** P7-W1 (APIs available), P7-W2 (types defined)

> React UI for Service-Now integration management and Smart Persistence promotion.

---

## UX/UI Design System (povinné)

Všechny komponenty MUSÍ dodržovat:
- [`docs/UX-UI/00-project-color-overrides.md`](../UX-UI/00-project-color-overrides.md) — Crimson brand `#C4314B`
- [`docs/UX-UI/02-design-system.md`](../UX-UI/02-design-system.md) — layout, typografie, spacing, elevace
- [`docs/UX-UI/03-figma-components.md`](../UX-UI/03-figma-components.md) — atomické komponenty
- Fluent UI v9 komponenty, `makeStyles()` pro styling, žádné hardcoded barvy
- Dark mode support přes FluentProvider

---

## P7-W4-001: Service-Now Integration Admin Page

**Type:** Frontend Page
**Effort:** 4 MD
**File:** `apps/frontend/src/pages/IntegrationPage.tsx`

**Tasks:**
- [ ] **Integration List View**:
  - DataGrid with columns: Instance URL, Auth Type, Status, Last Sync, Actions
  - Status badges using project status colors
  - Add/Edit/Delete actions
- [ ] **Integration Config Dialog**:
  - Form fields: Instance URL, Auth Type (dropdown: OAuth2/Basic), Credential Reference
  - Table selection: multi-select of Service-Now tables to sync
  - Schema Mapping template assignment per table
  - "Test Connection" button with loading state
- [ ] **Schedule Management**:
  - Cron expression builder (visual, not raw text)
  - Enable/disable toggle per schedule
  - Next run preview
- [ ] **Sync History Tab**:
  - Timeline view of sync job executions
  - Status, duration, records fetched/stored
  - Error detail expandable panel
- [ ] **API client**: `apps/frontend/src/api/integrations.ts`
- [ ] **Hook**: `apps/frontend/src/hooks/useIntegrations.ts` (React Query)

**AC:**
- [ ] Admin creates Service-Now integration via UI
- [ ] Connection test shows success/failure inline
- [ ] Sync history displays with correct status colors
- [ ] Dark mode renders correctly

---

## P7-W4-002: Report Distribution Configuration Page

**Type:** Frontend Page
**Effort:** 2 MD
**File:** `apps/frontend/src/pages/DistributionRulesPage.tsx`

**Tasks:**
- [ ] **Distribution Rules List**:
  - DataGrid: Schedule, Report Template, Recipients, Format, Status
  - Enable/disable toggle
- [ ] **Rule Editor Dialog**:
  - Schedule dropdown (from configured schedules)
  - Report template selector
  - Recipients: email input with tag-style chips
  - Format selector (XLSX)
- [ ] **Distribution History Tab**:
  - List of sent reports with timestamp, recipients, status
  - Download link for generated file
- [ ] API client and React Query hook

**AC:**
- [ ] Admin creates distribution rule → emails sent after next sync
- [ ] History shows delivery status

---

## P7-W4-003: Smart Persistence Promotion Admin Page

**Type:** Frontend Page
**Effort:** 3 MD
**File:** `apps/frontend/src/pages/PromotionPage.tsx`

**Tasks:**
- [ ] **Candidates List**:
  - DataGrid: Mapping Name, Usage Count, Orgs Using, Status, Actions
  - Status badges: `CANDIDATE`, `APPROVED`, `PROMOTED`, `DISMISSED`
  - Sort by usage count (descending default)
- [ ] **Promotion Detail Panel**:
  - Proposed DDL preview (syntax-highlighted SQL)
  - Editable DDL textarea for admin modifications
  - Column type suggestions with confidence indicators
  - Proposed indexes list
  - "Approve" / "Dismiss" action buttons
- [ ] **Migration Progress View**:
  - Progress bar for historical data migration
  - Record count: migrated / total
  - Status: running, completed, failed
- [ ] **Promoted Tables Overview**:
  - List of all promoted tables with creation date
  - Dual-write status and expiry date
  - Storage stats (row count, size)
- [ ] API client: `apps/frontend/src/api/promotions.ts`
- [ ] Hook: `apps/frontend/src/hooks/usePromotions.ts`

**AC:**
- [ ] Admin sees candidate with proposed schema
- [ ] DDL editable before approval
- [ ] Migration progress visible in real-time
- [ ] Promoted tables listed with status

---

## P7-W4-004: Navigation & Routing Updates

**Type:** Frontend Infrastructure
**Effort:** 1 MD

**Tasks:**
- [ ] **Router updates** (`apps/frontend/src/App.tsx`):
  - `/admin/integrations` → IntegrationPage
  - `/admin/integrations/distribution` → DistributionRulesPage
  - `/admin/promotions` → PromotionPage
- [ ] **Sidebar navigation** (Admin section):
  - "Integrations" menu item with Service-Now sub-item
  - "Data Promotion" menu item
  - Role guard: visible only to Admin/HoldingAdmin
- [ ] **Breadcrumb updates** for new pages

**AC:**
- [ ] New pages accessible from sidebar navigation
- [ ] Non-admin users cannot see integration/promotion pages

---
