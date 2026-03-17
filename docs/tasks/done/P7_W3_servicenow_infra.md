# P7 – Wave 3: Service-Now & Smart Persistence – Infra & Boilerplate (Haiku/Gemini)

**Phase:** P7 – External Integrations & Data Optimization
**Agent:** Haiku / Gemini
**Complexity:** Easy
**Total Effort:** ~3 MD
**Depends on:** P7-W1 (core logic), P7-W2 (configs)

> Boilerplate, environment files, test fixtures, and documentation.

---

## P7-W3-001: Environment & Configuration Files

**Type:** Configuration
**Effort:** 1 MD

**Tasks:**
- [ ] **Environment variables**:
  - Update `infra/docker/.env.example` with engine-integrations:servicenow variables:
    - `SERVICENOW_INSTANCE_URL`, `SERVICENOW_AUTH_TYPE`
    - `EXCEL_GENERATOR_PORT`, `SERVICENOW_CONNECTOR_PORT`
  - Update `apps/frontend/.env.example` with new API base URLs
- [ ] **Dapr component files**:
  - `infra/docker/dapr/ms-ext-snow/pubsub.yaml` – PubSub subscription
  - `infra/docker/dapr/ms-ext-snow/statestore.yaml` – Redis state store for scheduler lock
  - `infra/docker/dapr/ms-gen-xls/pubsub.yaml`
- [ ] **Dockerfile** for ms-ext-snow (copy from ms-admin template)
- [ ] **Dockerfile** for ms-gen-xls (copy from ms-atm-pptx template)
- [ ] **requirements.txt** / **build.gradle** dependency files

**AC:**
- [ ] Services build from Dockerfiles without errors
- [ ] Environment files document all required variables

---

## P7-W3-002: Test Fixtures & Seed Data

**Type:** Testing
**Effort:** 1 MD

**Tasks:**
- [ ] **WireMock stubs** for Service-Now API:
  - `tests/fixtures/servicenow/table_incident.json` – sample incident table response
  - `tests/fixtures/servicenow/table_cmdb_ci.json` – sample CMDB response
  - `tests/fixtures/servicenow/auth_token.json` – OAuth token response
  - `tests/fixtures/servicenow/rate_limited.json` – 429 response
- [ ] **Excel template fixtures**:
  - `tests/fixtures/excel/report_template.xlsx` – template with placeholders
  - `tests/fixtures/excel/expected_output.xlsx` – expected generated output
- [ ] **Seed data SQL** for integration tests:
  - Sample integration config
  - Sample sync schedule
  - Sample promotion candidate with proposed DDL
- [ ] **Postman collection** update:
  - Add Service-Now integration endpoints
  - Add promotion management endpoints

**AC:**
- [ ] Integration tests can run against fixtures without real Service-Now instance
- [ ] Seed data loads without errors

---

## P7-W3-003: Service README & Documentation

**Type:** Documentation
**Effort:** 1 MD

**Tasks:**
- [ ] **engine-integrations:servicenow README.md**:
  - Purpose, Dapr app-id, port allocation
  - Mermaid sequence diagram: Admin → Config → Schedule → Fetch → Transform → Store
  - Environment variables reference
  - API endpoint list
- [ ] **processor-generators:xls README.md**:
  - Purpose, Dapr app-id, gRPC service definition
  - Mermaid diagram: Request → Load Template → Fill Data → Generate → Store → Return URL
- [ ] **FS24 Documentation**:
  - Mermaid flowchart: Usage Tracking → Detection → Proposal → Admin Review → Table Creation → Routing
  - Admin guide for promotion workflow
- [ ] Update `docs/roadmap.md` with P7 phase details

**AC:**
- [ ] All new services have README with Mermaid diagrams
- [ ] Roadmap reflects P7 deliverables

---
