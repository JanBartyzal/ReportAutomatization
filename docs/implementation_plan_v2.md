# Implementation Plan
**Verze:** 2.0 – aktualizováno o FS17–FS21  
**Celkový rozsah:** 9 fází, 30 microservices  

> **Poznámka k MD:** frontend MD zahrnuje všechny obrazovky napříč fázemi, ale je zaúčtován do Phase 1 jako celek. Nové obrazovky (formuláře, lifecycle UI, PPTX generátor) jsou rozepsány v příslušných fázích jako přírůstkový effort.

---

## Phase 1 – MVP Core (M1–2)
**Cíl:** End-to-end průchod jednoho PPTX souboru – upload, parsování, uložení, základní viewer.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| router | API Gateway | Traefik reverse proxy – routing, SSL terminace, rate limiting, ForwardAuth | Traefik (config) | 5 |
| engine-core:auth | Auth Service | Validace Azure Entra ID tokenů, RBAC engine, KeyVault integrace, API key validace | Java 21 + Spring Boot | 30 |
| engine-ingestor | File Ingestor | Streaming upload do Blob, MIME validace, metadata zápis, sanitizace, trigger N8N webhook | Java 21 + Spring Boot | 25 |
| engine-ingestor:scanner | Security Scanner | Antivirová kontrola přes ICAP/ClamAV sidecar | ClamAV (sidecar/container) | 5 |
| processor-atomizers:pptx | PPTX Atomizer | Extrakce struktury, textů, tabulek a slide images z PPTX souborů | Python + FastAPI | 45 |
| engine-data:sink-tbl | Table API (Sink) | Ukládání strukturovaných dat (tabulky, OPEX) do PostgreSQL | Java 21 + Spring Boot | 12 |
| engine-data:sink-log | Log API (Sink) | Audit trail zpracování souborů – zápis processing logů | Java 21 + Spring Boot | 5 |
| engine-orchestrator | N8N Orchestrator | Business workflow engine – routing, batch processing, retry, circuit breaker, DLQ | N8N (JSON workflows) | 40 |
| frontend | Frontend SPA | React SPA – upload, viewer, dashboardy, notifikace (WebSocket/SSE), MSAL auth *(MD zahrnuje všechny fáze)* | React 18 + Vite + TS + Tailwind | 45 |
| **Celkem** | | | | **212** |

---

## Phase 2 – Extended Parsing (M3–4)
**Cíl:** Plná podpora formátů, RLS, Schema Mapping základ, BI dashboardy.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| processor-atomizers:xls | Excel Atomizer | Parsování Excel souborů per-sheet do JSON, partial success handling | Python + FastAPI | 20 |
| processor-atomizers:pdf | PDF/OCR Atomizer | OCR a extrakce textu ze skenovaných PDF dokumentů | Python + FastAPI | 15 |
| processor-atomizers:csv | CSV Atomizer | Konverze CSV souborů na strukturovaný JSON | Python + FastAPI | 5 |
| processor-atomizers:cleanup | Cleanup Worker | Cron/sidecar pro mazání dočasných souborů z Blob storage po expiraci | Python (CronJob) | 5 |
| engine-data:query | Query API (Read) | CQRS read model – optimalizované čtení pro frontend, caching (Redis) | Java 21 + Spring Boot | 15 |
| engine-data:dashboard | Dashboard Aggregation | Endpointy pro grafy, souhrny, Group By / Sort, SQL nad JSON tabulkami | Java 21 + Spring Boot | 40 |
| **Celkem** | | | | **100** |

---

## Phase 3 – Intelligence & Admin (M5–6)
**Cíl:** Holdingová hierarchie, AI integrace, Schema Mapping s learningem.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-core:admin | Admin Backend | Správa rolí (Admin/Editor/Viewer/Reviewer), holdingová hierarchie, secrets, API keys, Failed Jobs UI | Java 21 + Spring Boot | 25 |
| engine-core:batch | Batch & Org Service | Seskupování souborů do batchů, holdingová metadata, RLS enforcement | Java 21 + Spring Boot | 20 |
| engine-data:template | Template & Schema Registry | UI pro mapování sloupců, learning z historie, voláno z N8N před uložením; rozšíření: `POST /map/excel-to-form` pro FS19 | Java 21 + Spring Boot | 35 |
| processor-atomizers:ai | AI Gateway | LiteLLM integrace pro sémantickou analýzu, MetaTable logic, cost control (quotas) | Python + FastAPI | 35 |
| processor-generators:mcp | MCP Server (AI Agent) | Integrace AI agentů, On-Behalf-Of flow, token dědění, quotas | Python + FastAPI | 30 |
| **Celkem** | | | | **145** |

---

