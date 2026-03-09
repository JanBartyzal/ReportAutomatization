# Project Charter: PPTX Analyzer & Automation Platform

**Verze:** 4.0 – Release Candidate (APPROVED)  
**Datum:** Únor 2026  
**Architektura:** Event-Driven Microservices + N8N Orchestration  
**Microservices:** 25 service units (MS-FE … MS-BATCH)  
**Feature Sets:** FS01–FS16 + FS99 (DevOps)  
**Docs Reference:** `docs/project_standards.md`, `docs/dod_criteria.md`

---

## 1. Executive Summary

Projekt si klade za cíl vybudovat end-to-end platformu pro automatizovaný příjem, bezpečné zpracování a strukturovanou analýzu nestrukturovaných podnikových souborů – primárně PPTX prezentací, Excel sešitů a PDF dokumentů.

Platforma řeší klíčový problém finančního controllingu v holdingových strukturách: manuální, časově náročnou konsolidaci OPEX reportů z dceřiných společností, které dodávají data v různých formátech, s různou strukturou sloupců a různou úrovní kvality dat.

Výsledkem projektu bude SaaS-ready platforma nasaditelná na Azure (Azure Container Apps), která:

- Automaticky extrahuje finanční data z nahraných souborů bez manuálního přepisu.
- Normalizuje a sjednocuje data z různých zdrojů pomocí konfigurovatelného Schema Mapping Registry.
- Uchovává plnou auditní stopu s verzováním dat (kdo, kdy, co změnil).
- Zpřístupňuje data prostřednictvím BI dashboardů a AI agentů (MCP Server) pro interaktivní analýzu.
- Garantuje datovou izolaci mezi organizacemi v holdingové struktuře pomocí Row-Level Security.

| Obchodní cíl | Technický cíl | Kvalitativní cíl |
|---|---|---|
| Zkrátit čas konsolidace OPEX reportů o 80 % | Zpracovat 1 soubor < 30 s (end-to-end) | Audit-ready: každá akce logována a neměnná |
| Eliminovat chyby způsobené manuálním přepisem dat | 99,5 % dostupnost (SLA pro produkci) | ISO 27001 kompatibilní přístupová kontrola |
| Zajistit datovou suverenitu pro každou organizaci v holdingu | Horizontální škálování Atomizer vrstvy | Zero-trust: každý API call ověřen tokenem |

---

## 2. High-Level Architektura

Systém je rozdělen do šesti logických vrstev, přičemž každá vrstva má jasně definovanou odpovědnost a komunikuje s ostatními výhradně přes definované kontrakty (REST API / Dapr PubSub).

| # | Vrstva | Odpovědnost | Klíčové služby |
|---|---|---|---|
| 1 | **Presentation Layer** | React SPA – upload souborů, viewer parsovaných dat, BI dashboardy, real-time notifikace přes WebSocket/SSE, MSAL autentizace. | MS-FE |
| 2 | **Edge Layer** | API Gateway (Traefik) jako jediný vstupní bod. SSL terminace, rate limiting, ForwardAuth pro validaci Azure Entra ID tokenů. | MS-GW, MS-AUTH |
| 3 | **Ingestion Layer** | Streaming příjem souborů, antivirová kontrola (ClamAV/ICAP), sanitizace (odstranění maker), uložení do Blob Storage, trigger N8N workflow. | MS-ING, MS-SCAN |
| 4 | **Orchestration Layer** | N8N workflow engine řídí celý processing pipeline: routing dle typu souboru, batch zpracování, retry/circuit breaker, Dead Letter Queue. | MS-N8N, MS-DLQ |
| 5 | **Processing Layer (Atomizers)** | Bezstavové kontejnery pro extrakci dat z konkrétního formátu. Volány výhradně z N8N. Výsledky ukládají jako URL reference na Blob, ne jako inline payload. | MS-ATM-PPTX, -XLS, -PDF, -CSV, -AI, -CLN |
| 6 | **Persistence Layer (Sinks)** | Write-optimalizované API pro strukturovaná data (PostgreSQL), vector embeddings (pgVector), audit logy. CQRS: oddělený read model (MS-QRY, MS-DASH). | MS-SINK-TBL/DOC/LOG, MS-QRY, MS-DASH |

### 2.1 Komunikační pravidla

- Veškerá inter-service komunikace probíhá přes Dapr sidecars (gRPC interně, REST externě).
- Frontend komunikuje výhradně s API Gateway – nikdy přímo s backend službami.
- Atomizery jsou volány **VÝHRADNĚ** přes N8N Orchestrátor – nikdy přímo z frontendové vrstvy.
- Binary data (PNG slidy, CSV soubory) se **NIKDY** nepřenáší jako inline payload – vždy jako URL reference na Blob Storage.
- Každý request musí nést validní Azure Entra ID JWT token v2 se scope `api://<client_id>/access_as_user`.

### 2.2 Technologický stack

| Komponenta | Technologie | Verze (min) | Odůvodnění výběru |
|---|---|---|---|
| **Backend Core** | Java 21 + Spring Boot 3 | 21 LTS / 3.x | Dlouhodobá podpora, enterprise ecosystem, GraalVM pro scale-to-zero |
| **Backend AI/Data** | Python + FastAPI + Pydantic v2 | 3.11+ / latest | Nativní AI knihovny (python-pptx, openpyxl, LiteLLM, Tesseract) |
| **Frontend** | React 18 + Vite + TypeScript + Tailwind CSS | 18 / 5.x / 5.x | Moderní SPA s type safety, optimalizovaný build |
| **Orchestrátor** | N8N (self-hosted) | latest stable | Low-code workflow editor, webhooks, JSON flow as code |
| **Service Mesh** | Dapr Sidecars | 1.x | Abstrakce komunikace, PubSub, state management |
| **Auth** | Azure Entra ID (AAD) | v2.0 endpoint | Enterprise SSO, MSAL, On-Behalf-Of flow pro AI |
| **DB – Relační** | PostgreSQL 16 | 16 | JSONB, pgVector, Row-Level Security nativně |
| **DB – Cache** | Redis | latest stable | Session, rate limit counters, query cache |
| **DB – Vector** | pgVector extension | latest | Sémantické vyhledávání bez dalšího závislosti |
| **Security Scan** | ClamAV / ICAP | latest | Open-source AV, ICAP protokol pro streamový scan |
| **Observability** | OpenTelemetry + Prometheus + Grafana + Loki | latest | End-to-end tracing, metriky, centralizované logy |
| **Local Dev** | Tilt / Skaffold | latest | Hot-reload pro K8s/Docker lokální topologii |
| **E2E Testy** | Playwright | latest | React + API + N8N flow testování |
| **DB migrace** | Flyway / Liquibase | latest | Správa verzí DB schématu |
| **Schema Registry** | Apicurio / JSON Schema Store | latest | Centralizace schémat pro N8N a služby |

> **NOTE:** Pokud není určeno jinak, primárním programovacím jazykem je **Java (Spring Boot)**. Python se používá výhradně pro Atomizery a AI komponenty.


### Struktura projektu

|
|-> data   #lokální data pro Docker
|
|-> docs #dokumentace
   |-> api
   |-> demo
   |-> documents
   |-> tasks
   |-> architecture
    dod_criteria.md
    project_charter.md
    roadmap.md
    standards.md
|
|-> frontend #frontend
|
|-> infra
   |-> cert
   |-> datr
   |-> docker
      - docker-compose.yml
      - .env
      - Dockerfile
   |-> scripts
   |-> n8n
   |-> terraform
|
|-> local_data
|
|-> microservices
   |-> units
   |-> docs
   |-> logs
|
|-> packages #balíčky
   |-> charts
   |-> java-base
   |-> protos
   |-> python-base
   |-> types
   |-> ui
|
|-> tests
|
|-> tilt
|
| README.md #readme|
| .gitignore #gitignore





---

## 3. Port Allocation (Lokální Dev)

