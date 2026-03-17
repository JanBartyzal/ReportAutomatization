# P4b – Wave 3: Configuration (Haiku/Gemini)

**Phase:** P4b – PPTX Report Generation
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1 MD

---

## P4b-W3-001: Docker Compose – P4b Services

**Type:** Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] Add processor-generators:pptx, engine-reporting:pptx-template to docker-compose.yml
- [ ] Dapr sidecar configs
- [ ] Nginx routing: `/api/templates/pptx/*` → engine-reporting:pptx-template
- [ ] LibreOffice headless container for validation

---

## P4b-W3-002: Sample PPTX Templates

**Type:** Test Data
**Effort:** 0.5 MD

**Tasks:**
- [ ] Sample PPTX template with:
  - Text placeholders (`{{company_name}}`, `{{period}}`, `{{total_opex}}`)
  - Table placeholder (`{{TABLE:opex_summary}}`)
  - Chart placeholder (`{{CHART:monthly_trend}}`)
- [ ] Sample mapping configuration
- [ ] Place in `tests/fixtures/templates/`
