# Implementation Plan
**Verze:** 2.1 – doplněn odhad MD s AI Agentem  
**Celkový rozsah:** 9 fází, 30 microservices

> **Poznámka k MD:** MS-FE MD zahrnuje všechny obrazovky napříč fázemi, ale je zaúčtován do Phase 1 jako celek. Nové obrazovky (formuláře, lifecycle UI, PPTX generátor) jsou rozepsány v příslušných fázích jako přírůstkový effort.

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
| Config-only | MS-GW, MS-SCAN | 20 % |
| Standard Java CRUD | MS-SINK-*, MS-QRY, MS-NOTIF, MS-VER, MS-SRCH, MS-PERIOD, MS-BATCH | 45 % |
| Komplexní Java service | MS-AUTH, MS-ING, MS-ADMIN, MS-TMPL, MS-LIFECYCLE, MS-FORM, MS-AUDIT, MS-TMPL-PPTX | 32 % |
| Python Atomizer / FastAPI | MS-ATM-*, MS-ATM-AI, MS-MCP, MS-GEN-PPTX | 40 % |
| N8N workflows | MS-N8N a jeho rozšíření | 22 % |
| React Frontend | MS-FE a jeho rozšíření | 47 % |
| Dashboard / agregace | MS-DASH | 35 % |
| DevOps & Infra | Observability, CI/CD, Local Dev | 27 % |
| Onboarding / konzultace | First Holding setup | 0 % |

---

## Phase 1 – MVP Core (M1–2)
**Cíl:** End-to-end průchod jednoho PPTX souboru – upload, parsování, uložení, základní viewer.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-GW | API Gateway | Traefik (config) | 5 | 4 | 1 |
| MS-AUTH | Auth Service | Java 21 + Spring Boot | 30 | 20 | 10 |
| MS-ING | File Ingestor | Java 21 + Spring Boot | 25 | 17 | 8 |
| MS-SCAN | Security Scanner | ClamAV (sidecar) | 5 | 4 | 1 |
| MS-ATM-PPTX | PPTX Atomizer | Python + FastAPI | 45 | 27 | 18 |
| MS-SINK-TBL | Table API (Sink) | Java 21 + Spring Boot | 12 | 7 | 5 |
| MS-SINK-LOG | Log API (Sink) | Java 21 + Spring Boot | 5 | 3 | 2 |
| MS-N8N | N8N Orchestrator | N8N (JSON workflows) | 40 | 31 | 9 |
| MS-FE | Frontend SPA *(MD zahrnuje všechny fáze)* | React 18 + Vite + TS + Tailwind | 45 | 24 | 21 |
| **Celkem** | | | **212** | **137** | **75 (35 %)** |

---

## Phase 2 – Extended Parsing (M3–4)
**Cíl:** Plná podpora formátů, RLS, Schema Mapping základ, BI dashboardy.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-ATM-XLS | Excel Atomizer | Python + FastAPI | 20 | 12 | 8 |
| MS-ATM-PDF | PDF/OCR Atomizer | Python + FastAPI | 15 | 9 | 6 |
| MS-ATM-CSV | CSV Atomizer | Python + FastAPI | 5 | 3 | 2 |
| MS-ATM-CLN | Cleanup Worker | Python (CronJob) | 5 | 3 | 2 |
| MS-QRY | Query API (Read) | Java 21 + Spring Boot | 15 | 8 | 7 |
| MS-DASH | Dashboard Aggregation | Java 21 + Spring Boot | 40 | 26 | 14 |
| **Celkem** | | | **100** | **61** | **39 (39 %)** |

---

## Phase 3 – Intelligence & Admin (M5–6)
**Cíl:** Holdingová hierarchie, AI integrace, Schema Mapping s learningem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-ADMIN | Admin Backend | Java 21 + Spring Boot | 25 | 17 | 8 |
| MS-BATCH | Batch & Org Service | Java 21 + Spring Boot | 20 | 11 | 9 |
| MS-TMPL | Template & Schema Registry | Java 21 + Spring Boot | 35 | 24 | 11 |
| MS-ATM-AI | AI Gateway | Python + FastAPI | 35 | 21 | 14 |
| MS-MCP | MCP Server (AI Agent) | Python + FastAPI | 30 | 20 | 10 |
| **Celkem** | | | **145** | **93** | **52 (36 %)** |

> **Poznámka MS-TMPL:** Learning algoritmus Schema Mappingu (predikce mapování z historie) je náročný na logiku – AI Agent generuje strukturu, ale algoritmus musí být navržen člověkem. Proto nižší redukce (30 %) oproti standardnímu CRUD.

---

## Phase 3b – Reporting Lifecycle (M6–7)
**Cíl:** Nahradit e-mail/Excel workflow řízeným reportingovým cyklem se stavovým automatem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-LIFECYCLE | Report Lifecycle Service | Java 21 + Spring Boot | 30 | 20 | 10 |
| MS-PERIOD | Reporting Period Manager | Java 21 + Spring Boot | 20 | 11 | 9 |
| MS-N8N *(rozšíření)* | N8N – Lifecycle Workflows | N8N (JSON workflows) | 15 | 12 | 3 |
| MS-FE *(rozšíření)* | Frontend – Lifecycle UI | React | 20 | 11 | 9 |
| **Celkem** | | | **85** | **54** | **31 (36 %)** |

