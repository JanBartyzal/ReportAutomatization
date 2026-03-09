Roadmap
Phase 0 – Generali & GOSP approval (M0)
Goal: Obtaining approval for implementation using MS Azure
Prior to commencing implementation, the necessary groundwork must be established to formally initiate a new platform initiative intended to serve a broad base of target users across the group. Preliminary assessment confirms that no comparable solution of this scope is currently deployed within the group.
Phase 1 – MVP: End-to-End ingestion (M1–2) 
Goal: Prove the core data pipeline works end-to-end for a single file. 
Infrastructure: Azure ACA, PostgreSQL + pgVector, Blob Storage, Nginx API Gateway + Azure Front Door (WAF + SSL). 
Dev: File Ingestor (streaming upload, MIME validation – REST edge endpoint), PPTX Atomizer (text + table extraction – Dapr gRPC only), Custom Orchestrator MS-ORCH (Spring State Machine, Saga Pattern, Dapr gRPC for all internal service calls, ingest → parse → store), Auth Service (Azure Entra ID, RBAC – REST for auth_request). All internal communication via Dapr gRPC sidecars; REST exposed only on edge services (upload, query, auth).
Security: ForwardAuth on API Gateway, KeyVault for secrets.
Output: User uploads a PPTX file, extracted text and tables appear in DB and basic viewer.
Note: phase 1 covers approximately 75% of the original assignment

Phase 2 – Intelligence & visualization (M3–4)
Goal: Full format support, dashboards, and data normalization. Infrastructure: N/A. 
Dev: Excel Atomizer (per-sheet parsing, partial success – Dapr gRPC), PDF/OCR Atomizer (Dapr gRPC), Schema Mapping Registry (column normalization – Dapr gRPC from MS-ORCH), MS-QRY + MS-DASH (CQRS read model – REST edge endpoints for frontend), React dashboards with drill-down and period-on-period comparison, Azure OpenAI integration for data cleaning and semantic analysis.
Security: Row-Level Security in PostgreSQL (tenant isolation), ClamAV antivirus sidecar.
Output: All major file formats parsed, data normalized, holding-level dashboards live.

Phase 3 – Reporting lifecycle (M5–6) 
Goal: Replace the email-Excel workflow with a managed reporting cycle. Infrastructure: N/A. 
Dev: Report Lifecycle Service (state machine: Draft → Submitted → Approved/Rejected – REST edge API for frontend, Dapr Pub/Sub events to MS-ORCH), Reporting Period Manager (deadlines, automatic form closure, escalation reminders), Form Builder – central scope (dynamic forms, Excel template export/import, validation rules, submission checklist), MS-ORCH workflows for lifecycle events (Dapr Pub/Sub subscription, Dapr gRPC orchestration of approval flow, rejection with comments, deadline notifications).
Output: Subsidiaries collect and submit OPEX data through the platform. HoldingAdmin has a real-time matrix of who delivered and what state each report is in.

Phase 4 – Report generation & production hardening (M7–8) 
Goal: Close the loop from approved data to standardized output report, and prepare for production load. Infrastructure: WAF (Azure Front Door), autoscaling rules for Atomizer layer, monitoring (OpenTelemetry, Prometheus, Grafana, Loki). 
Dev: PPTX Template Manager (upload central template, define placeholder mappings – REST via MS-ADMIN proxy), PPTX Generator (automated report rendering from approved data, batch generation – Dapr gRPC from MS-ORCH), Versioning & Diff Tool, Audit & Compliance Log, Notifications (Dapr Pub/Sub + SMTP), Dead Letter Queue for failed processes.
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
MS-GW | MS-GW | API Gateway | Nginx (config) | 1 | 1 | 0
MS-CORE | MS-AUTH | Auth Service | Java 21 + Spring Boot | 30 | 9 | 21
MS-INGESTOR | MS-ING | File Ingestor | Java 21 + Spring Boot | 25 | 8 | 17
MS-INGESTOR | MS-SCAN | Security Scanner | ClamAV (sidecar) | 5 | 4 | 1
MS-PROCESSOR | MS-ATM-PPTX | PPTX Atomizer | Python + FastAPI | 35 | 16 | 19
MS-DATA | MS-SINK-TBL | Table API (Sink) | Java 21 + Spring Boot | 12 | 5 | 7
MS-DATA | MS-SINK-DOC | Document API (Sink) | Java 21 + Spring Boot | 10 | 5 | 5
MS-DATA | MS-SINK-LOG | Log API (Sink) | Java 21 + Spring Boot | 5 | 2 | 3
MS-ORCH | MS-ORCH | Custom Orchestrator | Java 21 + Spring Boot (Spring State Machine) | 45 | 18 | 27
MS-FE | MS-FE | Frontend SPA (MD zahrnuje všechny fáze) | React 18 + Vite + TS + Tailwind | 45 | 20 | 25


