Roadmap
Phase 0 – Generali & GOSP approval (M0)
Goal: Obtaining approval for implementation using MS Azure
Prior to commencing implementation, the necessary groundwork must be established to formally initiate a new platform initiative intended to serve a broad base of target users across the group. Preliminary assessment confirms that no comparable solution of this scope is currently deployed within the group.
Phase 1 – MVP: End-to-End ingestion (M1–2) 
Goal: Prove the core data pipeline works end-to-end for a single file. 
Infrastructure: Azure ACA, PostgreSQL + pgVector, Blob Storage, Nginx API Gateway + Azure Front Door (WAF + SSL). 
Dev: File Ingestor (streaming upload, MIME validation – REST edge endpoint), PPTX Atomizer (text + table extraction – Dapr gRPC only), Custom Orchestrator engine-orchestrator (Spring State Machine, Saga Pattern, Dapr gRPC for all internal service calls, ingest → parse → store), Auth Service (Azure Entra ID, RBAC – REST for auth_request). All internal communication via Dapr gRPC sidecars; REST exposed only on edge services (upload, query, auth).
Security: ForwardAuth on API Gateway, KeyVault for secrets.
Output: User uploads a PPTX file, extracted text and tables appear in DB and basic viewer.
Note: phase 1 covers approximately 75% of the original assignment

Phase 2 – Intelligence & visualization (M3–4)
Goal: Full format support, dashboards, and data normalization. Infrastructure: N/A. 
Dev: Excel Atomizer (per-sheet parsing, partial success – Dapr gRPC), PDF/OCR Atomizer (Dapr gRPC), Schema Mapping Registry (column normalization – Dapr gRPC from engine-orchestrator), engine-data:query + engine-data:dashboard (CQRS read model – REST edge endpoints for frontend), React dashboards with drill-down and period-on-period comparison, Azure OpenAI integration for data cleaning and semantic analysis.
Security: Row-Level Security in PostgreSQL (tenant isolation), ClamAV antivirus sidecar.
Output: All major file formats parsed, data normalized, holding-level dashboards live.

Phase 3 – Reporting lifecycle (M5–6) 
Goal: Replace the email-Excel workflow with a managed reporting cycle. Infrastructure: N/A. 
Dev: Report Lifecycle Service (state machine: Draft → Submitted → Approved/Rejected – REST edge API for frontend, Dapr Pub/Sub events to engine-orchestrator), Reporting Period Manager (deadlines, automatic form closure, escalation reminders), Form Builder – central scope (dynamic forms, Excel template export/import, validation rules, submission checklist), engine-orchestrator workflows for lifecycle events (Dapr Pub/Sub subscription, Dapr gRPC orchestration of approval flow, rejection with comments, deadline notifications).
Output: Subsidiaries collect and submit OPEX data through the platform. HoldingAdmin has a real-time matrix of who delivered and what state each report is in.

Phase 4 – Report generation & production hardening (M7–8) 
Goal: Close the loop from approved data to standardized output report, and prepare for production load. Infrastructure: WAF (Azure Front Door), autoscaling rules for Atomizer layer, monitoring (OpenTelemetry, Prometheus, Grafana, Loki). 
Dev: PPTX Template Manager (upload central template, define placeholder mappings – REST via engine-core:admin proxy), PPTX Generator (automated report rendering from approved data, batch generation – Dapr gRPC from engine-orchestrator), Versioning & Diff Tool, Audit & Compliance Log, Notifications (Dapr Pub/Sub + SMTP), Dead Letter Queue for failed processes.
Output: Approved data automatically generates a standardized PPTX report. Full audit trail. System is production-ready.

Phase 5 – Rollout & optimization (M9–10) 
Goal: Onboard first holding companies, stabilize under real data. 
Dev: MCP Server for AI agent integration, Tilt/Skaffold local dev environment, CI/CD pipeline hardening, tuning Schema Mapping learning from real column patterns. 
Output: First live holding onboarded. AI-assisted data Q&A available for CFO-level users.

