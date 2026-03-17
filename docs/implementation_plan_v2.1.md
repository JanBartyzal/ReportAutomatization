# Implementation Plan
**Verze:** 2.1 – doplněn odhad MD s AI Agentem  
**Celkový rozsah:** 9 fází, 30 microservices

> **Poznámka k MD:** frontend MD zahrnuje všechny obrazovky napříč fázemi, ale je zaúčtován do Phase 1 jako celek. Nové obrazovky (formuláře, lifecycle UI, PPTX generátor) jsou rozepsány v příslušných fázích jako přírůstkový effort.

---

## Předpoklady – AI Agent asistence

Sloupec **MD (AI)** vychází z předpokladu, že AI Agent (Claude Code nebo ekvivalent) zajišťuje:

- ✅ Generování boilerplate kódu (DTOs, repositories, CRUD endpointy, Flyway migrace)
- ✅ Scaffolding Spring Boot a FastAPI služeb dle definovaného kontraktu
- ✅ Generování unit testů (happy path + edge cases) z existující logiky
- ✅ React komponenty a formuláře ze specifikace nebo wireframu
- ✅ Převod OpenAPI specifikace na funkční skeleton implementace

AI Agent **nenahrazuje**:

- ❌ Architektonická rozhodnutí a security review (auth, RLS, token flow)
- ❌ Návrh N8N workflow business logiky (routing, retry strategie)
- ❌ Komplexní business logiku (state machine design, learning algoritmus Schema Mappingu)
- ❌ Integrační testování a manuální QA
- ❌ DevOps konfiguraci a onboarding

### Redukční faktory dle kategorie

| Kategorie | Příklady | Redukce |
|---|---|---|
| Config-only | router, engine-ingestor:scanner | 20 % |
| Standard Java CRUD | engine-data (sink modules), engine-data:query, engine-reporting:notification, engine-core:versioning, engine-data:search, engine-reporting:period, engine-core:batch | 45 % |
| Komplexní Java service | engine-core:auth, engine-ingestor, engine-core:admin, engine-data:template, engine-reporting:lifecycle, engine-reporting:form, engine-core:audit, engine-reporting:pptx-template | 32 % |
| Python Atomizer / FastAPI | processor-atomizers, processor-atomizers:ai, processor-generators:mcp, processor-generators:pptx | 40 % |
| N8N workflows | engine-orchestrator a jeho rozšíření | 22 % |
| React Frontend | frontend a jeho rozšíření | 47 % |
| Dashboard / agregace | engine-data:dashboard | 35 % |
| DevOps & Infra | Observability, CI/CD, Local Dev | 27 % |
| Onboarding / konzultace | First Holding setup | 0 % |

---

## Phase 1 – MVP Core (M1–2)
**Cíl:** End-to-end průchod jednoho PPTX souboru – upload, parsování, uložení, základní viewer.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| router | API Gateway | Traefik (config) | 5 | 4 | 1 |
| engine-core:auth | Auth Service | Java 21 + Spring Boot | 30 | 20 | 10 |
| engine-ingestor | File Ingestor | Java 21 + Spring Boot | 25 | 17 | 8 |
| engine-ingestor:scanner | Security Scanner | ClamAV (sidecar) | 5 | 4 | 1 |
| processor-atomizers:pptx | PPTX Atomizer | Python + FastAPI | 45 | 27 | 18 |
| engine-data:sink-tbl | Table API (Sink) | Java 21 + Spring Boot | 12 | 7 | 5 |
| engine-data:sink-log | Log API (Sink) | Java 21 + Spring Boot | 5 | 3 | 2 |
| engine-orchestrator | N8N Orchestrator | N8N (JSON workflows) | 40 | 31 | 9 |
| frontend | Frontend SPA *(MD zahrnuje všechny fáze)* | React 18 + Vite + TS + Tailwind | 45 | 24 | 21 |
| **Celkem** | | | **212** | **137** | **75 (35 %)** |

---

## Phase 2 – Extended Parsing (M3–4)
**Cíl:** Plná podpora formátů, RLS, Schema Mapping základ, BI dashboardy.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| processor-atomizers:xls | Excel Atomizer | Python + FastAPI | 20 | 12 | 8 |
| processor-atomizers:pdf | PDF/OCR Atomizer | Python + FastAPI | 15 | 9 | 6 |
| processor-atomizers:csv | CSV Atomizer | Python + FastAPI | 5 | 3 | 2 |
| processor-atomizers:cleanup | Cleanup Worker | Python (CronJob) | 5 | 3 | 2 |
| engine-data:query | Query API (Read) | Java 21 + Spring Boot | 15 | 8 | 7 |
| engine-data:dashboard | Dashboard Aggregation | Java 21 + Spring Boot | 40 | 26 | 14 |
| **Celkem** | | | **100** | **61** | **39 (39 %)** |

---

