# P6 – Wave 2: Data Integration for Local Scope (Sonnet)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~5 MD
**Depends on:** P6-W1 (scope support in MS-FORM, MS-TMPL-PPTX, MS-ADMIN)

> Extending query and lifecycle services to support multi-scope data flows.

---

## P6-W2-001: MS-QRY Extension – Multi-Scope Queries

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-qry (extension)

**Tasks:**
- [ ] Query parameters support `scope` filter (CENTRAL, LOCAL, ALL)
- [ ] CompanyAdmin queries: own org data (local + central assigned)
- [ ] HoldingAdmin queries: all data (central + released local)
- [ ] Materialized views updated for scope-aware aggregation
- [ ] Performance optimization for multi-org comparison queries

---

## P6-W2-002: MS-LIFECYCLE Extension – Local Report Flow

**Type:** Service Extension
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-lifecycle (extension)

**Tasks:**
- [ ] Local reports: simplified lifecycle (DRAFT → COMPLETED, no holding approval)
- [ ] Released local reports: optionally enter central approval flow
- [ ] Status matrix: include local reports in CompanyAdmin view