Phase 6 – Local scope & advanced analytics (M11+) 
Goal: Extend the platform for subsidiary-level internal use and deeper period analysis. 
Dev: Local Forms & PPTX Templates (subsidiaries can create their own forms and report templates for internal reporting; data can be optionally released to holding), Advanced Period Comparison (configurable KPIs, multi-org benchmarking, drill-down by cost center). 
Output: Platform usable as a standalone reporting tool at the subsidiary level, not just as a holding consolidation layer.

 
Implementation plan
Phase 1 – MVP Core
Goal: End-to-end traversal of a single PPTX file – upload, parsing, saving, basic viewer.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
router | router | API Gateway | Nginx (config) | 1 | 1 | 0
engine-core | engine-core:auth | Auth Service | Java 21 + Spring Boot | 30 | 9 | 21
engine-ingestorESTOR | engine-ingestor | File Ingestor | Java 21 + Spring Boot | 25 | 8 | 17
engine-ingestorESTOR | engine-ingestor:scanner | Security Scanner | ClamAV (sidecar) | 5 | 4 | 1
processor-atomizers | processor-atomizers:pptx | PPTX Atomizer | Python + FastAPI | 35 | 16 | 19
engine-data | engine-data:sink-tbl | Table API (Sink) | Java 21 + Spring Boot | 12 | 5 | 7
engine-data | engine-data:sink-doc | Document API (Sink) | Java 21 + Spring Boot | 10 | 5 | 5
engine-data | engine-data:sink-log | Log API (Sink) | Java 21 + Spring Boot | 5 | 2 | 3
engine-orchestrator | engine-orchestrator | Custom Orchestrator | Java 21 + Spring Boot (Spring State Machine) | 45 | 18 | 27
frontend | frontend | Frontend SPA (MD zahrnuje všechny fáze) | React 18 + Vite + TS + Tailwind | 45 | 20 | 25


Phase 2 – Extended parsing 
Goal: Full format support, RLS, Schema Mapping basics, BI dashboards.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
processor-atomizers | processor-atomizers:xls | Excel Atomizer | Python + FastAPI | 15 | 8 | 7
processor-atomizers | processor-atomizers:pdf | PDF/OCR Atomizer | Python + FastAPI | 15 | 7 | 8
processor-atomizers | processor-atomizers:csv | CSV Atomizer | Python + FastAPI | 4 | 2 | 2
processor-atomizers | processor-atomizers:cleanup | Cleanup Worker | Python (CronJob) | 5 | 3 | 2
engine-data | engine-data:query | Query API (Read) | Java 21 + Spring Boot | 12 | 6 | 6
engine-data | engine-data:dashboard | Dashboard Aggregation | Java 21 + Spring Boot | 35 | 17 | 18

Phase 3 – Intelligence & admin
Objective: Holding hierarchy, AI integration, Schema Mapping with learning.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-core | engine-core:admin | Admin Backend | Java 21 + Spring Boot | 20 | 12 | 8
engine-core | engine-core:batch | Batch & Org Service | Java 21 + Spring Boot | 15 | 8 | 7
engine-data | engine-data:template | Template & Schema Registry | Java 21 + Spring Boot | 30 | 17 | 13
processor | processor-atomizers:ai | AI Gateway | Python + FastAPI | 3 | 2 | 1
processor | processor-atomizers:ai | AI P/rompting | Prompts | 10 | 8 | 2
processor | processor-generators:mcp | MCP Server (AI Agent) | Python + FastAPI | 12 | 8 | 4

  
Phase 3b – Reporting lifecycle
Goal: Replace email/Excel workflow with a controlled reporting cycle with a state machine.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-reporting | engine-reporting:lifecycle | Report Lifecycle Service | Java 21 + Spring Boot | 25 | 12 | 13
engine-reporting | engine-reporting:period | Reporting Period Manager | Java 21 + Spring Boot | 15 | 8 | 7
engine-orchestrator | engine-orchestrator (rozšíření) | Orchestrator – Lifecycle Workflows | Java 21 + Spring Boot | 15 | 12 | 3
frontend | frontend (rozšíření) | Frontend – Lifecycle UI | React | 20 | 8 | 12

Phase 3c – Form builder
Goal: Central collection of OPEX data via forms – no more sending Excel templates by email.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-reporting | engine-reporting:form | Form Builder & Data Collection | Java 21 + Spring Boot | 40 | 24 | 16
engine-reporting | engine-reporting:form (Excel export/import) | Excel Template Export/Import | Java 21 + Spring Boot | 8 | 2 | 6
engine-data | engine-data:sink-tbl (rozšíření) | Table API – form_responses | Java 21 + Spring Boot | 8 | 2 | 6
frontend | frontend (rozšíření) | Frontend – Form UI | React | 25 | 15 | 10