| Služba | Host Port | Container Port | Debug Port | Protokol |
|---|---|---|---|---|
| Frontend (Vite SPA) | `3000` | `3000` | — | HTTP |
| API Gateway (Traefik) | `8080` | `80` | — | HTTP/HTTPS |
| Auth Service | `8081` | `8000` | `5005` | HTTP |
| File Ingestor | `8082` | `8000` | `5006` | HTTP / multipart |
| PPTX Atomizer | `8090` | `8000` | `5678` | HTTP |
| Excel Atomizer | `8091` | `8000` | `5679` | HTTP |
| Sink: Table API | `8100` | `8080` | `5005` | HTTP |
| N8N Webhook Listener | `5678` | `5678` | — | HTTP/WS |
| PostgreSQL | `5432` | `5432` | — | TCP |
| Redis | `6379` | `6379` | — | TCP |

---

## 4. Non-Functional Requirements (NFR)

| Kategorie | Požadavek | Měřítko / KPI | Priorita |
|---|---|---|---|
| Výkon | End-to-end zpracování souboru | < 30 s pro PPTX do 50 slidů | MUST |
| Výkon | Upload API latence | < 2 s pro soubory do 20 MB | MUST |
| Dostupnost | Produkční SLA | 99,5 % monthly uptime | MUST |
| Škálovatelnost | Atomizer vrstva | Horizontální scale-out, min. 10 paralelních jobů | SHOULD |
| Bezpečnost | Autentizace | Zero-trust: každý request ověřen JWT tokenem | MUST |
| Bezpečnost | Datová izolace | PostgreSQL RLS: cross-tenant data leak = 0 | MUST |
| Bezpečnost | Antivirový scan | 100 % nahraných souborů skenováno před zpracováním | MUST |
| Audit | Auditní trail | Každá akce uložena, neměnná, exportovatelná | MUST |
| Observability | Distribuované trasování | OpenTelemetry trace přes celý pipeline (FE→GW→N8N→ATM→Sink) | SHOULD |
| Kód | Linting / formátování | ESLint, Black, Checkstyle – CI blokující | MUST |
| Kód | Unit test coverage | Nová logika: min. happy path + 2 edge cases | MUST |

---

## 5. Definition of Done (DoD)

Každá Feature (FS) je považována za dokončenou teprve tehdy, když jsou splněna **VŠECHNA** následující kritéria:

| # | Oblast | Kritérium | Nástroj |
|---|---|---|---|
| 1 | Code Quality | Linting bez chyb – ESLint (TS), Black (Python), Checkstyle (Java) | CI Pipeline |
| 2 | Testy – Unit | Unit testy pokrývají happy path, edge cases a chybové stavy nové logiky | JUnit 5 / Pytest |
| 3 | Testy – Integrace | Klíčové integrační toky otestovány s mockovanými externími službami | Testcontainers / WireMock |
| 4 | Bezpečnost | Žádné hardcoded secrets; auth tokeny validovány na každém endpointu | Git secret scan |
| 5 | Dokumentace | README.md aktualizováno; Mermaid diagram odpovídá implementaci; API zdokumentováno v OpenAPI 3.0 | Swagger UI |
| 6 | CI/CD Pipeline | Build + Test + Docker image build prošly bez chyb | GitHub Actions / Azure DevOps |
| 7 | DB Migrace | Flyway / Liquibase migrace připravena a otestována (up + rollback) | Flyway |
| 8 | Observability | Service emituje OpenTelemetry traces a Prometheus metriky | Grafana / OTEL Collector |

---

## 6. Feature Sets – Detailní popis

### FS01 – Infrastructure & Core
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-GW, MS-AUTH

Business kontext: Základní infrastruktura, bez které žádná jiná FS nemůže fungovat. Zabezpečuje síťování, auth, secrets management a service discovery.

**Požadavky:**
- Kontejnerizovaná infrastruktura: N8N, Traefik, Redis, Grafana, Prometheus spustitelná přes Docker Compose i Kubernetes (Tilt/Skaffold pro lokální dev).
- Minimální base image pro Java (JDK 21, bez zbytečných závislostí) a Python (3.11 slim) – základ všech Atomizerů.
- **API Gateway (Traefik):** Routing `/api/auth` → MS-AUTH, `/api/upload` → MS-ING, `/api/query` → MS-QRY. SSL terminace, rate limiting (429 Too Many Requests), ForwardAuth middleware.
- **Service Discovery:** Dapr sidecar pattern pro vzájemnou inter-service komunikaci. Každá služba dostává Dapr sidecar s definovanými komponentami (state store, pub/sub).
- **Centralized Auth:** Validace Azure Entra ID tokenů (v2.0 endpoint) na úrovni API Gateway / ForwardAuth. RBAC role: Admin, Editor, Viewer.
- **Azure KeyVault:** Všechny secrets (DB connection strings, API keys, SMTP credentials) uloženy v KeyVault. Aplikace je čte při startu přes MSI (Managed Service Identity).
- Přístup do aplikace podmíněn členstvím v konkrétní AAD Security Group (Conditional Access).

**Acceptance kritéria:**
- Nová Spring Boot / FastAPI služba spuštěna do 30 s s kompletními base image.
- ForwardAuth vrátí `401` pro request bez tokenu, `403` pro token s nedostatečnými oprávněními.
- KeyVault secret dostupný v aplikaci přes environment variable při startu.
- Lokální topologie startuje příkazem `tilt up` do 5 minut.

---

### FS02 – File Ingestor (Input)
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-ING, MS-SCAN

Business kontext: První kontaktní bod pro nahrávání souborů. Musí být rychlý, bezpečný a odolný. Upload probíhá jako streaming – nikdy neblokuje frontend.

**Požadavky:**
- **Endpoint:** `POST /api/upload` s `multipart/form-data` nebo chunked streaming.Excel upload slouží jako datový vstup do formuláře (FS19), nejen jako soubor k parsování. Ingestor musí rozlišit `upload_purpose: PARSE` (původní flow) vs. `upload_purpose: FORM_IMPORT` (nový flow → MS-FORM).
- **Stream Upload:** Soubor streamován přímo do Azure Blob Storage nebo S3 – nikdy celý v paměti serveru.
- **Validace:** Kontrola MIME types (allowlist: `.pptx`, `.xlsx`, `.pdf`, `.csv`) a magic numbers (binární hlavička souboru).
- **Security Scan:** Integrovaný ClamAV sidecar (ICAP protokol) – soubor skenován **PŘED** uložením do Blobu. Infikované soubory vrací `422` s reason kódem.
- **Sanitizace:** Automatické odstranění VBA maker a externích odkazů z Office dokumentů (MS-SCAN) před předáním Atomizerům.
- **Metadata:** Zápis záznamu do PostgreSQL (`UserId`, `OrgId`, `Filename`, `Size`, `MimeType`, `UploadTimestamp`, `BlobUrl`, `ScanStatus`).
- **N8N Trigger:** Po úspěšném uložení fire-and-forget webhook na N8N: `{ file_id, type, org_id, blob_url }`.

**Acceptance kritéria:**
- Upload 20 MB PPTX souboru dokončen za < 5 s na 100 Mbps připojení.
- Soubor s EICAR test virem vrátí `422` s `{ error: "INFECTED", details: "..." }`.
- Soubor s nepovolený MIME type (`.exe`) vrátí `415 Unsupported Media Type`.
- N8N webhook doručen do 1 s od úspěšného uložení do Blobu.

---

### FS03 – Atomizers – Stateless Extractors
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-ATM-PPTX, MS-ATM-XLS, MS-ATM-PDF, MS-ATM-CSV, MS-ATM-AI, MS-ATM-CLN

Business kontext: Srdce platformy. Každý Atomizer je izolovaný, bezstavový kontejner specializovaný na jeden formát. Komunikují výhradně přes N8N a pracují asynchronně.

#### PPTX Atomizer (MS-ATM-PPTX)
- `POST /extract/pptx` – Vrací JSON strukturu: `{ slides: [{ slide_id, title, layout, has_tables, has_text }] }`
- `POST /extract/pptx/slide` – Extrahuje texty, tabulky a metadata konkrétního slidu. Výstup: `{ texts, tables: [{ headers, rows }], notes }`.
- `POST /extract/pptx/slide/image` – Renderuje slide jako PNG 800×600. PNG ukládá do Blob Storage, vrací `{ artifact_url }`.
- **MetaTable Logic:** Algoritmus pro rekonstrukci tabulek z nestrukturovaného textu na základě vizuálních oddělovačů (tabulátor, mezery) a porovnání s hlavičkovým řádkem.

