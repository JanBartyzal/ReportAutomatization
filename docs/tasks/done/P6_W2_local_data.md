# P6 – Wave 2: Data Integration for Local Scope (Sonnet)

**Phase:** P6 – Local Scope & Advanced Analytics
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~5 MD
**Depends on:** P6-W1 (scope support in engine-reporting:form, engine-reporting:pptx-template, engine-core:admin)

> Extending query and lifecycle services to support multi-scope data flows.

---

## P6-W2-001: engine-data:query Extension – Multi-Scope Queries

**Type:** Service Extension
**Effort:** 3 MD
**Service:** apps/engine/microservices/units/ms-qry (extension)

**Tasks:**
- [x] Query parameters support `scope` filter (CENTRAL, LOCAL, ALL)
- [x] CompanyAdmin queries: own org data (local + central assigned)
- [x] HoldingAdmin queries: all data (central + released local)
- [x] Materialized views updated for scope-aware aggregation
- [x] Performance optimization for multi-org comparison queries

---

## P6-W2-002: engine-reporting:lifecycle Extension – Local Report Flow

**Type:** Service Extension
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-lifecycle (extension)

**Tasks:**
- [x] Local reports: simplified lifecycle (DRAFT → COMPLETED, no holding approval)
- [x] Released local reports: optionally enter central approval flow
- [x] Status matrix: include local reports in CompanyAdmin view
