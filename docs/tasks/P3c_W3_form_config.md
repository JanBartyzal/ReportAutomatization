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
- [ ] Add MS-FORM to docker-compose.yml
- [ ] Dapr sidecar config
- [ ] Nginx routing: `/api/forms/*` → MS-FORM
- [ ] Update MS-SINK-TBL, MS-TMPL, MS-ORCH docker configs if needed

---

## P3c-W3-002: Sample Form Definitions

**Type:** Test Data
**Effort:** 0.5 MD

**Tasks:**
- [ ] Sample OPEX form with sections: Personnel, IT, Office, Travel
- [ ] Sample fields: budget_amount (number/CZK), headcount (number), category (dropdown)
- [ ] Sample form responses for testing
- [ ] Sample Excel template (exported from form)
- [ ] Flyway seed migration