> **Poznámka MS-LIFECYCLE:** Stavový automat je přímočarý na implementaci, ale validační podmínky přechodů (RBAC + business rules) a edge cases vyžadují důkladný lidský review. AI generuje strukturu i testy, lidský vývojář ověřuje korektnost přechodových pravidel.

---

## Phase 3c – Form Builder (M7–8)
**Cíl:** Centrální sběr OPEX dat přes formuláře – konec rozesílání Excel šablon e-mailem.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-FORM | Form Builder & Data Collection | Java 21 + Spring Boot | 55 | 38 | 17 |
| MS-FORM *(Excel export/import)* | Excel Template Export/Import | Java 21 + Spring Boot | 20 | 13 | 7 |
| MS-SINK-TBL *(rozšíření)* | Table API – form_responses | Java 21 + Spring Boot | 8 | 4 | 4 |
| MS-FE *(rozšíření)* | Frontend – Form UI | React | 30 | 16 | 14 |
| **Celkem** | | | **113** | **71** | **42 (37 %)** |

> **Poznámka MS-FORM:** Form engine (dynamické typy polí, závislostní validace, verzování) je nejkomplexnější nová service. AI Agent generuje scaffolding a CRUD, ale validační engine a verzovací logiku musí vývojář navrhnout a reviewovat ručně – odtud konzervativnějších 30 %.

---

## Phase 4 – Enterprise Features (M8–9)
**Cíl:** Compliance, verzování, notifikace, audit – production readiness.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-NOTIF | Notification Center | Java 21 + Spring Boot | 20 | 11 | 9 |
| MS-VER | Versioning Service | Java 21 + Spring Boot | 16 | 9 | 7 |
| MS-AUDIT | Audit & Compliance | Java 21 + Spring Boot | 25 | 17 | 8 |
| MS-SRCH | Search Service | Java 21 + Spring Boot | 15 | 8 | 7 |
| **Celkem** | | | **76** | **45** | **31 (41 %)** |

> **Poznámka MS-AUDIT:** Immutable log a compliance export jsou technicky přímočaré (append-only tabulka, export endpoint) – proto vyšší redukce i přes bezpečnostní citlivost. Security review zůstává na člověku.

---

## Phase 4b – PPTX Report Generation (M9–10)
**Cíl:** Uzavřít cyklus – ze schválených dat automaticky vygenerovat standardizovaný PPTX report.

| Unit ID | Název | Tech Stack | MD | MD (AI) | Úspora |
|---|---|---|---|---|---|
| MS-TMPL-PPTX | PPTX Template Manager | Java 21 + Spring Boot | 25 | 17 | 8 |
| MS-GEN-PPTX | PPTX Generator | Python + FastAPI (python-pptx, matplotlib) | 35 | 21 | 14 |
| MS-N8N *(rozšíření)* | N8N – Generation Workflow | N8N (JSON workflows) | 8 | 6 | 2 |
| MS-FE *(rozšíření)* | Frontend – Generator UI | React | 15 | 8 | 7 |
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
| MS-FORM *(rozšíření – FS21)* | Local Forms | Java 21 + Spring Boot | 25 | 17 | 8 |
| MS-TMPL-PPTX *(rozšíření – FS21)* | Local PPTX Templates | Java 21 + Spring Boot | 15 | 10 | 5 |
| MS-ADMIN *(rozšíření – FS21)* | CompanyAdmin Role | Java 21 + Spring Boot | 10 | 6 | 4 |
| MS-PERIOD *(rozšíření – FS22)* | Advanced Period Comparison | Java 21 + Spring Boot | TBD | TBD | — |
| MS-FE *(rozšíření)* | Frontend – Local Scope UI | React | TBD | TBD | — |
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
- **Iterační smyčky:** Pro komplexní service (MS-FORM, MS-LIFECYCLE) AI Agent výrazně urychluje boilerplate, ale business logiku vývojář stejně ladí ručně – proto konzervativní 30–32 % u těchto services.
- **Nejnižší přínos:** Phase 5 (DevOps, 19 %) a N8N workflows (22 %) – konfigurační a orchestrační práce, kde AI pomáhá méně než u klasického kódu.
- **Nejvyšší přínos:** React Frontend (47 %) a standardní Java CRUD services (45 %) – AI zde generuje funkční komponenty a endpointy s minimálními korekcemi.

> **Upozornění na duplicitu v původním plánu:** MS-DASH byl uveden v Phase 2 i Phase 4 (oba s 40 MD). V aktualizovaném plánu je MS-DASH pouze v Phase 2.

---

*Implementation Plan v2.1 | PPTX Analyzer & Automation Platform | Únor 2026*
