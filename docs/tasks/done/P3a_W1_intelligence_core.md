# P3a – Wave 1: Schema Mapping & AI Gateway (Opus)

**Phase:** P3a – Intelligence & Admin
**Agent:** Opus
**Complexity:** Hard
**Total Effort:** ~35 MD
**Depends on:** P1 (auth, orchestrator), P2 (query layer)

> Complex services with learning algorithms and AI integration.

---

## P3a-W1-001: MS-TMPL – Template & Schema Mapping Registry

**Type:** Core Service
**Effort:** 17 MD
**Service:** apps/engine/microservices/units/ms-tmpl

**Tasks:**
- [ ] Spring Boot 3.x + gRPC using `packages/java-base`
- [ ] Implement `TemplateMappingService` from `template.v1` proto:
  - `ApplyMapping` – apply mapping rules to extracted data before sink write
  - `SuggestMapping` – AI-assisted suggestion based on column headers
  - `MapExcelToForm` – map Excel columns to form fields (FS19 prep)
- [ ] **Mapping Rule Engine**:
  - Exact match rules (column name → target name)
  - Synonym rules (`Cena`, `Cost`, `Naklady` → `amount_czk`)
  - Regex pattern rules (`^IT.*cost$` → `it_costs`)
  - AI-suggested rules (via MS-ATM-AI)
- [ ] **Learning from History**:
  - Store successful mappings per org
  - Rank suggestions by historical frequency
  - Auto-suggest based on past mappings for same org
- [ ] **Mapping Template CRUD**:
  - Create/update/delete mapping templates
  - Templates scoped to org or global (admin)
  - Version mapping templates
- [ ] Flyway migrations: `mapping_templates`, `mapping_rules`, `mapping_history` tables
- [ ] Called from MS-ORCH via Dapr gRPC BEFORE sink write
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Column `Naklady` auto-mapped to `amount_czk` using synonym rule
- [ ] Second upload from same org → mapping auto-suggested from history
- [ ] AI suggestion confidence > 0.8 shown in UI

---

## P3a-W1-002: MS-ATM-AI – AI Gateway

**Type:** Core Service
**Effort:** 10 MD
**Service:** apps/processor/microservices/units/ms-atm-ai

**Tasks:**
- [ ] FastAPI + gRPC using `packages/python-base`
- [ ] Implement `AiGatewayService` from `atomizer.v1.ai` proto:
  - `AnalyzeSemantic` – text classification, summarization, entity extraction
  - `GenerateEmbeddings` – vector embeddings for pgVector
  - `SuggestCleaning` – column name normalization suggestions
- [ ] **LiteLLM Integration**:
  - OpenAI-compatible API wrapper
  - Model: Azure OpenAI (GPT-4o for analysis, text-embedding-3-small for embeddings)
  - Configurable model per operation type
- [ ] **Cost Control**:
  - Token counter per request
  - Monthly quota per user and per org
  - Quota exceeded → gRPC `RESOURCE_EXHAUSTED` (maps to `429`)
  - Token usage logged to MS-SINK-LOG
- [ ] **Prompt Engineering**:
  - Classification prompts for OPEX data categories
  - Entity extraction prompts for financial data
  - Column mapping suggestion prompts
  - Prompt templates stored in config (not hardcoded)
- [ ] **Rate Limiting**: Max concurrent requests per org
- [ ] Docker Compose entry + Dapr sidecar

**AC:**
- [ ] Semantic analysis returns valid classification
- [ ] Embedding generation produces 1536-dim vector
- [ ] Quota exceeded → `429` with `tokens_remaining: 0`
- [ ] All token usage logged

---

## P3a-W1-003: MS-MCP – MCP Server (AI Agent)

**Type:** Core Service
**Effort:** 8 MD
**Service:** apps/processor/microservices/units/ms-mcp

**Tasks:**
- [ ] FastAPI project using `packages/python-base`
- [ ] **MCP Server Protocol**:
  - Standard MCP tool definitions for data querying
  - Tools: `query_opex_data`, `search_documents`, `get_report_status`, `compare_periods`
- [ ] **On-Behalf-Of (OBO) Flow**:
  - Receive user's OAuth token
  - Exchange for downstream API token via OBO flow
  - AI agent never gets global access – always scoped to user's permissions
- [ ] **RLS Enforcement**: Every AI query scoped to user's `org_id`
- [ ] **Tool Definitions**:
  - `query_opex_data(org_id, period, metric)` → aggregated data
  - `search_documents(query, semantic=true)` → relevant documents
  - `get_report_status(period_id)` → submission matrix
  - `compare_periods(period_a, period_b, metric)` → delta analysis
- [ ] **Cost Control**: Same quota system as MS-ATM-AI
- [ ] Docker Compose entry

**AC:**
- [ ] MCP client can query data through AI agent
- [ ] OBO token correctly scoped to user's org
- [ ] Cross-tenant query returns empty (not error)