## Phase 3 – Intelligence & Admin (M5–6)
**Cíl:** Holdingová hierarchie, AI integrace, Schema Mapping s learningem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-core:admin | Admin Backend | Java 21 + Spring Boot | 25 | 17 | 8 |
| engine-core:batch | Batch & Org Service | Java 21 + Spring Boot | 20 | 11 | 9 |
| engine-data:template | Template & Schema Registry | Java 21 + Spring Boot | 35 | 24 | 11 |
| processor-atomizers:ai | AI Gateway | Python + FastAPI | 35 | 21 | 14 |
| processor-generators:mcp | MCP Server (AI Agent) | Python + FastAPI | 30 | 20 | 10 |
| **Celkem** | | | **145** | **93** | **52 (36 %)** |

> **Poznámka engine-data:template:** Learning algoritmus Schema Mappingu (predikce mapování z historie) je náročný na logiku – AI Agent generuje strukturu, ale algoritmus musí být navržen člověkem. Proto nižší redukce (30 %) oproti standardnímu CRUD.

---

## Phase 3b – Reporting Lifecycle (M6–7)
**Cíl:** Nahradit e-mail/Excel workflow řízeným reportingovým cyklem se stavovým automatem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-reporting:lifecycle | Report Lifecycle Service | Java 21 + Spring Boot | 30 | 20 | 10 |
| engine-reporting:period | Reporting Period Manager | Java 21 + Spring Boot | 20 | 11 | 9 |
| engine-orchestrator *(rozšíření)* | N8N – Lifecycle Workflows | N8N (JSON workflows) | 15 | 12 | 3 |
| frontend *(rozšíření)* | Frontend – Lifecycle UI | React | 20 | 11 | 9 |
| **Celkem** | | | **85** | **54** | **31 (36 %)** |

> **Poznámka engine-reporting:lifecycle:** Stavový automat je přímočarý na implementaci, ale validační podmínky přechodů (RBAC + business rules) a edge cases vyžadují důkladný lidský review. AI generuje strukturu i testy, lidský vývojář ověřuje korektnost přechodových pravidel.

---

## Phase 3c – Form Builder (M7–8)
**Cíl:** Centrální sběr OPEX dat přes formuláře – konec rozesílání Excel šablon e-mailem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-reporting:form | Form Builder & Data Collection | Java 21 + Spring Boot | 55 | 38 | 17 |
| engine-reporting:form *(Excel export/import)* | Excel Template Export/Import | Java 21 + Spring Boot | 20 | 13 | 7 |
| engine-data:sink-tbl *(rozšíření)* | Table API – form_responses | Java 21 + Spring Boot | 8 | 4 | 4 |
| frontend *(rozšíření)* | Frontend – Form UI | React | 30 | 16 | 14 |
| **Celkem** | | | **113** | **71** | **42 (37 %)** |

> **Poznámka engine-reporting:form:** Form engine (dynamické typy polí, závislostní validace, verzování) je nejkomplexnější nová service. AI Agent generuje scaffolding a CRUD, ale validační engine a verzovací logiku musí vývojář navrhnout a reviewovat ručně – odtud konzervativnějších 30 %.

---

## Phase 4 – Enterprise Features (M8–9)
**Cíl:** Compliance, verzování, notifikace, audit – production readiness.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-reporting:notification | Notification Center | Java 21 + Spring Boot | 20 | 11 | 9 |
| engine-core:versioning | Versioning Service | Java 21 + Spring Boot | 16 | 9 | 7 |
| engine-core:audit | Audit & Compliance | Java 21 + Spring Boot | 25 | 17 | 8 |
| engine-data:search | Search Service | Java 21 + Spring Boot | 15 | 8 | 7 |
| **Celkem** | | | **76** | **45** | **31 (41 %)** |

> **Poznámka engine-core:audit:** Immutable log a compliance export jsou technicky přímočaré (append-only tabulka, export endpoint) – proto vyšší redukce i přes bezpečnostní citlivost. Security review zůstává na člověku.

---

## Phase 4b – PPTX Report Generation (M9–10)
**Cíl:** Uzavřít cyklus – ze schválených dat automaticky vygenerovat standardizovaný PPTX report.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-reporting:pptx-template | PPTX Template Manager | Java 21 + Spring Boot | 25 | 17 | 8 |
| processor-generators:pptx | PPTX Generator | Python + FastAPI (python-pptx, matplotlib) | 35 | 21 | 14 |
| engine-orchestrator *(rozšíření)* | N8N – Generation Workflow | N8N (JSON workflows) | 8 | 6 | 2 |
| frontend *(rozšíření)* | Frontend – Generator UI | React | 15 | 8 | 7 |
| **Celkem** | | | **83** | **52** | **31 (37 %)** |

---

## Phase 5 – DevOps Maturity (M10–11)
**Cíl:** Production-ready observability, CI/CD, onboarding prvního holdingu.