## Phase 3b – Reporting Lifecycle (M6–7)
**Cíl:** Nahradit e-mail/Excel workflow řízeným reportingovým cyklem se stavovým automatem.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-reporting:lifecycle | Report Lifecycle Service | Stavový automat reportu (Draft → Submitted → Under Review → Approved / Rejected), submission checklist, audit přechodů, publikování `report.status_changed` eventů do Dapr PubSub | Java 21 + Spring Boot | 30 |
| engine-reporting:period | Reporting Period Manager | Správa period a deadlinů (MONTHLY / QUARTERLY / ANNUAL), automatické uzavírání formulářů, eskalační notifikace, completion tracking (matice Společnost × Stav), klonování periody | Java 21 + Spring Boot | 20 |
| engine-orchestrator *(rozšíření)* | N8N – Lifecycle Workflows | Nové N8N workflow pro lifecycle eventy: subscriber `report.status_changed`, routing dle `report_type`, různé approval flows pro různé typy period | N8N (JSON workflows) | 15 |
| frontend *(rozšíření)* | Frontend – Lifecycle UI | Period dashboard (matice stavu), submission flow, rejection reason UI, timeline přechodů stavu | React | 20 |
| **Celkem** | | | | **85** |

---

## Phase 3c – Form Builder (M7–8)
**Cíl:** Centrální sběr OPEX dat přes formuláře – konec rozesílání Excel šablon e-mailem.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-reporting:form | Form Builder & Data Collection | Drag & drop editor formulářů (typy polí: text, number, percentage, date, dropdown, table, file); validační pravidla; auto-save; submission checklist; verzování formulářů; přiřazení k `period_id`; správa stavů DRAFT / PUBLISHED / CLOSED | Java 21 + Spring Boot | 55 |
| engine-reporting:form *(Excel export)* | Excel Template Export/Import | `GET /forms/{id}/export/excel-template` – vygeneruje strukturovaný Excel s validačními pravidly a skrytým metadata listem (`form_version_id`); `POST /forms/{id}/import/excel` – re-import vyplněného souboru s automatickým párováním nebo fallback na FS15 Schema Mapping | Java 21 + Spring Boot | 20 |
| engine-data:sink-tbl *(rozšíření)* | Table API – form_responses | Nová tabulka `form_responses (org_id, period_id, form_version_id, field_id, value, comment, submitted_at)` s RLS; Flyway migrace | Java 21 + Spring Boot | 8 |
| frontend *(rozšíření)* | Frontend – Form UI | Form Builder editor, formulář pro vyplnění, Excel export/import flow, field-level komentáře, validace v reálném čase | React | 30 |
| **Celkem** | | | | **113** |

---