Phase 2 – Extended parsing 
Goal: Full format support, RLS, Schema Mapping basics, BI dashboards.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-PROCESSOR | MS-ATM-XLS | Excel Atomizer | Python + FastAPI | 15 | 8 | 7
MS-PROCESSOR | MS-ATM-PDF | PDF/OCR Atomizer | Python + FastAPI | 15 | 7 | 8
MS-PROCESSOR | MS-ATM-CSV | CSV Atomizer | Python + FastAPI | 4 | 2 | 2
MS-PROCESSOR | MS-ATM-CLN | Cleanup Worker | Python (CronJob) | 5 | 3 | 2
MS-DATA | MS-QRY | Query API (Read) | Java 21 + Spring Boot | 12 | 6 | 6
MS-DATA | MS-DASH | Dashboard Aggregation | Java 21 + Spring Boot | 35 | 17 | 18

Phase 3 – Intelligence & admin
Objective: Holding hierarchy, AI integration, Schema Mapping with learning.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-CORE | MS-ADMIN | Admin Backend | Java 21 + Spring Boot | 20 | 12 | 8
MS-CORE | MS-BATCH | Batch & Org Service | Java 21 + Spring Boot | 15 | 8 | 7
MS-DATA | MS-TMPL | Template & Schema Registry | Java 21 + Spring Boot | 30 | 17 | 13
MS-AI | MS-ATM-AI | AI Gateway | Python + FastAPI | 3 | 2 | 1
MS-AI | MS-ATM-AI | AI P/rompting | Prompts | 10 | 8 | 2
MS-AI | MS-MCP | MCP Server (AI Agent) | Python + FastAPI | 12 | 8 | 4

  
Phase 3b – Reporting lifecycle
Goal: Replace email/Excel workflow with a controlled reporting cycle with a state machine.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-REPORTING | MS-LIFECYCLE | Report Lifecycle Service | Java 21 + Spring Boot | 25 | 12 | 13
MS-REPORTING | MS-PERIOD | Reporting Period Manager | Java 21 + Spring Boot | 15 | 8 | 7
MS-ORCH | MS-ORCH (rozšíření) | Orchestrator – Lifecycle Workflows | Java 21 + Spring Boot | 15 | 12 | 3
MS-FE | MS-FE (rozšíření) | Frontend – Lifecycle UI | React | 20 | 8 | 12

Phase 3c – Form builder
Goal: Central collection of OPEX data via forms – no more sending Excel templates by email.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-REPORTING | MS-FORM | Form Builder & Data Collection | Java 21 + Spring Boot | 40 | 24 | 16
MS-REPORTING | MS-FORM (Excel export/import) | Excel Template Export/Import | Java 21 + Spring Boot | 8 | 2 | 6
MS-DATA | MS-SINK-TBL (rozšíření) | Table API – form_responses | Java 21 + Spring Boot | 8 | 2 | 6
MS-FE | MS-FE (rozšíření) | Frontend – Form UI | React | 25 | 15 | 10

Phase 4 – Enterprise features
Goal: Compliance, versioning, notification, audit – production readiness.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-CORE | MS-NOTIF | Notification Center | Java 21 + Spring Boot | 15 | 8 | 7
MS-CORE | MS-VER | Versioning Service | Java 21 + Spring Boot | 16 | 7 | 9
MS-CORE | MS-AUDIT | Audit & Compliance | Java 21 + Spring Boot | 25 | 10 | 15
MS-DATA | MS-SRCH | Search Service | Java 21 + Spring Boot | 15 | 4 | 11