Phase 4 – Enterprise features
Goal: Compliance, versioning, notification, audit – production readiness.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-core | engine-reporting:notification | Notification Center | Java 21 + Spring Boot | 15 | 8 | 7
engine-core | engine-core:versioning | Versioning Service | Java 21 + Spring Boot | 16 | 7 | 9
engine-core | engine-core:audit | Audit & Compliance | Java 21 + Spring Boot | 25 | 10 | 15
engine-data | engine-data:search | Search Service | Java 21 + Spring Boot | 15 | 4 | 11

Phase 4b – PPTX report generation
Goal: Close the cycle – automatically generate a standardized PPTX report from approved data.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-reporting | engine-reporting:pptx-template | PPTX Template Manager | Java 21 + Spring Boot | 20 | 9 | 11
processor | processor-generators:pptx | PPTX Generator | Python + FastAPI (python-pptx, matplotlib) | 32 | 16 | 16
engine-orchestrator | engine-orchestrator (rozšíření) | Orchestrator – Generation Workflow | Java 21 + Spring Boot | 8 | 6 | 2
frontend | frontend (rozšíření) | Frontend – Generator UI | React | 15 | 4 | 11

Phase5 - onboarding

Phase 6 – Local scope & advanced analytics
Goal: Platform for internal reporting of subsidiaries; advanced period comparison.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
engine-reporting | engine-reporting:form (rozšíření – FS21) | Local Forms | Java 21 + Spring Boot | 20 | 14 | 6
engine-reporting | engine-reporting:pptx-template (rozšíření – FS21) | Local PPTX Templates | Java 21 + Spring Boot | 15 | 8 | 7
engine-core | engine-core:admin (rozšíření – FS21) | CompanyAdmin Role | Java 21 + Spring Boot | 10 | 5 | 5
engine-reporting | engine-reporting:period (rozšíření – FS22) | Advanced Period Comparison | Java 21 + Spring Boot | TBD | TBD | ---
frontend | frontend (rozšíření) | Frontend – Local Scope UI | React | TBD | TBD | ---



## Katalog Microservices (Units)

