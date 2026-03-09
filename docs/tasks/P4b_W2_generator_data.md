# P4b – Wave 2: Data Aggregation for Generator (Sonnet)

**Phase:** P4b – PPTX Report Generation
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~6 MD

---

## P4b-W2-001: MS-QRY Extension – Report Data Aggregation

**Type:** Service Extension
**Effort:** 4 MD
**Service:** apps/engine/microservices/units/ms-qry (extension)

**Tasks:**
- [ ] New gRPC endpoint (internal, for MS-ORCH):
  - `GetReportData(report_id)` → aggregated data for PPTX generation
  - Combines: form responses + uploaded file data
  - Applies placeholder mapping to produce key-value pairs
- [ ] Data aggregation:
  - Sum/avg across form fields for TABLE placeholders
  - Time series for CHART placeholders
  - Text values for TEXT placeholders
- [ ] Caching: Redis cache for aggregated data (TTL 5 min)

---

## P4b-W2-002: MS-DASH Extension – Generator Trigger

**Type:** Service Extension
**Effort:** 2 MD
**Service:** apps/engine/microservices/units/ms-dash (extension)

**Tasks:**
- [ ] REST endpoint: `POST /api/dashboards/generate-pptx`
  - Trigger PPTX generation for dashboard data
  - Uses MS-GEN-PPTX with dashboard-specific template
- [ ] Dashboard export as PPTX (per-dashboard template)