Phase 4b – PPTX report generation
Goal: Close the cycle – automatically generate a standardized PPTX report from approved data.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-REPORTING | MS-TMPL-PPTX | PPTX Template Manager | Java 21 + Spring Boot | 20 | 9 | 11
MS-AI | MS-GEN-PPTX | PPTX Generator | Python + FastAPI (python-pptx, matplotlib) | 32 | 16 | 16
MS-ORCH | MS-ORCH (rozšíření) | Orchestrator – Generation Workflow | Java 21 + Spring Boot | 8 | 6 | 2
MS-FE | MS-FE (rozšíření) | Frontend – Generator UI | React | 15 | 4 | 11

Phase5 - onboarding

Phase 6 – Local scope & advanced analytics
Goal: Platform for internal reporting of subsidiaries; advanced period comparison.

Unit ID | Function ID | Name | Tech Stack | MD | MD (AI) | Savings
--------|---------|------|----------|----|---------|--------
MS-REPORTING | MS-FORM (rozšíření – FS21) | Local Forms | Java 21 + Spring Boot | 20 | 14 | 6
MS-REPORTING | MS-TMPL-PPTX (rozšíření – FS21) | Local PPTX Templates | Java 21 + Spring Boot | 15 | 8 | 7
MS-CORE | MS-ADMIN (rozšíření – FS21) | CompanyAdmin Role | Java 21 + Spring Boot | 10 | 5 | 5
MS-REPORTING | MS-PERIOD (rozšíření – FS22) | Advanced Period Comparison | Java 21 + Spring Boot | TBD | TBD | ---
MS-FE | MS-FE (rozšíření) | Frontend – Local Scope UI | React | TBD | TBD | ---



## Katalog Microservices (Units)

