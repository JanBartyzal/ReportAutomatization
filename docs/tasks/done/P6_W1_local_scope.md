# P6 – Wave 1: Local Scope Architecture (Opus)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~22 MD
**Depends on:** P3b-P4 (lifecycle, forms, templates, periods stable)

> Extending existing services with multi-scope support. Careful work to avoid breaking central flows.

---

## P6-W1-001: MS-FORM Extension – Local Forms

**Type:** Service Extension
**Effort:** 8 MD
**Service:** apps/engine/microservices/units/ms-form (extension)

**Tasks:**
- [ ] **Scope Support**:
  - `scope: CENTRAL` (existing) – owned by HoldingAdmin
  - `scope: LOCAL` – owned by CompanyAdmin, visible within own org only
  - `scope: SHARED_WITHIN_HOLDING` – shared with other CompanyAdmins in holding
- [ ] **CompanyAdmin Role**:
  - New sub-role of Editor with extended rights within own org
  - Can create/manage LOCAL forms
  - Can share LOCAL forms within holding
- [ ] **Local Form Lifecycle**:
  - Same states: DRAFT → PUBLISHED → CLOSED
  - No holding approval workflow (simplified)
  - Data primarily for internal use
- [ ] **Data Release**:
  - CompanyAdmin marks local form/data as `RELEASED`
  - HoldingAdmin gets notification
  - HoldingAdmin can pull released data into central reporting (manual)
- [ ] **Access Control**:
  - LOCAL forms visible only to org members
  - SHARED forms visible to all CompanyAdmins in holding
  - HoldingAdmin has read-only overview of all local/shared forms
- [ ] RLS policy updates for scope-based access
- [ ] Flyway migrations: add `scope`, `owner_org_id` columns, update RLS

**AC:**
- [ ] CompanyAdmin creates local form, not visible to other orgs
- [ ] Shared form visible to other CompanyAdmins in same holding
- [ ] Released data available for HoldingAdmin to pull
- [ ] Existing central forms unaffected

---

## P6-W1-002: MS-TMPL-PPTX Extension – Local Templates

**Type:** Service Extension
**Effort:** 5 MD
**Service:** apps/engine/microservices/units/ms-tmpl-pptx (extension)

**Tasks:**
- [ ] **Local Template Scope**:
  - `scope: CENTRAL` (existing) – HoldingAdmin only
  - `scope: LOCAL` – CompanyAdmin creates own templates
  - `scope: SHARED_WITHIN_HOLDING` – shared with other companies
- [ ] **Local Report Generation**:
  - CompanyAdmin triggers generation using local template + local data
  - Generated report not auto-shared with holding
- [ ] **Template Sharing**:
  - CompanyAdmin can share template with other CompanyAdmins
  - HoldingAdmin has visibility over all templates in holding
- [ ] Access control and RLS updates

---

## P6-W1-003: MS-ADMIN Extension – CompanyAdmin Role

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-admin (extension)

**Tasks:**
- [ ] **CompanyAdmin Role Definition**:
  - Extended Editor with: create forms, create templates, manage local users
  - Scoped to own org only
  - Cannot access other orgs' data (except shared)
- [ ] **Role Assignment**: HoldingAdmin or Admin assigns CompanyAdmin role
- [ ] **Admin UI Extension**: CompanyAdmin section in admin panel
- [ ] RBAC engine update in MS-AUTH

---

## P6-W1-004: Advanced Period Comparison (FS22 – Foundation)

**Type:** Service Extension
**Effort:** 6 MD
**Service:** apps/engine/microservices/units/ms-dash, ms-period (extension)

**Tasks:**
- [ ] **Configurable KPIs**:
  - User-defined comparison metrics
  - Dimension combinations (org x metric x period)
- [ ] **Cross-Type Comparison**:
  - Q1 vs full year (normalization to monthly/daily basis)
  - Period type awareness in aggregation
- [ ] **Multi-Org Comparison**:
  - HoldingAdmin sees same metric across all subsidiaries
  - Side-by-side visualization
- [ ] **Drill-Down**:
  - From holding → company → division → cost center
  - Requires granular data from local forms
- [ ] **Export**: Comparison report as PPTX (via MS-GEN-PPTX integration)
- [ ] REST endpoints for comparison queries
- [ ] Redis caching for computed comparisons

**AC:**
- [ ] HoldingAdmin compares IT costs Q1/2025 vs Q1/2026 across all subsidiaries
- [ ] Drill-down from company to cost center works
- [ ] Comparison export produces valid PPTX