#### Excel Atomizer (MS-ATM-XLS)
- `POST /extract/excel` – Vrací seznam listů: `{ sheets: [{ sheet_id, name, row_count, col_count }] }`.
- `POST /extract/excel/sheet` – Konverze listu na JSON. Výstup: `{ headers, rows: [{}], data_types }`.
- **Partial Success State:** Pokud 1 z 10 listů selže, vrátí `{ status: "PARTIAL", successful: [...], failed: [{ sheet_id, error }] }`.

#### PDF / OCR Atomizer (MS-ATM-PDF)
- `POST /extract/pdf` – Detekce, zda je PDF textové nebo skenované. Text extrahován přímo; skenované stránky přes Tesseract OCR.

#### CSV Atomizer (MS-ATM-CSV)
- `POST /extract/csv` – Automatická detekce oddělovače (`,;|\t`), kódování, hlavičkového řádku. Výstup: `{ headers, rows }`.

#### AI Gateway (MS-ATM-AI)
- `POST /analyze/semantic` – LiteLLM integrace pro sémantickou analýzu textu (klasifikace, sumarizace, extrakce entit).
- **Cost Control:** Každý request loguje počet spotřebovaných tokenů. Překročení kvóty vrací `429` s `{ quota_remaining: 0 }`.

#### Cleanup Worker (MS-ATM-CLN)
- CronJob: Každou hodinu smaže dočasné soubory (PNG slidy, CSV exporty) z Blob Storage starší než 24 hodin.
- Přidává se obrácený směr: MS-GEN-PPTX (generování). Cleanup Worker (MS-ATM-CLN) musí zahrnout i dočasné soubory generátoru. 

**Společné acceptance kritéria:**
- Každý Atomizer vrátí `200` + strukturovaný JSON nebo `{ artifact_url }` – nikdy inline binary data.
- Chybný soubor vrátí `422` s detailem chyby, nikdy `500`.
- Atomizer si soubor stahuje z Blob Storage sám (přes URL z requestu) – GW ho nepřeposílá.

---

### FS04 – Orchestrator – N8N JSON Workflows
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-N8N

Business kontext: N8N je mozek celého systému. Řídí tok dat, rozhoduje o routování, spravuje chyby a garantuje idempotenci. Veškerá business logika je v N8N workflows (JSON), nikoli roztroušena po mikroslužbách.

**Požadavky:**
- **Pipeline Workflow:** `Webhook (new_file)` → `Get Metadata` → `Router dle file type` → `Call Atomizer` → `Apply Schema Mapping (MS-TMPL)` → `Store (MS-SINK-*)`
- **Batch Processing:** `Split In Batches` node pro iteraci přes slidy (PPTX) nebo listy (Excel). Max. 5 paralelních volání Atomizeru.
- **Filter Logic:** Po extrakci každého elementu: je-li tabulka → MS-SINK-TBL (PostgreSQL); je-li text → MS-SINK-DOC (pgVector).
- **Error Handling:** Automatický retry (3×, exponential backoff). Circuit Breaker: po 5 selháních Atomizeru pozastaven workflow.
- **Idempotence:** Každý workflow krok je idempotentní. Duplicate processing `file_id` je detekován a přeskočen.
- **Dead Letter Queue:** Fatálně selhaný workflow odešle `{ file_id, error_stacktrace, timestamp }` do tabulky `failed_jobs`. Admin UI zobrazuje tyto záznamy pro manuální reprocessing.

**Acceptance kritéria:**
- Upload nového PPTX souboru spustí workflow automaticky bez manuálního zásahu.
- Při selhání Atomizeru (HTTP 500) jsou data uložena do `failed_jobs`, ne ztracena.
- Opětovné spuštění workflow pro stejné `file_id` nevytvoří duplicitní záznamy v DB.

---

### FS05 – Sinks – Storage APIs & Persistence
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG

Business kontext: Write-optimalizovaná API vrstva nad databázemi. Garantuje schéma validaci před zápisem, verzování a RLS enforcement.

**Table API – MS-SINK-TBL (Java/Spring):**
- `POST /tables/{org_id}/{batch_id}` – Bulk insert strukturovaných dat (tabulky, OPEX data) do PostgreSQL (JSONB).
- Flyway migrace pro správu schématu. RLS policy: uživatel vidí pouze záznamy svého `org_id`.
- ukldá data z formulářů (FS19). Schéma: `form_responses` tabulka s `(org_id, period_id, form_version_id, field_id, value, submitted_at)`.

**Document API – MS-SINK-DOC (Java/Spring):**
- `POST /documents/{org_id}` – Ukládání nestrukturovaného JSONu do PostgreSQL + generování vector embeddings (pgVector).
- Embeddings generovány asynchronně přes MS-ATM-AI po uložení dokumentu.

**Log API – MS-SINK-LOG (Java/Spring):**
- `POST /logs/{file_id}` – Append-only zápis processing logů (`step_name`, `status`, `duration_ms`, `error_detail`).
- Záznamy jsou read-only přístupné z MS-QRY a Admin UI.

**Databáze:**
- **PostgreSQL 16:** Primární store. JSONB pro semi-strukturovaná data, RLS pro tenant isolation, pgVector extension.
- **Redis:** Cache pro MS-QRY (TTL 5 min), rate limit counters, session tokens.
- **Blob Storage:** Fyzické soubory a dočasné artefakty (PNG slidy).

---

### FS06 – Analytics & Query – Read Model (CQRS)
**Priorita: VYSOKÁ**

**Pokrývající microservices:** MS-QRY, MS-DASH, MS-SRCH

Business kontext: Oddělený read model optimalizovaný pro rychlé čtení. Frontend nikdy nečte přímo z write databáze.

- **MS-QRY:** CQRS read API. Materialized views pro nejčastější dotazy. Redis caching s TTL.
- **MS-DASH:** Agregační endpointy pro grafy a souhrny. SQL over JSONB tabulky s GROUP BY / ORDER BY z UI konfigurace.MS-DASH musí zobrazovat i data pocházející z formulářů, nejen z parsovaných souborů. Zdroj dat je transparentní (flag `source_type: FORM / FILE`)
- **MS-SRCH:** Full-text search (PostgreSQL FTS nebo ElasticSearch). Vector search přes pgVector pro sémantické dotazy.

---

### FS07 – Admin Backend & UI
**Priorita: VYSOKÁ**

**Pokrývající microservices:** MS-AUTH, MS-ADMIN

- **Role management:** Admin, Editor, Viewer. Hierarchické oprávnění v holdingové struktuře.
- **Secrets management:** Superadmin UI pro update secrets → přímý zápis do Azure KeyVault.
- **API Key management:** Generování klíčů pro service accounts. Klíče hashované (bcrypt), nikdy uloženy v plaintextu.
- **Failed Jobs UI:** Tabulka `failed_jobs` s detailem chyby a tlačítkem "Reprocess" pro manuální opakování.
-  **Správa formulářů a šablon**  - další admin sekce. Role "Reviewer" jako nová sub-role HoldingAdmina pro schvalování reportů (FS17)

---

### FS08 – Organizace souborů & Batch Management
**Priorita: VYSOKÁ**

**Pokrývající microservices:** MS-ADMIN, MS-BATCH

- Každý soubor nese metadata o organizaci v holdingové struktuře (`holding_id`, `company_id`, `uploaded_by`).
- **Batch grouping:** HoldingAdmin vytvoří batch (např. `Q2/2025`). Uživatelé z dceřiných společností nahrávají soubory s tímto batch tagem. Holding vidí konsolidovaný report.
- **Row-Level Security (RLS):** Implementována přímo na PostgreSQL úrovni. Každý SQL dotaz automaticky filtrován dle `org_id` z JWT tokenu. Cross-tenant leak je architektonicky nemožný.
- **"Batch" se mapuje přímo na "Reporting Period"** (FS20). Koncepty se slučují – `period_id` nahrazuje generický `batch_id` tam, kde jde o OPEX reporting.

---

### FS09 – Frontend SPA (React)
**Priorita: KRITICKÁ**

