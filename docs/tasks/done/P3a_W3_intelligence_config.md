# P3a – Wave 3: Configuration (Haiku/Gemini)

**Phase:** P3a – Intelligence & Admin
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~1.5 MD

---

## P3a-W3-001: Docker Compose – P3a Services

**Type:** Infrastructure
**Effort:** 0.5 MD

**Tasks:**
- [ ] Add engine-core:admin, engine-core:batch, processor-atomizers:ai, processor-generators:mcp, engine-data:template to docker-compose.yml
- [ ] Dapr sidecar configs for new services
- [ ] Nginx routing updates for `/api/admin/*`, `/api/batches/*`
- [ ] LiteLLM configuration (API key, model selection)
- [ ] Environment variables for AI quotas

---

## P3a-W3-002: AI Prompt Templates

**Type:** Configuration
**Effort:** 1 MD

**Tasks:**
- [ ] Classification prompt templates (OPEX categories)
- [ ] Entity extraction templates (financial data)
- [ ] Column mapping suggestion templates
- [ ] Summarization templates
- [ ] Store in `apps/processor/microservices/units/ms-atm-ai/prompts/`
- [ ] Configurable via environment variables
