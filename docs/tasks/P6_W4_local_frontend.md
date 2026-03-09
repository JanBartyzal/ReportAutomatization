# P6 – Wave 4: Local Scope & Comparison Frontend (Gemini Flash/MiniMax)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Gemini Flash / MiniMax
**Complexity:** Frontend
**Total Effort:** ~8 MD
**Depends on:** P6-W1 (backend scope support), P6-W2 (multi-scope queries)

---

## P6-W4-001: Local Forms & Templates UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **CompanyAdmin Dashboard**:
  - My local forms list
  - My local templates list
  - Create local form / template buttons
- [ ] **Scope Indicator**: Badge on forms/templates showing CENTRAL / LOCAL / SHARED
- [ ] **Share Dialog**: Share local form/template with other companies
- [ ] **Release Dialog**: Release local data to holding
- [ ] **HoldingAdmin Overview**: List of all local/shared items across holding
- [ ] **Pull Released Data**: HoldingAdmin view of released data, import button

---

## P6-W4-002: Advanced Comparison UI

**Type:** Frontend Feature
**Effort:** 4 MD

**Tasks:**
- [ ] **Comparison Builder**:
  - Select metric, periods (multi-select), orgs (multi-select)
  - Chart type: bar (grouped), line, table
  - Normalization toggle (monthly/quarterly/annual)
- [ ] **Multi-Org Comparison Dashboard**:
  - Horizontal bar chart: same metric, all subsidiaries
  - Heatmap: orgs × periods, color = metric value
- [ ] **Drill-Down View**:
  - Click org → expand to divisions/cost centers
  - Breadcrumb navigation
- [ ] **Export Button**: Generate PPTX comparison report