| Oblast | Popis | MD | MD (AI) | Úspora |
|---|---|---|---|---|
| Observability Stack | OpenTelemetry + Prometheus + Grafana + Loki | 20 | 15 | 5 |
| CI/CD Pipelines | GitHub Actions / Azure DevOps; Native Image pipeline | 15 | 11 | 4 |
| Local Dev | Tilt / Skaffold konfigurace | 8 | 6 | 2 |
| First Holding Onboarding | AAD tenant, KeyVault, první perioda, Schema Mapping tuning | 15 | 15 | 0 |
| **Celkem** | | **58** | **47** | **11 (19 %)** |

> **Poznámka Phase 5:** DevOps konfigurace a onboarding mají nejnižší AI úsporu. Observability a CI/CD YAML lze z části generovat, ale výsledné pipeline a Grafana dashboardy vyžadují ladění na konkrétní infrastruktuře. Onboarding je čistě lidská práce.

---

## Phase 6 – Local Scope & Advanced Analytics (M12+)
**Cíl:** Platforma pro interní reporting dceřiných společností; pokročilé srovnání period.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| engine-reporting:form *(rozšíření – FS21)* | Local Forms | Java 21 + Spring Boot | 25 | 17 | 8 |
| engine-reporting:pptx-template *(rozšíření – FS21)* | Local PPTX Templates | Java 21 + Spring Boot | 15 | 10 | 5 |
| engine-core:admin *(rozšíření – FS21)* | CompanyAdmin Role | Java 21 + Spring Boot | 10 | 6 | 4 |
| engine-reporting:period *(rozšíření – FS22)* | Advanced Period Comparison | Java 21 + Spring Boot | TBD | TBD | — |
| frontend *(rozšíření)* | Frontend – Local Scope UI | React | TBD | TBD | — |
| **Celkem (bez TBD)** | | | **50** | **33** | **17 (34 %)** |

---

## Souhrnný přehled

| Fáze | Měsíce | MD | MD (AI) | Úspora | Úspora % | Klíčový výstup |
|---|---|---|---|---|---|---|
| Phase 1 – MVP Core | M1–2 | 212 | 137 | 75 | 35 % | Upload + PPTX extrakce + viewer |
| Phase 2 – Extended Parsing | M3–4 | 100 | 61 | 39 | 39 % | Všechny formáty + BI dashboardy |
| Phase 3 – Intelligence & Admin | M5–6 | 145 | 93 | 52 | 36 % | AI, Schema Mapping, holdingová hierarchie |
| Phase 3b – Reporting Lifecycle | M6–7 | 85 | 54 | 31 | 36 % | Stavový automat, periody, deadliny |
| Phase 3c – Form Builder | M7–8 | 113 | 71 | 42 | 37 % | Centrální sběr dat, Excel export/import |
| Phase 4 – Enterprise Features | M8–9 | 76 | 45 | 31 | 41 % | Audit, versioning, notifikace |
| Phase 4b – PPTX Generator | M9–10 | 83 | 52 | 31 | 37 % | Automatické generování reportů |
| Phase 5 – DevOps & Onboarding | M10–11 | 58 | 47 | 11 | 19 % | Production-ready, první holding live |
| Phase 6 – Local Scope & Advanced | M12+ | 50+ | 33+ | 17+ | 34 % | Lokální reporting, pokročilé srovnání |
| **CELKEM** | | **922+** | **593+** | **329+** | **~36 %** | |

### Interpretace výsledků

Celková úspora **~36 %** (329 MD z 922 MD) je konzervativní odhad. Reálná úspora závisí na:

- **Kvalitě specifikace:** Čím detailnější OpenAPI kontrakt a datový model, tím více AI Agent vygeneruje bez iterací.
- **Code review overhead:** AI-generovaný kód vyžaduje review – zhruba 15–20 % z ušetřeného času se vrátí jako review effort. Výsledná čistá úspora je pak spíše **28–30 %**.
- **Iterační smyčky:** Pro komplexní service (engine-reporting:form, engine-reporting:lifecycle) AI Agent výrazně urychluje boilerplate, ale business logiku vývojář stejně ladí ručně – proto konzervativní 30–32 % u těchto services.
- **Nejnižší přínos:** Phase 5 (DevOps, 19 %) a N8N workflows (22 %) – konfigurační a orchestrační práce, kde AI pomáhá méně než u klasického kódu.
- **Nejvyšší přínos:** React Frontend (47 %) a standardní Java CRUD services (45 %) – AI zde generuje funkční komponenty a endpointy s minimálními korekcemi.

> **Upozornění na duplicitu v původním plánu:** engine-data:dashboard byl uveden v Phase 2 i Phase 4 (oba s 40 MD). V aktualizovaném plánu je engine-data:dashboard pouze v Phase 2.

---

*Implementation Plan v2.1 | PPTX Analyzer & Automation Platform | Únor 2026*
