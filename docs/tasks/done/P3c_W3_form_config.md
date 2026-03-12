# P3c – Wave 3: Configuration (Haiku/Gemini)

**Phase:** P3c – Form Builder & Data Collection
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1 MD

---

## P3c-W3-001: Docker Compose – P3c Services

**Type:** Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [x] Add MS-FORM to docker-compose.yml
- [x] Dapr sidecar config
- [x] Nginx routing: `/api/forms/*` → MS-FORM
- [x] Update MS-SINK-TBL, MS-TMPL, MS-ORCH docker configs if needed

---

## P3c-W3-002: Sample Form Definitions

**Type:** Test Data
**Effort:** 0.5 MD

**Tasks:**
- [x] Sample OPEX form with sections: Personnel, IT, Office, Travel
- [x] Sample fields: budget_amount (number/CZK), headcount (number), category (dropdown)
- [x] Sample form responses for testing
- [ ] Sample Excel template (exported from form)
- [x] Flyway seed migration