**Pokrývající microservices:** MS-FE

- **Auth:** MSAL Provider, login/logout flow, automatický token refresh (Axios interceptor s retry).
- **Upload Manager:** Drag & Drop zóna (react-dropzone), progress bar přes XHR upload events, automatický refresh seznamu po uploadu (React Query invalidation).
- **Viewer:** Read-only zobrazení parsovaných dat slide-by-slide s preview obrázků (PNG z Blob).
- **Real-time feedback:** WebSocket nebo SSE připojení přes API Gateway/BFF pro zobrazení stavu zpracování v reálném čase (`Processing 50%...`).
- **N8N trigger:** Spouštění N8N workflow z frontendových akcí. Uživatel nikdy nevidí N8N UI.
- **Form Builder UI**
- **formulář pro vyplnění**
- **submission workflow UI**
- **period dashboard**
- **PPTX generator trigger**


**Knihovny:**
- State Management: TanStack Query (server state), Zustand (client state)
- UI Framework: Tailwind CSS + Radix UI / Shadcn
- HTTP Client: Axios s nastavenými interceptory

---

### FS10 – Excel Parsing Logic
**Priorita: VYSOKÁ**

**Pokrývající microservices:** MS-ATM-XLS

- Excel listy parsovány per-sheet do JSONB. Každý list je samostatný záznam v DB.
- **Partial Success:** Soubor s 10 listy, kde 1 selže – 9 úspěšných uloženo, 1 označen `FAILED` s chybovým detailem. Soubor jako celek: `PARTIAL`.
- **Datová kompatibilita:** JSONB záznamy z Excelu a PPTX jsou nerozlišitelné na úrovni databáze – umožňuje jednotné dotazování.

---

### FS11 – Dashboards & SQL Reporting
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-FE, MS-DASH

- Dashboard vytváří Admin/Editor. Viewer vidí pouze Public dashboardy (příznak `is_public`).
- Data source: SQL dotazy nad JSONB tabulkami (PostgreSQL JSON functions). JSONB sloupce přístupné jako virtuální SQL tabulky.
- UI konfigurace: GROUP BY, ORDER BY, filtr datum/org definovatelný v UI bez znalosti SQL. Pro pokročilé: přímý SQL editor.

---

### FS12 – API Rozhraní & AI Integration (MCP)
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-ATM-AI, MS-MCP

- API přístup přes API klíč (Bearer token v `Authorization` headeru).
- **MCP Server (MS-MCP):** Integrace AI agentů. Server dědí OAuth token uživatele přes On-Behalf-Of flow – AI nikdy nemá globální přístup.
- **Security constraint:** MCP Server vynucuje RLS – každý AI dotaz je scoped na `org_id` uživatele.
- **Cost Control:** Měsíční token quota na úrovni uživatele i firmy. Překročení → `429`. Spotřeba viditelná v Admin UI.

---

### FS13 – Notification Center & Alerts
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-NOTIF