| # | Unit ID | Function ID | Název | Popis | FeatureSet | Tech Stack | Effort |
|---|---------|-------------|-------|-------|------------|------------|--------|
| 1 | frontend | **frontend** | Frontend SPA | React SPA – upload, viewer, dashboardy, notifikace (WebSocket/SSE), MSAL auth | FS09, FS11 | React 18 + Vite + TS + Tailwind | **XL** |
| 2 | router | **router** | API Gateway | Nginx – Host-based routing, Azure Front Door (WAF + SSL), rate limiting (100/10 req/s, burst 20), ForwardAuth | FS01 | Nginx (config) | **S** |
| 3 | engine-core | **engine-core:auth** | Auth Service | Validace Azure Entra ID tokenů, RBAC engine, KeyVault integrace, API key validace | FS01, FS07 | Java 21 + Spring Boot | **L** |
| 4 | engine-ingestorESTOR | **engine-ingestor** | File Ingestor | Streaming upload do Blob, MIME validace, metadata zápis, sanitizace, trigger engine-orchestrator (Dapr PubSub / gRPC) | FS02 | Java 21 + Spring Boot | **L** |
| 5 | engine-ingestorESTOR | **engine-ingestor:scanner** | Security Scanner | Antivirová kontrola přes ICAP/ClamAV sidecar | FS02 | ClamAV (sidecar/container) | **S** |
| 6 | engine-orchestrator | **engine-orchestrator** | Custom Orchestrator | Workflow engine (Spring State Machine), Saga Pattern, Type-Safe Contracts, gRPC, Redis state, exponential backoff retry, DLQ | FS04 | Java 21 + Spring Boot | **XL** |
| 7 | processor-atomizers | **processor-atomizers:pptx** | PPTX Atomizer | Extrakce struktury, textů, tabulek a slide images z PPTX souborů | FS03 | Python + FastAPI | **L** |
| 8 | processor-atomizers | **processor-atomizers:xls** | Excel Atomizer | Parsování Excel souborů per-sheet do JSON, partial success handling | FS03, FS10 | Python + FastAPI | **M** |
| 9 | processor-atomizers | **processor-atomizers:pdf** | PDF/OCR Atomizer | OCR a extrakce textu ze skenovaných PDF dokumentů | FS03 | Python + FastAPI | **M** |
| 10 | processor-atomizers | **processor-atomizers:csv** | CSV Atomizer | Konverze CSV souborů na strukturovaný JSON | FS03 | Python + FastAPI | **S** |
| 11 | processor | **processor-atomizers:ai** | AI Gateway | LiteLLM integrace pro sémantickou analýzu, MetaTable logic, cost control (quotas) | FS03, FS12 | Python + FastAPI | **L** |
| 12 | processor-atomizers | **processor-atomizers:cleanup** | Cleanup Worker | Cron/sidecar pro mazání dočasných souborů z Blob storage po expiraci | FS03 | Python (CronJob) | **S** |
| 13 | engine-data | **engine-data:sink-tbl** | Table API (Sink) | Ukládání strukturovaných dat (tabulky, OPEX) do PostgreSQL | FS05 | Java 21 + Spring Boot | **M** |
| 14 | engine-data | **engine-data:sink-doc** | Document API (Sink) | Ukládání nestrukturovaného JSONu + vector embeddings (pgVector) | FS05 | Java 21 + Spring Boot | **M** |
| 15 | engine-data | **engine-data:sink-log** | Log API (Sink) | Audit trail zpracování souborů – zápis processing logů | FS05 | Java 21 + Spring Boot | **S** |
| 16 | engine-data | **engine-data:query** | Query API (Read) | CQRS read model – optimalizované čtení pro frontend, caching (Redis) | FS06 | Java 21 + Spring Boot | **M** |
| 17 | engine-data | **engine-data:dashboard** | Dashboard Aggregation | Endpointy pro grafy, souhrny, Group By / Sort, SQL nad JSON tabulkami | FS06, FS11 | Java 21 + Spring Boot | **L** |
| 18 | engine-data | **engine-data:search** | Search Service | Full-text search přes ElasticSearch / PostgreSQL FTS + vector search | FS06 | Java 21 + Spring Boot | **M** |
| 19 | engine-core | **engine-core:admin** | Admin Backend | Správa rolí (Admin/Editor/Viewer), holdingová hierarchie, secrets, API keys, Failed Jobs UI | FS07, FS08 | Java 21 + Spring Boot | **L** |
| 20 | engine-core | **engine-reporting:notification** | Notification Center | In-app notifikace (WebSocket/SSE), e-mail alerty (SMTP), granulární nastavení | FS13 | Java 21 + Spring Boot | **M** |
| 21 | engine-data | **engine-data:template** | Template & Schema Registry | UI pro mapování sloupců, learning z historie, voláno z engine-orchestrator (gRPC) před uložením | FS15 | Java 21 + Spring Boot | **L** |
| 22 | engine-core | **engine-core:versioning** | Versioning Service | Verzování dat (v1→v2), diff tool pro zobrazení změn mezi verzemi | FS14 | Java 21 + Spring Boot | **M** |
| 23 | engine-core | **engine-core:audit** | Audit & Compliance | Immutable logy (kdo-kdy-co), read access log, AI audit (prompty/odpovědi), export | FS16 | Java 21 + Spring Boot | **M** |
| 24 | processor | **processor-generators:mcp** | MCP Server (AI Agent) | Integrace AI agentů, On-Behalf-Of flow, token dědění, quotas | FS12 | Python + FastAPI | **L** |
| 25 | engine-core | **engine-core:batch** | Batch & Org Service | Seskupování souborů do batchů, holdingová metadata, RLS enforcement | FS08 | Java 21 + Spring Boot | **M** |
| 26 | engine-reporting | **engine-reporting:lifecycle** | Report Lifecycle Service | Správa stavového automatu reportů, submission checklist, rejection flow, hromadné akce | FS17 | Java 21 + Spring Boot | **L** |
| 27 | engine-reporting | **engine-reporting:pptx-template** | PPTX Template Manager | Nahrávání, verzování a správa PPTX šablon; extrakce placeholderů; mapování na datové zdroje | FS18 | Java 21 + Spring Boot | **L** |
| 28 | processor | **processor-generators:pptx** | PPTX Generator | Renderování PPTX ze zdrojových dat + šablony; placeholder substituce; grafy; batch generování | FS18 | Python + FastAPI (python-pptx, matplotlib) | **L** |
| 29 | engine-reporting | **engine-reporting:form** | Form Builder & Data Collection | Definice formulářů, správa verzí, sběr dat, validace, Excel import, napojení na engine-reporting:lifecycle | FS19 | Java 21 + Spring Boot | **XL** |
| 30 | engine-reporting | **engine-reporting:period** | Reporting Period Manager | Správa period a deadlinů, automatické uzavírání, completion tracking, eskalace, historické srovnání | FS20 | Java 21 + Spring Boot | **M** |