| # | Unit ID | Function ID | Název | Popis | FeatureSet | Tech Stack | Effort |
|---|---------|-------------|-------|-------|------------|------------|--------|
| 1 | MS-FE | **MS-FE** | Frontend SPA | React SPA – upload, viewer, dashboardy, notifikace (WebSocket/SSE), MSAL auth | FS09, FS11 | React 18 + Vite + TS + Tailwind | **XL** |
| 2 | MS-GW | **MS-GW** | API Gateway | Nginx – Host-based routing, Azure Front Door (WAF + SSL), rate limiting (100/10 req/s, burst 20), ForwardAuth | FS01 | Nginx (config) | **S** |
| 3 | MS-CORE | **MS-AUTH** | Auth Service | Validace Azure Entra ID tokenů, RBAC engine, KeyVault integrace, API key validace | FS01, FS07 | Java 21 + Spring Boot | **L** |
| 4 | MS-INGESTOR | **MS-ING** | File Ingestor | Streaming upload do Blob, MIME validace, metadata zápis, sanitizace, trigger MS-ORCH (Dapr PubSub / gRPC) | FS02 | Java 21 + Spring Boot | **L** |
| 5 | MS-INGESTOR | **MS-SCAN** | Security Scanner | Antivirová kontrola přes ICAP/ClamAV sidecar | FS02 | ClamAV (sidecar/container) | **S** |
| 6 | MS-ORCH | **MS-ORCH** | Custom Orchestrator | Workflow engine (Spring State Machine), Saga Pattern, Type-Safe Contracts, gRPC, Redis state, exponential backoff retry, DLQ | FS04 | Java 21 + Spring Boot | **XL** |
| 7 | MS-PROCESSOR | **MS-ATM-PPTX** | PPTX Atomizer | Extrakce struktury, textů, tabulek a slide images z PPTX souborů | FS03 | Python + FastAPI | **L** |
| 8 | MS-PROCESSOR | **MS-ATM-XLS** | Excel Atomizer | Parsování Excel souborů per-sheet do JSON, partial success handling | FS03, FS10 | Python + FastAPI | **M** |
| 9 | MS-PROCESSOR | **MS-ATM-PDF** | PDF/OCR Atomizer | OCR a extrakce textu ze skenovaných PDF dokumentů | FS03 | Python + FastAPI | **M** |
| 10 | MS-PROCESSOR | **MS-ATM-CSV** | CSV Atomizer | Konverze CSV souborů na strukturovaný JSON | FS03 | Python + FastAPI | **S** |
| 11 | MS-AI | **MS-ATM-AI** | AI Gateway | LiteLLM integrace pro sémantickou analýzu, MetaTable logic, cost control (quotas) | FS03, FS12 | Python + FastAPI | **L** |
| 12 | MS-PROCESSOR | **MS-ATM-CLN** | Cleanup Worker | Cron/sidecar pro mazání dočasných souborů z Blob storage po expiraci | FS03 | Python (CronJob) | **S** |
| 13 | MS-DATA | **MS-SINK-TBL** | Table API (Sink) | Ukládání strukturovaných dat (tabulky, OPEX) do PostgreSQL | FS05 | Java 21 + Spring Boot | **M** |
| 14 | MS-DATA | **MS-SINK-DOC** | Document API (Sink) | Ukládání nestrukturovaného JSONu + vector embeddings (pgVector) | FS05 | Java 21 + Spring Boot | **M** |
| 15 | MS-DATA | **MS-SINK-LOG** | Log API (Sink) | Audit trail zpracování souborů – zápis processing logů | FS05 | Java 21 + Spring Boot | **S** |
| 16 | MS-DATA | **MS-QRY** | Query API (Read) | CQRS read model – optimalizované čtení pro frontend, caching (Redis) | FS06 | Java 21 + Spring Boot | **M** |
| 17 | MS-DATA | **MS-DASH** | Dashboard Aggregation | Endpointy pro grafy, souhrny, Group By / Sort, SQL nad JSON tabulkami | FS06, FS11 | Java 21 + Spring Boot | **L** |
| 18 | MS-DATA | **MS-SRCH** | Search Service | Full-text search přes ElasticSearch / PostgreSQL FTS + vector search | FS06 | Java 21 + Spring Boot | **M** |
| 19 | MS-CORE | **MS-ADMIN** | Admin Backend | Správa rolí (Admin/Editor/Viewer), holdingová hierarchie, secrets, API keys, Failed Jobs UI | FS07, FS08 | Java 21 + Spring Boot | **L** |
| 20 | MS-CORE | **MS-NOTIF** | Notification Center | In-app notifikace (WebSocket/SSE), e-mail alerty (SMTP), granulární nastavení | FS13 | Java 21 + Spring Boot | **M** |
| 21 | MS-DATA | **MS-TMPL** | Template & Schema Registry | UI pro mapování sloupců, learning z historie, voláno z MS-ORCH (gRPC) před uložením | FS15 | Java 21 + Spring Boot | **L** |
| 22 | MS-CORE | **MS-VER** | Versioning Service | Verzování dat (v1→v2), diff tool pro zobrazení změn mezi verzemi | FS14 | Java 21 + Spring Boot | **M** |
| 23 | MS-CORE | **MS-AUDIT** | Audit & Compliance | Immutable logy (kdo-kdy-co), read access log, AI audit (prompty/odpovědi), export | FS16 | Java 21 + Spring Boot | **M** |
| 24 | MS-AI | **MS-MCP** | MCP Server (AI Agent) | Integrace AI agentů, On-Behalf-Of flow, token dědění, quotas | FS12 | Python + FastAPI | **L** |
| 25 | MS-CORE | **MS-BATCH** | Batch & Org Service | Seskupování souborů do batchů, holdingová metadata, RLS enforcement | FS08 | Java 21 + Spring Boot | **M** |
| 26 | MS-REPORTING | **MS-LIFECYCLE** | Report Lifecycle Service | Správa stavového automatu reportů, submission checklist, rejection flow, hromadné akce | FS17 | Java 21 + Spring Boot | **L** |
| 27 | MS-REPORTING | **MS-TMPL-PPTX** | PPTX Template Manager | Nahrávání, verzování a správa PPTX šablon; extrakce placeholderů; mapování na datové zdroje | FS18 | Java 21 + Spring Boot | **L** |
| 28 | MS-AI | **MS-GEN-PPTX** | PPTX Generator | Renderování PPTX ze zdrojových dat + šablony; placeholder substituce; grafy; batch generování | FS18 | Python + FastAPI (python-pptx, matplotlib) | **L** |
| 29 | MS-REPORTING | **MS-FORM** | Form Builder & Data Collection | Definice formulářů, správa verzí, sběr dat, validace, Excel import, napojení na MS-LIFECYCLE | FS19 | Java 21 + Spring Boot | **XL** |
| 30 | MS-REPORTING | **MS-PERIOD** | Reporting Period Manager | Správa period a deadlinů, automatické uzavírání, completion tracking, eskalace, historické srovnání | FS20 | Java 21 + Spring Boot | **M** |