- **In-app notifikace:** Push přes WebSocket/SSE při dokončení/chybě zpracování.
- **E-mail notifikace:** SendGrid/SMTP pro kritické chyby a dokončení batch jobů.
- **Granulární nastavení:** Opt-in/Opt-out pro každý typ události (import, parsing fail, report ready) na úrovni uživatele i organizace.
- **notifikační triggery:** z FS17 (stavové přechody) a FS20 (deadliny, eskalace). 
- **Typy notifikací rozšířeny o:** `REPORT_SUBMITTED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `DEADLINE_APPROACHING`, `DEADLINE_MISSED`.

---

### FS14 – Data Versioning & Diff Tool
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-VER

> **Killer feature pro finanční controlling:** žádná editace nepřepisuje originál.

- **Versioning:** Každá změna dat vytváří novou verzi (v1 → v2). Originál vždy zachován.
- **Diff Tool:** UI zobrazuje rozdíl mezi verzemi (změna hodnoty, přidané/odebrané řádky). Pro OPEX data: `+500k v IT nákladech v2 vs v1`.

---

### FS15 – Template & Schema Mapping Registry
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-TMPL

> Řeší klíčový problém heterogenity dat: různé firmy pojmenovávají sloupce odlišně (`Cost`, `Náklady`, `Cena`).

- **Editor UI:** Definice mapovacích šablon. *"Pokud sloupec obsahuje Cena / Cost / Náklady → namapuj jako `amount_czk`."*
- **Learning:** Systém si pamatuje předchozí mapování a navrhuje je automaticky pro nové soubory.
- **N8N integrace:** MS-TMPL je voláno z N8N workflow **PŘED** zápisem do DB.
- **Excel import do formuláře** - nopé volání, už nejen z N8N pipeline. MS-TMPL dostane nový endpoint `POST /map/excel-to-form`.

---

### FS16 – Audit & Compliance Log
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** MS-AUDIT

- **Immutable logy:** Záznamy "kdo-kdy-co viděl/editoval". Append-only tabulka (INSERT only, bez UPDATE/DELETE oprávnění pro app user).
- **Read Access Log:** Každé zobrazení citlivého reportu logováno (`User ID`, `Document ID`, `IP`, `Timestamp`).
- **AI Audit:** Každý prompt a odpověď AI (FS12) logován pro zpětnou kontrolu halucinací a úniku dat.
- **Export:** CSV/JSON export logů pro bezpečnostní audit holdingu.
- **Auditovány i stavové přechody** (stavové přechody z FS17) a veškeré akce ve formuláři (pole změněno, komentář přidán, import potvrzen).


## FS17 – OPEX Report Lifecycle & Submission Workflow
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** MS-LIFECYCLE, MS-N8N

**Tech Stack:** Java 21 + Spring Boot (MS-LIFECYCLE)

### Business kontext

Každá dceřiná společnost musí do stanoveného termínu dodat OPEX data pro holding. Dnes tento proces probíhá přes e-mail, sdílené složky nebo SharePoint – bez transparentního přehledu o tom, kdo dodal, v jakém stavu jsou data a zda jsou připravena k převzetí do centrálního reportingu.

Platforma zavede **stavový automat pro každý report** s jasnými přechody, odpovědnostmi a auditní stopou.

### Stavy reportu (State Machine)

```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                  ↘              ↘ REJECTED → DRAFT (resubmit)
```

| Stav | Kdo nastavuje | Popis |
|---|---|---|
| `DRAFT` | Systém (při vytvoření) | Report byl vytvořen, data se teprve plní |
| `SUBMITTED` | Editor (dceřiná společnost) | Editor potvrdil, že data jsou kompletní a předává je k revizi |
| `UNDER_REVIEW` | HoldingAdmin / Reviewer | Holding přijal report k revizi |
| `APPROVED` | HoldingAdmin / Reviewer | Report schválen, data přijata do centrálního reportingu |
| `REJECTED` | HoldingAdmin / Reviewer | Report vrácen s komentářem k opravě; přechází zpět do `DRAFT` |

### Požadavky

- **Report entita:** Každý report je vázán na `(org_id, period_id, report_type)`. Jedno období = jeden report na organizaci.
- **Stavové přechody:** Každý přechod je logován v audit logu (kdo, kdy, z jakého stavu, do jakého, volitelný komentář).
- **Komentáře k rejection:** Při zamítnutí musí Reviewer povinně vyplnit důvod. Komentář je viditelný Editorovi v jeho UI.
- **Submission checklist:** Editor vidí checklist podmínek před potvrzením odesláním (`SUBMITTED`): jsou všechna povinná pole vyplněna, jsou všechny listy nahrané, odpovídají data validačním pravidlům?
- **Přehled pro HoldingAdmin:** Dashboard s matricí `[Společnost × Perioda]` a stavem každého reportu. Okamžitý přehled, kdo ještě nedodal.
- **Hromadné akce:** HoldingAdmin může schválit nebo zamítnout více reportů najednou.
- **Uzamčení dat po schválení:** Po přechodu do `APPROVED` jsou zdrojová data read-only. Jakákoli změna vyžaduje nový přechod do `DRAFT` (vytvoří novou verzi – viz FS14).

#### Architektura a Workflow customizace (N8N vs MS-LIFECYCLE)
- **MS-LIFECYCLE:** Spravuje stavový automat entity `Report`, vystavuje endpointy pro přechody stavů, validuje oprávnění, loguje do MS-AUDIT a publikuje event `report.status_changed` do Dapr PubSub.
- **N8N Orchestrátor:** Odebírá event `report.status_changed` a orchestruje následné kroky (např. notifikace, automatické kontroly dat, triggerování generování PPTX). Různé `report_type` mohou mít různý N8N workflow.

### Acceptance kritéria

- Editor nemůže odeslat report (`SUBMITTED`), dokud checklist nehlásí 100 % kompletnost.
- Přechod `APPROVED` automaticky triggeruje zahrnutí dat do centrálního reportingu.
- Přechod `REJECTED` automaticky odesílá notifikaci Editorovi (FS13) s komentářem.
- Audit log záznamu přechodu stavu obsahuje: `user_id`, `from_state`, `to_state`, `timestamp`, `comment`.
- Historie všech stavových přechodů jednoho reportu je zobrazitelná v UI (timeline view).

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-LIFECYCLE** | Report Lifecycle Service | Správa stavového automatu reportů, submission checklist, rejection flow, hromadné akce | Java 21 + Spring Boot | **L** |

---

## FS18 – PPTX Report Generation (Template Engine)
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** MS-TMPL-PPTX, MS-GEN-PPTX

**Tech Stack:** Python + FastAPI (MS-GEN-PPTX) + Java (MS-TMPL-PPTX)

### Business kontext

Dnes každá společnost generuje PPTX report sama – různé šablony, různé formáty grafů, různé pojmenování sekcí. HoldingAdmin pak manuálně sjednocuje vizuální podobu před prezentací vedení.

Nová funkce umožní **generovat standardizovaný PPTX report automaticky** ze strukturovaných zdrojových dat uložených v platformě, na základě centrálně spravované šablony.

Toto je **"obrácený Atomizer"** – místo extrakce dat z PPTX do DB jde o renderování dat z DB do PPTX.

### Požadavky

#### Správa PPTX šablon (MS-TMPL-PPTX)
- HoldingAdmin nahraje PPTX soubor jako šablonu (`POST /templates/pptx`). Šablony v tomto FS mají scope `CENTRAL` (vlastník = HoldingAdmin).
- Šablona obsahuje **placeholder tagy** ve formátu `{{variable_name}}` v textových polích, `{{TABLE:table_name}}` pro tabulky, `{{CHART:metric_name}}` pro grafy.
- Systém šablonu naparsuje a extrahuje seznam všech placeholderů → zobrazí v UI jako "požadované datové vstupy".
- Šablony jsou verzovány (v1, v2). Přiřazení šablony k `period_id` nebo `report_type`.
- Náhled šablony v UI bez dat (placeholder hodnoty zobrazeny jako ukázka).

#### Generování reportu (MS-GEN-PPTX)
- `POST /generate/pptx` s `{ template_id, report_id }` → systém načte schválená zdrojová data z DB a vyrenderuje PPTX.
- Generátor nahradí textové placeholdery hodnotami, vyplní tabulky, vygeneruje grafy (python-pptx + matplotlib/plotly pro grafy).
- Výsledný PPTX uložen do Blob Storage, URL uložena k `report_id`.
- Generování probíhá asynchronně – výsledek doručen přes notifikaci (FS13) a WebSocket/SSE (FS09).
- **Batch generování:** HoldingAdmin může spustit generování PPTX pro všechny schválené reporty v periodě najednou.

#### Mapování dat na šablonu
- UI pro přiřazení: "Placeholder `{{it_costs}}` → pole `amount_czk` z formuláře / sloupec z Excel uploadu".
- Toto mapování je součástí konfigurace šablony, ne per-report.
- Pokud zdrojová data neobsahují hodnotu pro placeholder → report je vygenerován s výrazným vizuálním upozorněním (červený rámeček, text `DATA MISSING`), nikoli selháním.

### Acceptance kritéria

- Generování PPTX reportu ze 20 slidů dokončeno za < 60 s.
- Výsledný PPTX je validní soubor otevíratelný v MS PowerPoint a LibreOffice.
- Chybějící data nezpůsobí selhání generování – slide s chybějícím polem je označen `DATA MISSING`.
- Batch generování 10 reportů najednou dokončeno za < 15 minut.
- Vygenerovaný soubor ke stažení přímo z UI reportu.

### Nové microservices

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-TMPL-PPTX** | PPTX Template Manager | Nahrávání, verzování a správa PPTX šablon; extrakce placeholderů; mapování na datové zdroje | Java 21 + Spring Boot | **L** |
| **MS-GEN-PPTX** | PPTX Generator | Renderování PPTX ze zdrojových dat + šablony; placeholder substituce; grafy; batch generování | Python + FastAPI (python-pptx, matplotlib) | **L** |

---

## FS19 – Dynamic Form Builder & Data Collection
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** MS-FORM, MS-FE

**Tech Stack:** Java 21 + Spring Boot (MS-FORM) + React (součást MS-FE)

### Business kontext

Dnešní proces sběru dat funguje tak, že HoldingAdmin pošle e-mailem Excel šablonu, každá společnost ji vyplní a pošle zpět. Výsledkem jsou desítky různě modifikovaných Excelů, které musí analytik rukou konsolidovat.

Nová funkce nahradí tento proces **centrálním formulářem v platformě**. HoldingAdmin definuje, jaká data chce sbírat. Uživatelé dceřiných společností data vyplní přímo v platformě. Data jsou okamžitě strukturovaně uložena – bez parsování, bez deduplikace, bez manuální práce.

Platforma zároveň nadále podporuje **nahrávání Excel souborů jako datový vstup** pro případy, kdy jsou data rozsáhlá nebo vznikají v externím systému.

### Požadavky

#### Form Builder (pro HoldingAdmin / Editor)
- UI editor pro tvorbu formulářů metodou drag & drop.
- Typy polí: `text`, `number` (s volitelnou měnou/jednotkou), `percentage`, `date`, `dropdown` (výběr z předdefinovaných hodnot), `table` (uživatel vyplňuje tabulku s pevnými sloupci), `file_attachment` (příloha).
- Povinnost pole (`required: true/false`).
- Validační pravidla na úrovni pole: min/max hodnota, regex pattern, závislost na jiném poli (`if field_A > 0 then field_B is required`).
- Sekce a popisné texty pro strukturování formuláře.
- Náhled formuláře před publikováním.
- **Verzování formuláře:** Změna publikovaného formuláře vytvoří novou verzi. Existující vyplněná data jsou vázána na verzi, ve které byla vyplněna.

#### Správa formulářů
- Formulář je přiřazen k `period_id` a `report_type`.
- Přiřazení formuláře ke konkrétním společnostem (ne vždy všechny vyplňují stejný formulář – holdingová struktura).
- Stav formuláře: `DRAFT` (jen admin vidí), `PUBLISHED` (viditelný přiřazeným uživatelům), `CLOSED` (nelze vyplňovat, deadline).
- **Deadline:** Formulář lze uzavřít ručně nebo automaticky k datu (napojení na FS20).

#### Vyplňování formuláře (pro Editor / dceřiná společnost)
- Editor vidí seznam formulářů k vyplnění v aktuálním období.
- Průběžné ukládání (`auto-save` každých 30 s nebo při přechodu mezi sekcemi).
- Validace v reálném čase – chybová pole označena před odesláním.
- Možnost uložit jako `DRAFT` a vrátit se later.
- Po odeslání (`SUBMITTED`) přechod do submission workflow (FS17).
- **Komentáře na úrovni pole:** Editor může přidat vysvětlující komentář k jakékoli hodnotě (např. "Toto číslo zahrnuje jednorázový odpis z Q1.").

#### Export Excel šablony z formuláře
- Každý publikovaný formulář lze exportovat jako strukturovaný Excel soubor (`GET /forms/{form_id}/export/excel-template`).
- Vygenerovaný Excel má list per sekce formuláře. Sloupce odpovídají polím formuláře včetně jejich validačních pravidel.
- Soubor obsahuje skrytý metadata list (`__form_meta`) s `form_id` a `form_version_id` – slouží pro párování při importu zpět.
- Uživatel Excel stáhne, vyplní offline a nahraje zpět.

#### Import vyplněného Excelu zpět do formuláře
- `POST /forms/{form_id}/import/excel` – systém zkontroluje metadata list, ověří `form_version_id`.
- Pokud verze souhlasí: data jsou přímo namapována bez nutnosti potvrzovat mapování.
- Pokud verze nesouhlasí: systém upozorní a nabídne best-effort mapování přes FS15.
- Po importu jsou data zobrazena v UI formuláře k vizuální kontrole před odesláním.

#### Import libovolného Excelu (bez šablony)
- Vedle formuláře může Editor nahrát vlastní Excel soubor jako datový vstup.
- Systém Excel naparsuje (MS-ATM-XLS) a nabídne **mapování sloupců → pole formuláře** (napojení na FS15 Schema Mapping).
- Editor zkontroluje a potvrdí mapování → data jsou importována do formuláře.
- Po importu jsou data editovatelná jako kdyby byla zadána ručně.
- Původní Excel soubor je uložen jako příloha reportu (auditní stopa).

#### Granularita formulářů
- V rámci FS19 se implementují pouze formuláře se scope `CENTRAL` (vlastník = HoldingAdmin, viditelnost = všechny přiřazené společnosti). Datový model MS-FORM však musí `scope` a `owner_org_id` zohledňovat od začátku (příprava na FS21).

### Acceptance kritéria

- HoldingAdmin vytvoří a publikuje nový formulář do 10 minut bez technických znalostí.
- Auto-save funguje; po ztrátě připojení a znovuotevření jsou data zachována.
- Validace formuláře vrátí seznam všech chybných polí najednou, nikoli po jednom.
- Import z Excelu: mapování sloupců navrženo automaticky na základě FS15; Editor potvrdí za < 2 minuty.
- Vyplněná data jsou okamžitě dostupná v centrálním reportingu bez dalšího zpracování.
- Formulář verze v1 a v2 jsou uložena odděleně; historická data nejsou přepsána upgradem formuláře.

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-FORM** | Form Builder & Data Collection | Definice formulářů, správa verzí, sběr dat, validace, Excel import, napojení na MS-LIFECYCLE | Java 21 + Spring Boot | **XL** |

---

## FS20 – Reporting Period & Deadline Management
**Priorita: VYSOKÁ**  

**Pokrývající microservices:** MS-PERIOD

**Tech Stack:** Java 21 + Spring Boot (MS-PERIOD)

### Business kontext

OPEX reporting probíhá v opakujících se cyklech (měsíčně, kvartálně, ročně). Každý cyklus má deadline, v rámci kterého musí všechny společnosti dodat data. Dnes tyto termíny existují jen v e-mailech a kalendářích – systém o nich neví a nemůže automaticky upozorňovat, uzavírat formuláře nebo generovat eskalace.

### Požadavky

#### Správa period
- HoldingAdmin vytvoří reportovací periodu: `{ name, type: MONTHLY|QUARTERLY|ANNUAL, start_date, submission_deadline, review_deadline, period_code }`.
- Perioda je přiřazena k holdingu a viditelná všem dceřiným společnostem.
- Stav periody: `OPEN` → `COLLECTING` → `REVIEWING` → `CLOSED`.
- Perioda lze klonovat z předchozí (přenese přiřazení formulářů a šablon).

#### Deadline management
- **Submission deadline:** Datum, do kdy musí společnosti odeslat data (`SUBMITTED`). Po deadlinu formuláře automaticky přejdou do `CLOSED` (nelze již vyplňovat). Opozdilé submisse vyžadují explicitní override od HoldingAdmina.
- **Review deadline:** Datum, do kdy musí Holding schválit/zamítnout přijaté reporty.
- **Automatické upozornění:** X dní před deadlinem systém odešle notifikaci (FS13) všem, kdo ještě nemají `SUBMITTED`. Konfigurovatelný počet dní (default: 7, 3, 1 den před).
- **Eskalace:** Pokud společnost nepodá v termínu, HoldingAdmin dostane upozornění s přehledem neplničů.

#### Completion tracking
- Dashboard periody: matice `[Společnost × Stav]` s barvovým rozlišením (šedá = DRAFT, žlutá = SUBMITTED, zelená = APPROVED, červená = REJECTED/overdue).
- Procento dokončenosti periody (počet APPROVED / celkový počet povinných reportů).
- Export statusu periody jako PDF nebo Excel pro vedení.

#### Historická data a srovnání period (Základní srovnání as-is)
- Každá uzavřená perioda je archivována a přístupná pro srovnávací analýzu.
- Dashboard umožní srovnání stejné metriky napříč periodami (např. Q1/2024 vs. Q1/2025). Srovnání probíhá na úrovni: stejná metrika, stejná organizace, dvě různé periody stejného typu.
- Vizualizace: sloupcový nebo spojnicový graf, tabulka s delta hodnotami (absolutní i procentuální změna).
- Napojení na MS-VER (FS14): opravy v rámci periody vytvářejí verze, nikoli přepisují historii.

### Acceptance kritéria

- Vytvoření nové periody klonem z předchozí trvá < 2 minuty.
- Automatické uzavření formulářů po submission deadlinu bez manuálního zásahu.
- Notifikace odeslána 7/3/1 den před deadlinem všem uživatelům s `DRAFT` nebo nevyplněným formulářem.
- Dashboard periody se načte se stavem všech společností za < 3 s.
- Export statusu periody funkční pro 50+ společností.

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-PERIOD** | Reporting Period Manager | Správa period a deadlinů, automatické uzavírání, completion tracking, eskalace, historické srovnání | Java 21 + Spring Boot | **M** |



## FS21 – Local Forms & Local PPTX Templates (Lokální scope)
**Priorita: STŘEDNÍ**  
**Fáze:** P5 (vlastní fáze po stabilizaci centrálního reportingu)

**Pokrývající microservices:** MS-FORM, MS-TMPL-PPTX, MS-ADMIN, MS-LIFECYCLE

### Business kontext
Po nasazení centrálního reportingového cyklu vznikne přirozená poptávka ze strany dceřiných společností: "Chceme platformu využít i pro naše interní reporty a dashboardy, ne jen pro holding." FS21 tento požadavek řeší a zároveň vytváří zárodek pro budoucí horizontální expanzi platformy na standalone produkt pro jednotlivé společnosti.

### Požadavky

#### Lokální formuláře
- CompanyAdmin (nová sub-role Editora s rozšířenými právy v rámci vlastní organizace) může vytvářet formuláře se `scope: LOCAL`.
- Lokální formuláře jsou viditelné a vyplnitelné pouze uživateli v rámci dané `org_id`.
- Data z lokálních formulářů jsou primárně pro interní use – neproudí automaticky do centrálního reportingu.
- **"Uvolnění" dat:** CompanyAdmin může označit konkrétní lokální formulář nebo jeho data jako `RELEASED`. HoldingAdmin obdrží notifikaci a může data zahrnout do centrálního reportingu (manuální pull, nikoli automatický push).
- Lokální formuláře sledují stejný lifecycle jako centrální (DRAFT / PUBLISHED / CLOSED), ale bez holdingového approval workflow.

#### Lokální PPTX šablony
- CompanyAdmin může nahrát vlastní PPTX šablonu (scope: `LOCAL`).
- Generátor (MS-GEN-PPTX) je schopný generovat PPTX z lokální šablony pro potřeby interního reportu.
- Vygenerovaný lokální report není automaticky sdílen s holdingem.

#### Sdílení lokálních šablon
- CompanyAdmin může sdílet lokální šablonu nebo formulář s jiným CompanyAdminem v rámci stejného holdingu (`scope: SHARED_WITHIN_HOLDING`).
- HoldingAdmin má přehled o všech lokálních a sdílených šablonách/formulářích v holdingu.

## FS22 – Advanced Period Comparison (Granularita srovnání)
**Priorita: NÍZKÁ**  

**Pokrývající microservices:** MS-DASH, MS-PERIOD

**Fáze:** P6 (po stabilizaci FS20)  
**Status: PLACEHOLDER – implementace later**

### Scope (zatím jen definice, ne implementace)

- Konfigurovatelné KPI pro srovnání: uživatel si definuje vlastní srovnávací metriky a kombinace dimenzí.
- Srovnání across types: Q1 vs. celý rok (normalizace na denní/měsíční bázi).
- Multi-org srovnání: holding vidí stejnou metriku pro všechny dceřiné společnosti vedle sebe.
- Drill-down: srovnání na úrovni cost center nebo divize (vyžaduje granularitu dat z FS19 lokálních formulářů).
- Export srovnávacích reportů jako PPTX (napojení na FS18 generátor).

*Tento FS bude detailně specifikován až po nasazení FS20 a prvních zkušenostech z provozu.*
---

### FS99 – DevOps & Observability
**Priorita: VYSOKÁ**

**Pokrývající microservices:** CI/CD, Observability stack

- **CI/CD:** Pipeline pro Linting → Unit Testy → Integration Testy → Docker Build → Push to Registry. Oddělená pipeline pro GraalVM Native Image build (release).
- **OpenTelemetry tracing:** E2E trace přes celý stack: Frontend → API Gateway → N8N → Atomizer → Sink. Jaeger/Tempo jako trace backend.
- **Centralizované logy:** Loki nebo ELK stack. Structured JSON logging ze všech služeb.
- **Metriky:** Prometheus scrape z každé služby. Grafana dashboardy: chybovost, délka N8N fronty, Atomizer latence, DB connection pool.
- **Local Dev:** `tilt up` spustí kompletní topologii v lokálním K8s (Kind) nebo Docker Compose s hot-reloadem pro React a Python služby.

---

## 7. Katalog Microservices

| # | Unit ID | Název | Popis / Odpovědnost | Feature Set | Tech Stack | Effort |
|---|---|---|---|---|---|---|
| 1 | **MS-FE** | Frontend SPA | React SPA – upload, viewer, dashboardy, notifikace (WebSocket/SSE), MSAL auth | FS09, FS11 | React 18 + Vite + TS + Tailwind | **XL** |
| 2 | **MS-GW** | API Gateway | Traefik – routing, SSL, rate limiting, ForwardAuth | FS01 | Traefik (config) | **S** |
| 3 | **MS-AUTH** | Auth Service | Validace Entra ID tokenů, RBAC engine, KeyVault integrace, API key validace | FS01, FS07 | Java 21 + Spring Boot | **L** |
| 4 | **MS-ING** | File Ingestor | Streaming upload, MIME validace, metadata zápis, sanitizace, trigger N8N | FS02 | Java 21 + Spring Boot | **L** |
| 5 | **MS-SCAN** | Security Scanner | Antivirová kontrola přes ICAP/ClamAV sidecar | FS02 | ClamAV (sidecar) | **S** |
| 6 | **MS-N8N** | N8N Orchestrator | Business workflow engine – routing, batch, retry, circuit breaker, DLQ | FS04 | N8N (JSON workflows) | **L** |
| 7 | **MS-ATM-PPTX** | PPTX Atomizer | Extrakce struktury, textů, tabulek a slide images z PPTX | FS03 | Python + FastAPI | **L** |
| 8 | **MS-ATM-XLS** | Excel Atomizer | Parsování Excel per-sheet do JSON, partial success handling | FS03, FS10 | Python + FastAPI | **M** |
| 9 | **MS-ATM-PDF** | PDF/OCR Atomizer | OCR a extrakce textu ze skenovaných PDF | FS03 | Python + FastAPI | **M** |
| 10 | **MS-ATM-CSV** | CSV Atomizer | Konverze CSV na strukturovaný JSON | FS03 | Python + FastAPI | **S** |
| 11 | **MS-ATM-AI** | AI Gateway | LiteLLM integrace, sémantická analýza, MetaTable logic, cost control | FS03, FS12 | Python + FastAPI | **L** |
| 12 | **MS-ATM-CLN** | Cleanup Worker | Cron pro mazání dočasných souborů z Blob po expiraci | FS03 | Python (CronJob) | **S** |
| 13 | **MS-SINK-TBL** | Table API (Sink) | Ukládání strukturovaných dat (tabulky, OPEX) do PostgreSQL | FS05 | Java 21 + Spring Boot | **M** |
| 14 | **MS-SINK-DOC** | Document API (Sink) | Ukládání nestrukturovaného JSONu + vector embeddings (pgVector) | FS05 | Java 21 + Spring Boot | **M** |
| 15 | **MS-SINK-LOG** | Log API (Sink) | Audit trail zpracování souborů – append-only processing logy | FS05 | Java 21 + Spring Boot | **S** |
| 16 | **MS-QRY** | Query API (Read) | CQRS read model, Redis caching, optimalizované čtení pro FE | FS06 | Java 21 + Spring Boot | **M** |
| 17 | **MS-DASH** | Dashboard Aggregation | Grafy, souhrny, GROUP BY/SORT, SQL nad JSON tabulkami | FS06, FS11 | Java 21 + Spring Boot | **L** |
| 18 | **MS-SRCH** | Search Service | Full-text search (PostgreSQL FTS / ES) + vector search | FS06 | Java 21 + Spring Boot | **M** |
| 19 | **MS-ADMIN** | Admin Backend | Správa rolí, holdingová hierarchie, secrets, API keys, Failed Jobs UI | FS07, FS08 | Java 21 + Spring Boot | **L** |
| 20 | **MS-NOTIF** | Notification Center | In-app (WS/SSE), e-mail (SMTP/SendGrid), granulární nastavení | FS13 | Java 21 + Spring Boot | **M** |
| 21 | **MS-TMPL** | Template Registry | UI pro mapování sloupců, learning z historie, voláno z N8N | FS15 | Java 21 + Spring Boot | **L** |
| 22 | **MS-VER** | Versioning Service | Verzování dat (v1→v2), diff tool pro zobrazení změn | FS14 | Java 21 + Spring Boot | **M** |
| 23 | **MS-AUDIT** | Audit & Compliance | Immutable logy, read access log, AI audit, export | FS16 | Java 21 + Spring Boot | **M** |
| 24 | **MS-MCP** | MCP Server (AI Agent) | AI agenti, On-Behalf-Of flow, token dědění, quotas | FS12 | Python + FastAPI | **L** |
| 25 | **MS-BATCH** | Batch & Org Service | Seskupování do batchů, holdingová metadata, RLS enforcement | FS08 | Java 21 + Spring Boot | **M** |
| 26 | **MS-LIFECYCLE** | Report Lifecycle Service | FS17 | Java 21 + Spring Boot | **L** |
| 27 | **MS-TMPL-PPTX** | PPTX Template Manager | FS18 | Java 21 + Spring Boot | **L** |
| 28 | **MS-GEN-PPTX** | PPTX Generator | FS18 | Python + FastAPI | **L** |
| 29 | **MS-FORM** | Form Builder & Data Collection | FS19 | Java 21 + Spring Boot | **XL** |
| 30 | **MS-PERIOD** | Reporting Period Manager | FS20 | Java 21 + Spring Boot | **M** |

### Effort legenda

| Effort | Story Points | Popis |
|---|---|---|
| **S** | 3–5 | Jednoduchá služba, konfigurace nebo thin wrapper |
| **M** | 8–13 | Středně komplexní s vlastní business logikou |
| **L** | 13–21 | Komplexní, více endpointů, integrace, edge cases |
| **XL** | 21–34 | Rozsáhlá komponenta s mnoha obrazovkami/moduly |

---

## 8. Doporučený Rollout – Fáze

| Fáze | Název | Microservices | Feature Sets | Výstup / Milestone |
|---|---|---|---|---|
| **P1** | MVP Core | MS-GW, MS-AUTH, MS-ING, MS-SCAN, MS-N8N, MS-ATM-PPTX, MS-SINK-TBL, MS-SINK-LOG, MS-FE (základní) | FS01, FS02, FS03-PPTX, FS04, FS05, FS09-basic | Funkční upload + extrakce PPTX + základní viewer |
| **P2** | Extended Parsing | MS-ATM-XLS, MS-ATM-PDF, MS-ATM-CSV, MS-ATM-CLN, MS-QRY, MS-DASH | FS03-rest, FS10, FS06 | Plná podpora formátů + BI dashboardy |
| **P3a** | Intelligence & Admin | MS-ADMIN, MS-BATCH, MS-ATM-AI, MS-MCP, MS-TMPL | FS07, FS08, FS12, FS15 | Holdingová hierarchie + AI integrace + schema mapping |
| **P3b** | Lifecycle + Period Mgmt  |MS-LIFECYCLE, MS-PERIOD | | Řízení OPEX cyklu, deadliny, stavový automat |
| **P3c** | Form Builder – centrální (MS-FORM, Excel export/import) | | | Sběr dat bez Excelu po e-mailu |
| **P4a** | Enterprise Features | MS-NOTIF, MS-VER, MS-AUDIT, MS-SRCH, MS-DASH (extended) | FS11, FS13, FS14, FS16 | Plná enterprise výbava: versioning, diff tool, compliance |
| **P4b** | PPTX Generator (MS-TMPL-PPTX, MS-GEN-PPTX) | | |  Automatické generování standardizovaných reportů |
| **P5** | DevOps Maturity | Observability stack (Prometheus, Grafana, Loki, OTEL), CI/CD pipelines | FS99 | Production-ready: monitoring, tracing, automated 
| **P6** | Advanced Period Mgmt (deadliny, eskalace, as-is srovnání) | | | Plná správa reportingových cyklů |pipelines |
| **P7** | FS22 Advanced Comparison (placeholder) | | |  Granulární srovnání period |





---

## 9. Risk Register

| # | Riziko | Pravd. | Dopad | Mitigace | Owner |
|---|---|---|---|---|---|
| R1 | N8N Payload Size: přenášení velkých JSONů (base64 obrázky) způsobí OOM crash | M | KRITICKÝ | Atomizery NIKDY neposílají binary inline. Vždy ukládají do Blob a vracejí URL. Max. payload limit: 5 MB na N8N webhook. | MS-ATM-*, MS-N8N |
| R2 | Auth Token Propagation: desynchronizace tokenů mezi FE → GW → N8N → Backend | M | VYSOKÝ | Strict Token Propagation policy. Dapr middleware automaticky propaguje `Authorization` header. E2E testy pokrývají celý auth flow. | MS-GW, MS-AUTH |
| R3 | Scope Creep: postupné přidávání features způsobí nerealistické termíny | H | STŘEDNÍ | Striktní fázový rollout (P1–P5). FS kategorizovány jako MUST/SHOULD/COULD. Scope freeze pro každou fázi. | PM / Architekt |
| R4 | Komplexita N8N workflows: JSON workflow soubory se stanou neudržitelnými | M | STŘEDNÍ | Workflows verzovány v Gitu jako JSON. Code review povinný. Standardizované naming konvence pro všechny workflow uzly. | MS-N8N |
| R5 | PostgreSQL RLS konfigurace: chybná policy způsobí cross-tenant data leak | L | KRITICKÝ | Automatické integráční testy ověřují RLS po každé DB migraci. Minimální oprávnění pro app user (no superuser). | MS-SINK-TBL |
| R6 | Vendor Lock-in: Azure specifické závislosti (Blob, Entra ID, KeyVault) | M | STŘEDNÍ | Abstrakční vrstva pro storage (interface → Azure/S3). Auth abstrahován přes OIDC standard. KeyVault nahraditelný HashiCorp Vault. | Architekt |
| R7 | Výkonnostní bottleneck: serializační overhead Dapr sidecar pro high-throughput Atomizery | L | STŘEDNÍ | Load testy v P1. Možnost přímé HTTP komunikace Atomizer↔Sink bez Dapr pro vysoce výkonné paths. | MS-ATM-* |

*Legenda: Pravděpodobnost: H = Vysoká, M = Střední, L = Nízká*

---

## 10. Závislosti & Předpoklady

### 10.1 Interní závislosti

| Závislá služba | Závisí na | Typ závislosti |
|---|---|---|
| MS-ING | MS-AUTH, MS-SCAN, Blob Storage | HARD – bez auth a skenu nelze přijmout soubor |
| MS-N8N | MS-ING (webhook), MS-ATM-*, MS-SINK-* | HARD – orchestrátor bez zdrojů a sinks nefunguje |
| MS-SINK-TBL/DOC | PostgreSQL (RLS, Flyway migrace) | HARD – DB musí být migrována před nasazením |
| MS-TMPL | MS-N8N (volání), PostgreSQL (mapování history) | SOFT – N8N může fungovat bez TMPL (bez normalizace) |
| MS-MCP | MS-AUTH (OBO flow), MS-QRY | HARD – AI nesmí fungovat bez autorizace |

### 10.2 Externí závislosti

- **Azure Entra ID tenant:** Nakonfigurovaná App Registration s RBAC skupinami.
- **Azure KeyVault:** Provisionovaný vault s oprávněními pro MSI aplikace.
- **Azure Blob Storage nebo S3:** Bucket/container s odpovídajícími CORS pravidly.
- **SendGrid nebo SMTP server:** Pro e-mailové notifikace (FS13).
- **LiteLLM / OpenAI API:** Pro AI Gateway (FS03, FS12). Potřeba API klíč a quota.

---

## 11. Kommunikační matice (Dapr)

| Caller | Callee | Protokol | Typ |
|---|---|---|---|
| MS-GW | MS-AUTH | REST (ForwardAuth) | Sync |
| MS-GW | MS-ING | REST | Sync |
| MS-GW | MS-QRY | REST | Sync |
| MS-ING | MS-SCAN | ICAP / gRPC | Sync |
| MS-ING | MS-N8N | REST (Webhook) | Async (fire & forget) |
| MS-N8N | MS-ATM-* | REST | Sync (within workflow) |
| MS-N8N | MS-SINK-* | REST | Sync |
| MS-N8N | MS-TMPL | REST | Sync |
| MS-N8N | MS-NOTIF | REST / Pub-Sub | Async |
| MS-NOTIF | MS-FE | WebSocket / SSE | Push |
| MS-QRY | Redis | TCP | Cache lookup |
| MS-SINK-* | PostgreSQL | TCP | Write |
| MS-AUDIT | PostgreSQL | TCP | Append-only write |

---

## 12. Glossary

| Termín | Definice |
|---|---|
| **Atomizer** | Bezstavový mikroservice specializovaný na extrakci dat z konkrétního formátu (PPTX, Excel, PDF, CSV). Vždy voláni přes N8N Orchestrátor. |
| **Sink** | Write-optimalizovaná API vrstva pro trvalé uložení zpracovaných dat do databáze. Implementuje CQRS write side. |
| **Batch** | Sada souborů ze stejného reportovacího období (např. Q2/2025) seskupená pro hromadné zpracování a konsolidaci. |
| **RLS** | Row-Level Security – PostgreSQL mechanismus zajišťující, že uživatel vidí pouze řádky patřící jeho organizaci, přímo na úrovni DB enginu. |
| **DLQ** | Dead Letter Queue – tabulka `failed_jobs`, kam N8N ukládá informace o fatálně selhaných workflow pro manuální reprocessing. |
| **CQRS** | Command Query Responsibility Segregation – oddělení write modelu (Sinks) od read modelu (MS-QRY, MS-DASH) pro optimalizaci výkonu. |
| **Schema Mapping** | Konfigurovatelné pravidlo pro normalizaci názvů sloupců z různých zdrojových souborů do jednotného interního datového modelu. |
| **Dapr Sidecar** | Kontejner běžící vedle každé mikroslužby, který abstrahuje inter-service komunikaci, state management a PubSub. |
| **OBO flow** | On-Behalf-Of OAuth flow: AI agent (MS-MCP) používá token uživatele pro volání downstream API, nikoli vlastní servisní identitu. |
| **GraalVM Native Image** | Kompilace Java aplikace do nativního binárního souboru se sub-sekundovým startem – vhodné pro scale-to-zero Atomizery. |
| **ForwardAuth** | Traefik middleware přesměrovávající každý příchozí request na MS-AUTH pro validaci tokenu PŘED předáním backend službě. |
| **OPEX report** | Operating Expenditure report – finanční výkaz provozních nákladů, primární datový typ zpracovávaný touto platformou. |

---

*PPTX Analyzer & Automation Platform – Project Charter v4.0 | Únor 2026 | Interní dokument*