## Phase 4 – Enterprise Features (M8–9)
**Cíl:** Compliance, verzování, notifikace, audit – production readiness.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-reporting:notification | Notification Center | In-app notifikace (WebSocket/SSE), e-mail (SMTP/SendGrid), granulární opt-in/opt-out; nové triggery: `REPORT_SUBMITTED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `DEADLINE_APPROACHING`, `DEADLINE_MISSED` | Java 21 + Spring Boot | 20 |
| engine-core:versioning | Versioning Service | Verzování dat (v1→v2), diff tool pro zobrazení změn mezi verzemi; uzamčení dat po `APPROVED` | Java 21 + Spring Boot | 16 |
| engine-core:audit | Audit & Compliance | Immutable logy (kdo-kdy-co), read access log, AI audit (prompty/odpovědi), lifecycle přechody, formulářové akce (pole změněno, komentář), export | Java 21 + Spring Boot | 25 |
| engine-data:search | Search Service | Full-text search přes PostgreSQL FTS + vector search (pgVector) | Java 21 + Spring Boot | 15 |
| **Celkem** | | | | **76** |

---

## Phase 4b – PPTX Report Generation (M9–10)
**Cíl:** Uzavřít cyklus – ze schválených dat automaticky vygenerovat standardizovaný PPTX report.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-reporting:pptx-template | PPTX Template Manager | Nahrávání a verzování centrálních PPTX šablon (scope: `CENTRAL`); extrakce placeholderů (`{{variable}}`, `{{TABLE:name}}`, `{{CHART:metric}}`); mapování placeholder → datové pole; datový model připravený na `scope` pro FS21 | Java 21 + Spring Boot | 25 |
| processor-generators:pptx | PPTX Generator | Renderování PPTX ze schválených zdrojových dat + šablony; substituce textu, tabulek a grafů; `DATA MISSING` fallback pro chybějící hodnoty; asynchronní generování + batch generování pro celou periodu | Python + FastAPI (python-pptx, matplotlib) | 35 |
| engine-orchestrator *(rozšíření)* | N8N – Generation Workflow | Workflow triggerovaný `APPROVED` eventem: načti data → generuj PPTX → ulož do Blob → notifikuj uživatele | N8N (JSON workflows) | 8 |
| frontend *(rozšíření)* | Frontend – Generator UI | Správa šablon, mapování placeholderů, trigger generování, download vygenerovaného reportu, batch generování pro periodu | React | 15 |
| **Celkem** | | | | **83** |

---

## Phase 5 – DevOps Maturity (M10–11)
**Cíl:** Production-ready observability, CI/CD, onboarding prvního holdingu.

| Unit ID / Oblast | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| Observability Stack | OpenTelemetry + Prometheus + Grafana + Loki | E2E tracing přes celý stack, metriky, centralizované logy, dashboardy pro chybovost / délku N8N fronty / Atomizer latenci | OTEL Collector, Grafana | 20 |
| CI/CD Pipelines | GitHub Actions / Azure DevOps | Linting → Unit Testy → Integration Testy → Docker Build → Push; oddělená pipeline pro GraalVM Native Image | YAML pipelines | 15 |
| Local Dev | Tilt / Skaffold | Kompletní topologie v lokálním K8s (Kind) nebo Docker Compose s hot-reloadem | Tilt / Skaffold | 8 |
| Onboarding | First Holding | Konfigurace AAD tenant, KeyVault, první perioda, první formulář, tuning Schema Mapping z reálných dat | — | 15 |
| **Celkem** | | | | **58** |

---

## Phase 6 – Local Scope & Advanced Analytics (M12+)
**Cíl:** Platforma využitelná i pro interní reporting dceřiných společností; pokročilé srovnání period.

| Unit ID | Název | Popis | Tech Stack | MD |
|---|---|---|---|---|
| engine-reporting:form *(rozšíření – FS21)* | Local Forms | Podpora `scope: LOCAL` a `scope: RELEASED`; nová role CompanyAdmin; lokální lifecycle bez holdingového approval | Java 21 + Spring Boot | 25 |
| engine-reporting:pptx-template *(rozšíření – FS21)* | Local PPTX Templates | Lokální šablony pro interní reporty; sdílení šablon v rámci holdingu (`scope: SHARED_WITHIN_HOLDING`) | Java 21 + Spring Boot | 15 |
| engine-core:admin *(rozšíření – FS21)* | CompanyAdmin Role | Správa role CompanyAdmin, přehled lokálních šablon/formulářů pro HoldingAdmin, "uvolnění" lokálních dat | Java 21 + Spring Boot | 10 |
| engine-reporting:period *(rozšíření – FS22)* | Advanced Period Comparison | Konfigurovatelné KPI pro srovnání, multi-org benchmarking, drill-down dle cost center *(placeholder – detailní spec later)* | Java 21 + Spring Boot | TBD |
| frontend *(rozšíření)* | Frontend – Local Scope UI | CompanyAdmin rozhraní, local form editor, sdílení šablon, advanced comparison dashboard | React | TBD |
| **Celkem (bez TBD)** | | | | **50+** |

---

## Souhrnný přehled

| Fáze | Měsíce | MD | Kumulativní MD | Klíčový výstup |
|---|---|---|---|---|
| Phase 1 – MVP Core | M1–2 | 212 | 212 | Upload + PPTX extrakce + viewer |
| Phase 2 – Extended Parsing | M3–4 | 100 | 312 | Všechny formáty + BI dashboardy |
| Phase 3 – Intelligence & Admin | M5–6 | 145 | 457 | AI, Schema Mapping, holdingová hierarchie |
| Phase 3b – Reporting Lifecycle | M6–7 | 85 | 542 | Stavový automat, periody, deadliny |
| Phase 3c – Form Builder | M7–8 | 113 | 655 | Centrální sběr dat, Excel export/import |
| Phase 4 – Enterprise Features | M8–9 | 76 | 731 | Audit, versioning, notifikace |
| Phase 4b – PPTX Generator | M9–10 | 83 | 814 | Automatické generování standardizovaných reportů |
| Phase 5 – DevOps & Onboarding | M10–11 | 58 | 872 | Production-ready, první holding live |
| Phase 6 – Local Scope & Advanced | M12+ | 50+ | 920+ | Lokální reporting, pokročilé srovnání |

> **Upozornění na duplicitu v původním plánu:** engine-data:dashboard byl uveden v Phase 2 i Phase 4 (oba s 40 MD). V aktualizovaném plánu je engine-data:dashboard pouze v Phase 2. Phase 4 obsahuje engine-core:audit, engine-reporting:notification, engine-core:versioning a engine-data:search, které odpovídají FS13–FS16.

---

*Implementation Plan v2.0 | PPTX Analyzer & Automation Platform | Únor 2026*
