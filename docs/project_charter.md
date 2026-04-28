# Project Charter: PPTX Analyzer & Automation Platform

**Verze:** 5.3 – Live Excel Export & External Sync (FS27)
**Datum:** Duben 2026
**Architektura:** Event-Driven Microservices + Custom Orchestrator (engine-orchestrator)
**Deployment Units:** 10 consolidated services (router, engine-core, engine-ingestor, engine-orchestrator, engine-data, engine-reporting, engine-integrations, processor-atomizers, processor-generators, frontend)
**Feature Sets:** FS01–FS27 + FS99 (DevOps)
**Docs Reference:** `docs/project_standards.md`, `docs/dod_criteria.md`

> **Consolidation Note (P8):** Původních 29+ mikroslužeb bylo konsolidováno do 10 deployment units.
> Každý unit běží jako jeden kontejner a obsahuje moduly (např. `engine-core` = auth + admin + batch + versioning + audit).
> Modul se referencuje ve formátu `deployment-unit:modul` (např. `engine-data:query`).

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
| 1 | **Presentation Layer** | React SPA – upload souborů, viewer parsovaných dat, BI dashboardy, real-time notifikace přes WebSocket/SSE, MSAL autentizace. | frontend |
| 2 | **Edge Layer** | API Gateway (Nginx) jako jediný vstupní bod. Azure Front Door (WAF + SSL terminace), Host-based routing, rate limiting (100 req/s API, 10 req/s Auth/Upload, burst 20), ForwardAuth pro validaci Azure Entra ID tokenů. CORS whitelist: `https://*.company.cz` + `localhost:3000` (dev). | router, engine-core:auth |
| 3 | **Ingestion Layer** | Streaming příjem souborů, antivirová kontrola (ClamAV/ICAP), sanitizace (odstranění maker), uložení do Blob Storage, trigger orchestrátoru přes Dapr PubSub / gRPC. | engine-ingestor, engine-ingestor:scanner |
| 4 | **Orchestration Layer** | Custom Orchestrator (Spring State Machine) řídí celý processing pipeline: routing dle typu souboru, Saga Pattern pro distribuované transakce, exponential backoff retry, Dead Letter Queue. gRPC pro interní volání Atomizerů a Sinků. | engine-orchestrator |
| 5 | **Processing Layer (Atomizers)** | Bezstavové kontejnery pro extrakci dat z konkrétního formátu. Volány výhradně přes engine-orchestrator (Dapr gRPC). **Žádné REST endpointy** – pouze gRPC service definitions. Výsledky ukládají jako URL reference na Blob, ne jako inline payload. | processor-atomizers:pptx, -XLS, -PDF, -CSV, -AI, -CLN |
| 6 | **Persistence Layer (Sinks)** | Write-optimalizované gRPC API pro strukturovaná data (PostgreSQL), vector embeddings (pgVector), audit logy. Voláno výhradně z engine-orchestrator přes Dapr gRPC. **Žádné REST endpointy** – čtení přes CQRS read model (engine-data:query, engine-data:dashboard – ty vystavují REST pro FE). | engine-data:sink-tbl/DOC/LOG, engine-data:query, engine-data:dashboard |

### 2.1 Komunikační pravidla

- **Interní komunikace (service-to-service):** Probíhá **výhradně** přes Dapr sidecars s gRPC protokolem. Žádná interní služba nevystavuje REST rozhraní pro interní volání.
- **Externí komunikace (frontend-facing):** REST API dostupné **pouze** přes API Gateway (router). REST endpointy vystavují pouze edge služby: engine-core:auth (auth_request z Nginx), engine-ingestor (upload endpoint), engine-data:query (čtení pro FE), engine-data:dashboard (agregace pro FE).
- **Interní služby bez REST:** processor-atomizers (Atomizery), engine-data (sink modules) (Sinky), engine-data:template, engine-orchestrator – tyto služby komunikují **výhradně** přes Dapr gRPC a nemají žádné REST endpointy.
- Frontend komunikuje výhradně s API Gateway – nikdy přímo s backend službami.
- Atomizery jsou volány **VÝHRADNĚ** přes engine-orchestrator (Dapr gRPC) – nikdy přímo z frontendové vrstvy.
- Sinky jsou volány **VÝHRADNĚ** přes engine-orchestrator (Dapr gRPC) pro zápis. Čtení probíhá přes engine-data:query (CQRS read model, REST pro FE).
- **Asynchronní eventy:** Dapr Pub/Sub pro události typu `file-uploaded`, `report.status_changed`, `notify`. Fire-and-forget s built-in retry.
- Binary data (PNG slidy, CSV soubory) se **NIKDY** nepřenáší jako inline payload – vždy jako URL reference na Blob Storage.
- Každý request musí nést validní Azure Entra ID JWT token v2 se scope `api://<client_id>/access_as_user`.

**Přehled protokolů:**

| Komunikační cesta | Protokol | Důvod |
|---|---|---|
| frontend → router | REST (HTTPS) | Kompatibilita s prohlížečem |
| router → engine-core:auth | REST (auth_request) | Nginx nepodporuje gRPC auth_request |
| router → engine-ingestor, engine-data:query, engine-data:dashboard | REST | Frontend-facing edge služby |
| engine-ingestor → engine-ingestor:scanner | Dapr gRPC | Interní služba |
| engine-ingestor → engine-orchestrator | Dapr Pub/Sub | Async event trigger |
| engine-orchestrator → processor-atomizers | Dapr gRPC | Interní zpracování |
| engine-orchestrator → engine-data (sink modules) | Dapr gRPC | Interní persistence |
| engine-orchestrator → engine-data:template | Dapr gRPC | Interní mapping |
| engine-orchestrator → engine-reporting:notification | Dapr Pub/Sub | Async notifikace |
| engine-reporting:notification → frontend | WebSocket / SSE | Real-time push |

### 2.2 Technologický stack

| Komponenta | Technologie | Verze (min) | Odůvodnění výběru |
|---|---|---|---|
| **Backend Core** | Java 21 + Spring Boot 3 | 21 LTS / 3.x | Dlouhodobá podpora, enterprise ecosystem, GraalVM pro scale-to-zero |
| **Backend AI/Data** | Python + FastAPI + Pydantic v2 | 3.11+ / latest | Nativní AI knihovny (python-pptx, openpyxl, LiteLLM, Tesseract) |
| **Frontend** | React 18 + Vite + TypeScript + Tailwind CSS | 18 / 5.x / 5.x | Moderní SPA s type safety, optimalizovaný build |
| **Orchestrátor** | Custom Orchestrator (engine-orchestrator) | — | Spring State Machine, Saga Pattern, gRPC, Type-Safe Contracts, JSON workflow definitions |
| **Service Mesh** | Dapr Sidecars | 1.x | Abstrakce komunikace, PubSub, state management |
| **Auth** | Azure Entra ID (AAD) | v2.0 endpoint | Enterprise SSO, MSAL, On-Behalf-Of flow pro AI |
| **DB – Relační** | PostgreSQL 16 | 16 | JSONB, pgVector, Row-Level Security nativně |
| **DB – Cache** | Redis | latest stable | Session, rate limit counters, query cache |
| **DB – Vector** | pgVector extension | latest | Sémantické vyhledávání bez dalšího závislosti |
| **Security Scan** | ClamAV (clamd TCP socket) | latest | Open-source AV, clamd TCP socket (port 3310) pro scan |
| **Observability** | OpenTelemetry + Prometheus + Grafana + Loki | latest | End-to-end tracing, metriky, centralizované logy |
| **Local Dev** | Tilt / Skaffold | latest | Hot-reload pro K8s/Docker lokální topologii |
| **E2E Testy** | Playwright | latest | React + API + Orchestrator flow testování |
| **DB migrace** | Flyway / Liquibase | latest | Správa verzí DB schématu |
| **Schema Registry** | Apicurio / JSON Schema Store | latest | Centralizace schémat pro engine-orchestrator a služby |

> **NOTE:** Pokud není určeno jinak, primárním programovacím jazykem je **Java (Spring Boot)**. Python se používá výhradně pro Atomizery a AI komponenty.


### Struktura projektu
|
├── apps   #aplikace
|   ├── frontend #frontend
|   ├── engine #java microservice
|   |   └── microservices
|   |       ├── units   # services
|   |       ├── docs
|   |       └── logs
|   ├── processor #python microservice
|   |   └── microservices
|   |       ├── units   # services
|   |       ├── docs
|   |       └── logs
|   └── orchestrator #ms-orch
|
├── data   #lokální data pro Docker
|
├── docs #dokumentace
|   ├── api
|   ├── demo
|   ├── documents
|   ├── tasks
|   └── architecture
|    dod_criteria.md
|   project_charter.md
|   roadmap.md
|   standards.md
|
├── infra
|   ├── cert
|   ├── datr
|   └── docker
|   docker-compose.yml
|   .env
|   Dockerfile
|   ├── scripts
|   ├── orchestrator
|   └── terraform
|
├── local_data
|
├── packages #balíčky
|   ├── charts
|   ├── java-base
|   ├── protos
|   ├── python-base
|   ├── types
|   └── ui
|
├── tests
|
├── tilt
|
├── README.md #readme|
| .gitignore #gitignore


---

## 3. Port Allocation (Lokální Dev)

| Služba | Host Port | Container Port | Protokol |
|---|---|---|---|
| frontend (Vite SPA) | `5173` | `5173` | HTTP |
| router (Nginx API Gateway) | `80` / `443` | `80` / `443` | HTTP/HTTPS |
| engine-core (auth + admin + batch + versioning + audit) | `8081` | `8081` | HTTP |
| engine-ingestor (ingestion + scanner) | `8082` | `8082` | HTTP / multipart |
| engine-orchestrator (workflow + saga) | `8083` | `8080` | gRPC / HTTP |
| engine-data (sinks + query + dashboard + search + template) | `8100` | `8100` | HTTP / gRPC |
| engine-reporting (lifecycle + period + form + pptx-template + notification) | `8105` | `8105` | HTTP |
| engine-integrations (servicenow) | `8106` | `8106` | HTTP |
| processor-atomizers (pptx/xls/pdf/csv/ai/cleanup) | `8088` | `8088` | HTTP / gRPC |
| processor-generators (pptx/xls generation + MCP) | `8111` | `8111` | HTTP / gRPC |
| PostgreSQL | `5432` | `5432` | TCP |
| Redis | `6379` | `6379` | TCP |

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
| Observability | Distribuované trasování | OpenTelemetry trace přes celý pipeline (FE→GW→engine-orchestrator→ATM→Sink) | SHOULD |
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

**Pokrývající microservices:** router, engine-core:auth

Business kontext: Základní infrastruktura, bez které žádná jiná FS nemůže fungovat. Zabezpečuje síťování, auth, secrets management a service discovery.

**Požadavky:**
- Kontejnerizovaná infrastruktura: engine-orchestrator, Nginx, Redis, Grafana, Prometheus spustitelná přes Docker Compose i Kubernetes (Tilt/Skaffold pro lokální dev).
- Minimální base image pro Java (JDK 21, bez zbytečných závislostí) a Python (3.11 slim) – základ všech Atomizerů.
- **API Gateway (Nginx):** Host-based routing `/api/auth` → engine-core:auth, `/api/upload` → engine-ingestor, `/api/query` → engine-data:query. SSL terminace přes Azure Front Door (WAF). Rate limiting: 100 req/s per IP (API), 10 req/s per IP (Auth/Upload), burst size 20, `429 Too Many Requests`. ForwardAuth middleware. CORS whitelist: `https://*.company.cz` + `localhost:3000` (dev).
- **Service Discovery:** Dapr sidecar pattern pro vzájemnou inter-service komunikaci. Každá služba dostává Dapr sidecar s definovanými komponentami (state store, pub/sub).
- **Centralized Auth:** Validace Azure Entra ID tokenů (v2.0 endpoint) na úrovni API Gateway / ForwardAuth. RBAC role: Admin (vše v rámci Org), Editor (Upload/Edit), Viewer (Read-only), HoldingAdmin (Cross-org Read + approval).
- **Organizační hierarchie:** Fixní 3 úrovně: Holding → Společnost → Divize/Nákladové středisko.
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

**Pokrývající microservices:** engine-ingestor, engine-ingestor:scanner

Business kontext: První kontaktní bod pro nahrávání souborů. Musí být rychlý, bezpečný a odolný. Upload probíhá jako streaming – nikdy neblokuje frontend.

**Požadavky:**
- **Endpoint:** `POST /api/upload` s `multipart/form-data` nebo chunked streaming.Excel upload slouží jako datový vstup do formuláře (FS19), nejen jako soubor k parsování. Ingestor musí rozlišit `upload_purpose: PARSE` (původní flow) vs. `upload_purpose: FORM_IMPORT` (nový flow → engine-reporting:form).
- **Stream Upload:** Soubor streamován přímo do Azure Blob Storage (lokálně Azurite v Dockeru) – nikdy celý v paměti serveru. Max file size: 50 MB (PPTX/XLSX/CSV), 100 MB (PDF s OCR).
- **Validace:** Kontrola MIME types (allowlist: `.pptx`, `.xlsx`, `.pdf`, `.csv`) a magic numbers (binární hlavička souboru).
- **Security Scan:** ClamAV přes clamd TCP socket (port 3310) – soubor skenován **PŘED** uložením do Blobu. Infikované soubory vrací `422` s reason kódem.
- **Sanitizace:** Automatické odstranění VBA maker a externích odkazů z Office dokumentů (engine-ingestor:scanner) před předáním Atomizerům. Originální soubory (`_raw/`) uchovávat 90 dní (audit), poté smazat. Sanitizované verze trvale.
- **Metadata:** Zápis záznamu do PostgreSQL (`UserId`, `OrgId`, `Filename`, `Size`, `MimeType`, `UploadTimestamp`, `BlobUrl`, `ScanStatus`).
- **Blob naming:** `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}`
- **Orchestrátor Trigger:** Po úspěšném uložení event přes Dapr PubSub (nebo gRPC) na engine-orchestrator: `{ file_id, type, org_id, blob_url }`.

**Acceptance kritéria:**
- Upload 20 MB PPTX souboru dokončen za < 5 s na 100 Mbps připojení.
- Soubor s EICAR test virem vrátí `422` s `{ error: "INFECTED", details: "..." }`.
- Soubor s nepovolený MIME type (`.exe`) vrátí `415 Unsupported Media Type`.
- Orchestrátor event doručen do 1 s od úspěšného uložení do Blobu.

---

### FS03 – Atomizers – Stateless Extractors
**Priorita: KRITICKÁ**

**Pokrývající microservices:** processor-atomizers:pptx, processor-atomizers:xls, processor-atomizers:pdf, processor-atomizers:csv, processor-atomizers:ai, processor-atomizers:cleanup

Business kontext: Srdce platformy. Každý Atomizer je izolovaný, bezstavový kontejner specializovaný na jeden formát. Komunikují výhradně přes engine-orchestrator (gRPC) a pracují asynchronně.

#### PPTX Atomizer (processor-atomizers:pptx)
- `gRPC ExtractStructure(ExtractRequest)` → `PptxStructureResponse { slides: [{ slide_id, title, layout, has_tables, has_text }] }` – Vrací strukturu PPTX souboru.
- `gRPC ExtractSlideContent(SlideRequest)` → `SlideContentResponse { texts, tables: [{ headers, rows }], notes }` – Extrahuje texty, tabulky a metadata konkrétního slidu.
- `gRPC RenderSlideImage(SlideRequest)` → `SlideImageResponse { artifact_url }` – Renderuje slide jako PNG 1280×720 (720p, ~200 KB per slide) přes **LibreOffice Headless** (`--convert-to png`). Python-pptx na rendering nestačí (SmartArty, grafy). PNG ukládá do Blob Storage.
- **MetaTable Logic:** Algoritmus pro rekonstrukci tabulek z nestrukturovaného textu na základě vizuálních oddělovačů (tabulátor, mezery) a porovnání s hlavičkovým řádkem. Confidence threshold > 0.85 – pokud nižší, uložit jako plain text s příznakem `low_confidence`.

#### Excel Atomizer (processor-atomizers:xls)
- `gRPC ExtractStructure(ExtractRequest)` → `ExcelStructureResponse { sheets: [{ sheet_id, name, row_count, col_count }] }` – Vrací seznam listů.
- `gRPC ExtractSheetContent(SheetRequest)` → `SheetContentResponse { headers, rows: [{}], data_types }` – Konverze listu na JSON.
- **Partial Success State:** Pokud 1 z 10 listů selže, vrátí `{ status: "PARTIAL", successful: [...], failed: [{ sheet_id, error }] }`.

#### PDF / OCR Atomizer (processor-atomizers:pdf)
- `gRPC ExtractPdf(ExtractRequest)` → `PdfContentResponse` – Detekce, zda je PDF textové nebo skenované. Text extrahován přímo; skenované stránky přes Tesseract OCR.

#### CSV Atomizer (processor-atomizers:csv)
- `gRPC ExtractCsv(ExtractRequest)` → `CsvContentResponse { headers, rows }` – Automatická detekce oddělovače (`,;|\t`), kódování, hlavičkového řádku.

#### AI Gateway (processor-atomizers:ai)
- `gRPC AnalyzeSemantic(SemanticRequest)` → `SemanticResponse` – LiteLLM integrace pro sémantickou analýzu textu (klasifikace, sumarizace, extrakce entit).
- **Cost Control:** Každý request loguje počet spotřebovaných tokenů. Překročení kvóty vrací `429` s `{ quota_remaining: 0 }`.

#### Cleanup Worker (processor-atomizers:cleanup)
- CronJob: Každou hodinu smaže dočasné soubory (PNG slidy, CSV exporty) z Blob Storage starší než 24 hodin.
- Přidává se obrácený směr: processor-generators:pptx (generování). Cleanup Worker (processor-atomizers:cleanup) musí zahrnout i dočasné soubory generátoru. 

**Společné acceptance kritéria:**
- Každý Atomizer vrátí `200` + strukturovaný JSON nebo `{ artifact_url }` – nikdy inline binary data.
- Chybný soubor vrátí `422` s detailem chyby, nikdy `500`.
- Atomizer si soubor stahuje z Blob Storage sám (přes URL z requestu) – GW ho nepřeposílá.

---

### FS04 – Custom Orchestrator (engine-orchestrator)
**Priorita: KRITICKÁ**

**Pokrývající microservices:** engine-orchestrator

Business kontext: engine-orchestrator je mozek celého systému. Řídí tok dat, rozhoduje o routování, spravuje chyby a garantuje idempotenci. Implementován jako custom Java služba (Spring State Machine) s Type-Safe Contracts, Saga Pattern pro distribuované transakce, gRPC pro interní volání Atomizerů a Sinků. Workflow definice jako JSON soubory verzované v Gitu. Stav běžících flows v Redis, stav paused/waiting flows v PostgreSQL.

**Požadavky:**
- **Workflow Engine:** Spring State Machine. Workflow definice jako JSON soubory verzované v Gitu (ne GUI editor).
- **Type-Safe Contracts:** Java interfaces + DTOs pro veškerou komunikaci s Atomizery a Sinky. Žádné volné JSON objekty – každý krok má definovaný vstup a výstup.
- **Pipeline Workflow:** `Event (new_file)` → `Get Metadata` → `Router dle file type` → `Call Atomizer (gRPC)` → `Apply Schema Mapping (engine-data:template)` → `Store (engine-data (sink modules) via gRPC)`
- **Saga Pattern:** Distribuované transakce s rollback capability. Každý krok definuje compensating action pro případ selhání.
- **Async Worker Layer:** Dapr Pub/Sub (nebo RabbitMQ / Azure Service Bus) pro asynchronní zpracování. 20–50 paralelních slide extractions.
- **gRPC Internal Communication:** Všechna interní volání Atomizerů a Sinků přes gRPC (ne REST webhooky).
- **Filter Logic:** Po extrakci každého elementu: je-li tabulka → engine-data:sink-tbl (PostgreSQL); je-li text → engine-data:sink-doc (pgVector).
- **Error Handling:** Exponential backoff: 3 retry (1s, 5s, 30s), pak záznam do `failed_jobs`. Specifické exception types: `ParsingException`, `StorageException`, `VirusDetectedException`.
- **Idempotence:** Redis-based: `file_id + step_hash` jako klíč. Duplicate processing detekován a přeskočen.
- **State Management:** Redis pro stav běžících flows (nízká latence). PostgreSQL pro paused/waiting flows (persistence).
- **Dead Letter Queue:** Fatálně selhaný workflow odešle `{ file_id, error_stacktrace, timestamp }` do tabulky `failed_jobs`. Admin UI zobrazuje tyto záznamy pro manuální reprocessing.

**Acceptance kritéria:**
- Upload nového PPTX souboru spustí workflow automaticky bez manuálního zásahu.
- Při selhání Atomizeru (HTTP 500) jsou data uložena do `failed_jobs`, ne ztracena.
- Opětovné spuštění workflow pro stejné `file_id` nevytvoří duplicitní záznamy v DB.

---

### FS05 – Sinks – Storage APIs & Persistence
**Priorita: KRITICKÁ**

**Pokrývající microservices:** engine-data:sink-tbl, engine-data:sink-doc, engine-data:sink-log

Business kontext: Write-optimalizovaná API vrstva nad databázemi. Garantuje schéma validaci před zápisem, verzování a RLS enforcement.

**Table API – engine-data:sink-tbl (Java/Spring):**
- `gRPC BulkInsert(BulkInsertRequest)` → `BulkInsertResponse` – Bulk insert strukturovaných dat (tabulky, OPEX data) do PostgreSQL (JSONB). Voláno výhradně z engine-orchestrator přes Dapr gRPC.
- `gRPC DeleteByFileId(DeleteRequest)` → `DeleteResponse` – Compensating action pro Saga rollback.
- Flyway migrace pro správu schématu. RLS policy: uživatel vidí pouze záznamy svého `org_id`.
- Ukládá data z formulářů (FS19). Schéma: `form_responses` tabulka s `(org_id, period_id, form_version_id, field_id, value, submitted_at)`.
- **Čtení:** Data přístupná přes engine-data:query (CQRS read model, REST pro frontend).

**Document API – engine-data:sink-doc (Java/Spring):**
- `gRPC StoreDocument(StoreDocumentRequest)` → `StoreDocumentResponse` – Ukládání nestrukturovaného JSONu do PostgreSQL + generování vector embeddings (pgVector). Voláno výhradně z engine-orchestrator přes Dapr gRPC.
- `gRPC DeleteByFileId(DeleteRequest)` → `DeleteResponse` – Compensating action pro Saga rollback.
- Embeddings generovány asynchronně přes processor-atomizers:ai po uložení dokumentu. Model: **OpenAI text-embedding-3-small** (1536 dimenzí) přes Azure Foundry AI Services.
- **Čtení:** Data přístupná přes engine-data:query (CQRS read model, REST pro frontend).

**Log API – engine-data:sink-log (Java/Spring):**
- `gRPC AppendLog(AppendLogRequest)` → `AppendLogResponse` – Append-only zápis processing logů (`step_name`, `status`, `duration_ms`, `error_detail`). Voláno výhradně z engine-orchestrator přes Dapr gRPC.
- **Čtení:** Záznamy přístupné z engine-data:query (REST pro frontend a Admin UI).

**Databáze:**
- **PostgreSQL 16:** Primární store. JSONB pro semi-strukturovaná data, RLS pro tenant isolation, pgVector extension.
- **Redis:** Cache pro engine-data:query (TTL 5 min), rate limit counters, session tokens.
- **Blob Storage:** Fyzické soubory a dočasné artefakty (PNG slidy).

---

### FS06 – Analytics & Query – Read Model (CQRS)
**Priorita: VYSOKÁ**

**Pokrývající microservices:** engine-data:query, engine-data:dashboard, engine-data:search

Business kontext: Oddělený read model optimalizovaný pro rychlé čtení. Frontend nikdy nečte přímo z write databáze.

- **engine-data:query:** CQRS read API – **REST endpointy pro frontend** (přes API Gateway). Materialized views pro nejčastější dotazy. Redis caching s TTL. Jediný způsob, jak frontend čte data uložená přes Sinky.
- **engine-data:dashboard:** Agregační REST endpointy pro grafy a souhrny – **přístupné přes API Gateway pro frontend**. SQL over JSONB tabulky s GROUP BY / ORDER BY z UI konfigurace. Chart library: **Recharts** (lehká, React-nativní) pro standardní grafy + **Nivo** pro komplexní vizualizace (heatmaps). engine-data:dashboard musí zobrazovat i data pocházející z formulářů, nejen z parsovaných souborů. Zdroj dat je transparentní (flag `source_type: FORM / FILE`).
- **engine-data:search:** Full-text search (PostgreSQL FTS nebo ElasticSearch). Vector search přes pgVector pro sémantické dotazy. REST endpoint pro frontend přes API Gateway.

---

### FS07 – Admin Backend & UI
**Priorita: VYSOKÁ**

**Pokrývající microservices:** engine-core:auth, engine-core:admin

- **Role management:** Admin, Editor, Viewer. Hierarchické oprávnění v holdingové struktuře.
- **Secrets management:** Superadmin UI pro update secrets → přímý zápis do Azure KeyVault.
- **API Key management:** Generování klíčů pro service accounts. Klíče hashované (bcrypt), nikdy uloženy v plaintextu.
- **Failed Jobs UI:** Tabulka `failed_jobs` s detailem chyby a tlačítkem "Reprocess" pro manuální opakování.
-  **Správa formulářů a šablon**  - další admin sekce. Role "Reviewer" jako nová sub-role HoldingAdmina pro schvalování reportů (FS17)

---

### FS08 – Organizace souborů & Batch Management
**Priorita: VYSOKÁ**

**Pokrývající microservices:** engine-core:admin, engine-core:batch

- Každý soubor nese metadata o organizaci v holdingové struktuře (`holding_id`, `company_id`, `uploaded_by`).
- **Batch grouping:** HoldingAdmin vytvoří batch (např. `Q2/2025`). Uživatelé z dceřiných společností nahrávají soubory s tímto batch tagem. Holding vidí konsolidovaný report.
- **Row-Level Security (RLS):** Implementována přímo na PostgreSQL úrovni. Každý SQL dotaz automaticky filtrován dle `org_id` z JWT tokenu. Cross-tenant leak je architektonicky nemožný.
- **"Batch" se mapuje přímo na "Reporting Period"** (FS20). Koncepty se slučují – `period_id` nahrazuje generický `batch_id` tam, kde jde o OPEX reporting.

---

### FS09 – Frontend SPA (React)
**Priorita: KRITICKÁ**

**Pokrývající microservices:** frontend

- **Auth:** MSAL Provider, login/logout flow, automatický token refresh (Axios interceptor s retry).
- **Upload Manager:** Drag & Drop zóna (react-dropzone), progress bar přes XHR upload events, automatický refresh seznamu po uploadu (React Query invalidation).
- **Viewer:** Read-only zobrazení parsovaných dat slide-by-slide s preview obrázků (PNG z Blob).
- **Real-time feedback:** V P1 polling přes React Query (interval 3 s). SSE/WebSocket zavést v P2 (ušetří starosti s load-balancerem).
- **Orchestrátor trigger:** Spouštění engine-orchestrator workflow z frontendových akcí (přes API). Uživatel nemá přímý přístup k orchestrátoru.
- **Form Builder UI**
- **formulář pro vyplnění**
- **submission workflow UI**
- **period dashboard**
- **PPTX generator trigger**


**Knihovny:**
- State Management: TanStack Query (server state), Zustand (client state)
- UI Framework: Tailwind CSS + FluentUI (Microsoft)
- HTTP Client: Axios s nastavenými interceptory

---

### FS10 – Excel Parsing Logic
**Priorita: VYSOKÁ**

**Pokrývající microservices:** processor-atomizers:xls

- Excel listy parsovány per-sheet do JSONB. Každý list je samostatný záznam v DB.
- **Partial Success:** Soubor s 10 listy, kde 1 selže – 9 úspěšných uloženo, 1 označen `FAILED` s chybovým detailem. Soubor jako celek: `PARTIAL`.
- **Datová kompatibilita:** JSONB záznamy z Excelu a PPTX jsou nerozlišitelné na úrovni databáze – umožňuje jednotné dotazování.

---

### FS11 – Dashboards & SQL Reporting
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** frontend, engine-data:dashboard

- Dashboard vytváří Admin/Editor. Viewer vidí pouze Public dashboardy (příznak `is_public`).
- Data source: SQL dotazy nad JSONB tabulkami (PostgreSQL JSON functions). JSONB sloupce přístupné jako virtuální SQL tabulky.
- UI konfigurace: GROUP BY, ORDER BY, filtr datum/org definovatelný v UI bez znalosti SQL. Pro pokročilé: přímý SQL editor.

---

### FS12 – API Rozhraní & AI Integration (MCP)
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** processor-atomizers:ai, processor-generators:mcp, engine-data:query (AI/MCP proxy)

- API přístup přes API klíč (Bearer token v `Authorization` headeru).
- **MCP Server (processor-generators:mcp):** Integrace AI agentů. Server dědí OAuth token uživatele přes On-Behalf-Of flow – AI nikdy nemá globální přístup.
- **Security constraint:** MCP Server vynucuje RLS – každý AI dotaz je scoped na `org_id` uživatele.
- **Cost Control:** Měsíční token quota na úrovni uživatele i firmy. Překročení → `429`. Spotřeba viditelná v Admin UI.
- **AI/MCP REST Proxy (engine-data:query):** Frontend-facing REST endpointy na engine-data:query, které fungují jako proxy k interním AI a MCP službám:
  - `POST /api/query/ai/analyze` – AI sémantická analýza textu (proxy na processor-atomizers:ai přes Dapr).
  - `GET /api/query/ai/quota` – Stav spotřeby AI tokenů a zbývající kvóty pro organizaci.
  - `GET /api/query/mcp/health` – Health check MCP serveru (proxy na processor-generators:mcp přes Dapr).
  - Tyto endpointy jsou přístupné přes API Gateway (router → engine-data:query) a dědí autorizační kontext uživatele.

---

### FS13 – Notification Center & Alerts
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** engine-reporting:notification

- **In-app notifikace:** Push přes WebSocket/SSE při dokončení/chybě zpracování.
- **E-mail notifikace:** SMTP server pro kritické chyby a dokončení batch jobů.
- **Granulární nastavení:** Opt-in/Opt-out pro každý typ události (import, parsing fail, report ready) na úrovni uživatele i organizace.
- **notifikační triggery:** z FS17 (stavové přechody) a FS20 (deadliny, eskalace). 
- **Typy notifikací rozšířeny o:** `REPORT_SUBMITTED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `DEADLINE_APPROACHING`, `DEADLINE_MISSED`.

---

### FS14 – Data Versioning & Diff Tool
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** engine-core:versioning

> **Killer feature pro finanční controlling:** žádná editace nepřepisuje originál.

- **Versioning:** Každá změna dat vytváří novou verzi (v1 → v2). Originál vždy zachován.
- **Diff Tool:** UI zobrazuje rozdíl mezi verzemi (změna hodnoty, přidané/odebrané řádky). Pro OPEX data: `+500k v IT nákladech v2 vs v1`.

---

### FS15 – Template & Schema Mapping Registry
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** engine-data:template

> Řeší klíčový problém heterogenity dat: různé firmy pojmenovávají sloupce odlišně (`Cost`, `Náklady`, `Cena`).

- **Editor UI:** Definice mapovacích šablon. *"Pokud sloupec obsahuje Cena / Cost / Náklady → namapuj jako `amount_czk`."*
- **Learning:** Systém si pamatuje předchozí mapování a navrhuje je automaticky pro nové soubory.
- **engine-orchestrator integrace:** engine-data:template je voláno z engine-orchestrator workflow (gRPC) **PŘED** zápisem do DB.
- **Excel import do formuláře** – nové volání, už nejen z orchestrátoru. engine-data:template dostane nový endpoint `POST /map/excel-to-form`.
- **Schema Mapping REST API (engine-data:template):** Původně interní Dapr gRPC funkce template mappingu je nově vystavena i jako REST API přes API Gateway pro přímý přístup z frontendu:
  - `GET/POST /api/query/templates/mappings` – CRUD operace nad mapovacími šablonami.
  - `POST /api/query/templates/mappings/suggest` – Automatický návrh mapování na základě hlaviček sloupců.
  - `POST /api/query/templates/mappings/excel-to-form` – Inference mapování z Excel hlaviček na pole formuláře (FS19).
  - `GET/POST /api/query/templates/slide-metadata` – CRUD pro metadata slidů (popisky, kategorizace).
  - `POST /api/query/templates/slide-metadata/validate` – Validace slide metadat proti šabloně.
  - `GET /api/query/templates/slide-metadata/match` – Automatické párování slidů se šablonami na základě obsahu.

---

### FS16 – Audit & Compliance Log
**Priorita: STŘEDNÍ**

**Pokrývající microservices:** engine-core:audit

- **Immutable logy:** Záznamy "kdo-kdy-co viděl/editoval". Append-only tabulka (INSERT only, bez UPDATE/DELETE oprávnění pro app user).
- **Read Access Log:** Každé zobrazení citlivého reportu logováno (`User ID`, `Document ID`, `IP`, `Timestamp`).
- **AI Audit:** Každý prompt a odpověď AI (FS12) logován pro zpětnou kontrolu halucinací a úniku dat.
- **Export:** CSV/JSON export logů pro bezpečnostní audit holdingu.
- **Auditovány i stavové přechody** (stavové přechody z FS17) a veškeré akce ve formuláři (pole změněno, komentář přidán, import potvrzen).


## FS17 – OPEX Report Lifecycle & Submission Workflow
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** engine-reporting:lifecycle, engine-orchestrator

**Tech Stack:** Java 21 + Spring Boot (engine-reporting:lifecycle)

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

#### Architektura a Workflow customizace (engine-orchestrator vs engine-reporting:lifecycle)
- **engine-reporting:lifecycle:** Spravuje stavový automat entity `Report`, vystavuje endpointy pro přechody stavů, validuje oprávnění, loguje do engine-core:audit a publikuje event `report.status_changed` do Dapr PubSub.
- **engine-orchestrator:** Odebírá event `report.status_changed` a orchestruje následné kroky (např. notifikace, automatické kontroly dat, triggerování generování PPTX). Různé `report_type` mohou mít různý JSON workflow definition.

### Acceptance kritéria

- Editor nemůže odeslat report (`SUBMITTED`), dokud checklist nehlásí 100 % kompletnost.
- Přechod `APPROVED` automaticky triggeruje zahrnutí dat do centrálního reportingu.
- Přechod `REJECTED` automaticky odesílá notifikaci Editorovi (FS13) s komentářem.
- Audit log záznamu přechodu stavu obsahuje: `user_id`, `from_state`, `to_state`, `timestamp`, `comment`.
- Historie všech stavových přechodů jednoho reportu je zobrazitelná v UI (timeline view).

### Nová microservice

| Function ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **engine-reporting:lifecycle** | Report Lifecycle Service | Správa stavového automatu reportů, submission checklist, rejection flow, hromadné akce | Java 21 + Spring Boot | **L** |

---

## FS18 – PPTX Report Generation (Template Engine)
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** engine-reporting:pptx-template, processor-generators:pptx

**Tech Stack:** Python + FastAPI (processor-generators:pptx) + Java (engine-reporting:pptx-template)

### Business kontext

Dnes každá společnost generuje PPTX report sama – různé šablony, různé formáty grafů, různé pojmenování sekcí. HoldingAdmin pak manuálně sjednocuje vizuální podobu před prezentací vedení.

Nová funkce umožní **generovat standardizovaný PPTX report automaticky** ze strukturovaných zdrojových dat uložených v platformě, na základě centrálně spravované šablony.

Toto je **"obrácený Atomizer"** – místo extrakce dat z PPTX do DB jde o renderování dat z DB do PPTX.

### Požadavky

#### Správa PPTX šablon (engine-reporting:pptx-template)
- HoldingAdmin nahraje PPTX soubor jako šablonu (`POST /templates/pptx`). Šablony v tomto FS mají scope `CENTRAL` (vlastník = HoldingAdmin).
- Šablona obsahuje **placeholder tagy** ve formátu `{{variable_name}}` v textových polích, `{{TABLE:table_name}}` pro tabulky, `{{CHART:metric_name}}` pro grafy.
- Systém šablonu naparsuje a extrahuje seznam všech placeholderů → zobrazí v UI jako "požadované datové vstupy".
- Šablony jsou verzovány (v1, v2). Přiřazení šablony k `period_id` nebo `report_type`.
- Náhled šablony v UI bez dat (placeholder hodnoty zobrazeny jako ukázka).

#### Generování reportu (processor-generators:pptx)
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

| Function ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **engine-reporting:pptx-template** | PPTX Template Manager | Nahrávání, verzování a správa PPTX šablon; extrakce placeholderů; mapování na datové zdroje | Java 21 + Spring Boot | **L** |
| **processor-generators:pptx** | PPTX Generator | Renderování PPTX ze zdrojových dat + šablony; placeholder substituce; grafy; batch generování | Python + FastAPI (python-pptx, matplotlib) | **L** |

---

## FS19 – Dynamic Form Builder & Data Collection
**Priorita: KRITICKÁ**  

**Pokrývající microservices:** engine-reporting:form, frontend

**Tech Stack:** Java 21 + Spring Boot (engine-reporting:form) + React (součást frontend)

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
- Systém Excel naparsuje (processor-atomizers:xls) a nabídne **mapování sloupců → pole formuláře** (napojení na FS15 Schema Mapping).
- Editor zkontroluje a potvrdí mapování → data jsou importována do formuláře.
- Po importu jsou data editovatelná jako kdyby byla zadána ručně.
- Původní Excel soubor je uložen jako příloha reportu (auditní stopa).

#### Granularita formulářů
- V rámci FS19 se implementují pouze formuláře se scope `CENTRAL` (vlastník = HoldingAdmin, viditelnost = všechny přiřazené společnosti). Datový model engine-reporting:form však musí `scope` a `owner_org_id` zohledňovat od začátku (příprava na FS21).

### Acceptance kritéria

- HoldingAdmin vytvoří a publikuje nový formulář do 10 minut bez technických znalostí.
- Auto-save funguje; po ztrátě připojení a znovuotevření jsou data zachována.
- Validace formuláře vrátí seznam všech chybných polí najednou, nikoli po jednom.
- Import z Excelu: mapování sloupců navrženo automaticky na základě FS15; Editor potvrdí za < 2 minuty.
- Vyplněná data jsou okamžitě dostupná v centrálním reportingu bez dalšího zpracování.
- Formulář verze v1 a v2 jsou uložena odděleně; historická data nejsou přepsána upgradem formuláře.

### Nová microservice

| Function ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **engine-reporting:form** | Form Builder & Data Collection | Definice formulářů, správa verzí, sběr dat, validace, Excel import, napojení na engine-reporting:lifecycle | Java 21 + Spring Boot | **XL** |

---

## FS20 – Reporting Period & Deadline Management
**Priorita: VYSOKÁ**  

**Pokrývající microservices:** engine-reporting:period

**Tech Stack:** Java 21 + Spring Boot (engine-reporting:period)

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
- Napojení na engine-core:versioning (FS14): opravy v rámci periody vytvářejí verze, nikoli přepisují historii.

### Acceptance kritéria

- Vytvoření nové periody klonem z předchozí trvá < 2 minuty.
- Automatické uzavření formulářů po submission deadlinu bez manuálního zásahu.
- Notifikace odeslána 7/3/1 den před deadlinem všem uživatelům s `DRAFT` nebo nevyplněným formulářem.
- Dashboard periody se načte se stavem všech společností za < 3 s.
- Export statusu periody funkční pro 50+ společností.

### Nová microservice

| Function ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **engine-reporting:period** | Reporting Period Manager | Správa period a deadlinů, automatické uzavírání, completion tracking, eskalace, historické srovnání | Java 21 + Spring Boot | **M** |



## FS21 – Local Forms & Local PPTX Templates (Lokální scope)
**Priorita: STŘEDNÍ**  
**Fáze:** P5 (vlastní fáze po stabilizaci centrálního reportingu)

**Pokrývající microservices:** engine-reporting:form, engine-reporting:pptx-template, engine-core:admin, engine-reporting:lifecycle

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
- Generátor (processor-generators:pptx) je schopný generovat PPTX z lokální šablony pro potřeby interního reportu.
- Vygenerovaný lokální report není automaticky sdílen s holdingem.

#### Sdílení lokálních šablon
- CompanyAdmin může sdílet lokální šablonu nebo formulář s jiným CompanyAdminem v rámci stejného holdingu (`scope: SHARED_WITHIN_HOLDING`).
- HoldingAdmin má přehled o všech lokálních a sdílených šablonách/formulářích v holdingu.

## FS22 – Advanced Period Comparison (Granularita srovnání)
**Priorita: NÍZKÁ**  

**Pokrývající microservices:** engine-data:dashboard, engine-reporting:period

**Fáze:** P6 (po stabilizaci FS20)  
**Status: PLACEHOLDER – implementace later**

### Scope (zatím jen definice, ne implementace)

- Konfigurovatelné KPI pro srovnání: uživatel si definuje vlastní srovnávací metriky a kombinace dimenzí.
- Srovnání across types: Q1 vs. celý rok (normalizace na denní/měsíční bázi).
- Multi-org srovnání: holding vidí stejnou metriku pro všechny dceřiné společnosti vedle sebe.
- Drill-down: srovnání na úrovni cost center nebo divize (vyžaduje granularitu dat z FS19 lokálních formulářů).
- Export srovnávacích reportů jako PPTX (napojení na FS18 generátor).

*Tento FS bude detailně specifikován až po nasazení FS20 a prvních zkušenostech z provozu.*

**Fáze:** P7 (rozvojová aktivita)  
## FS23 – Service-Now API Integration & Automation
Priorita: STŘEDNÍ Pokrývající microservices: engine-integrations:servicenow, processor-generators:xls, engine-reporting:notification
- Napojení na API: Integrace se Service-Now (REST API) pro automatizovaný export dat (např. IT ticketing, asset management).
- Auth & Security: Přihlašovací údaje (OAuth2/Basic) uloženy v Azure KeyVault; komunikace probíhá přes šifrovaný kanál.
- Plánované úlohy (Scheduler): Možnost nastavit pravidelné intervaly stahování (denně, týdně).
- Report Distribution: Automatické generování Excel reportů z čerstvých dat (processor-generators:xls) a jejich odesílání na definované e-mailové adresy přes engine-reporting:notification.
- BI Dashboardy: Data jsou po stažení dostupná pro vizualizaci v engine-data:dashboard stejně jako data z PPTX/XLS.

## FS24 – Smart Persistence Promotion (Managed Tables)
Priorita: STŘEDNÍ Pokrývající microservices: engine-core:admin, engine-data:sink-tbl, engine-data:template
- Detekce frekvence: Systém sleduje využití konkrétních Schema Mappings (FS15). Pokud je stejná struktura nahrána více než X-krát (např. 5x), označí mapování jako "Candidate for Promotion".
- Návrh pro Admina: V Admin UI se zobrazí notifikace: "Tato šablona je používána často. Chcete pro ni vytvořit dedikovanou tabulku pro vyšší výkon?".
- Konverzní asistent: Systém vygeneruje návrh SQL schématu (názvy sloupců, optimální datové typy místo JSONB) na základě analýzy historicky nahraných dat.
- Admin Approval: Admin může návrh upravit (změnit délku polí, přidat indexy) a potvrdit. Až poté systém fyzicky vytvoří tabulku v PostgreSQL.
- Transparentní Routing: Po vytvoření tabulky engine-orchestrator automaticky přesměruje budoucí importy z obecného Sink-u do této nové struktury bez nutnosti měnit frontend.

---

## FS25 – Sink Browser, Manual Corrections & Extraction Learning
**Priorita: VYSOKÁ**  

**Pokrývající microservices:** engine-data:sink-tbl, engine-data:query, engine-data:template, processor-atomizers (pptx, xls), frontend

**Tech Stack:** Java 21 + Spring Boot (engine-data), Python + FastAPI (processor-atomizers), React (frontend)

### Business kontext

Při extrakci dat z PPTX prezentací (MetaTable logic) a částečně i z Excel souborů dochází k chybám – špatně rozpoznané hranice tabulek, chybné hlavičky, sloučené buňky nebo nesprávný datový typ. Uživatel dnes nemá možnost tyto chyby opravit, nemůže procházet jednotlivé datové části (sinky) nezávisle na zdrojovém souboru a nemůže určit, které sinky budou použity ve finálním reportu.

FS25 zavádí **Sink Browser** – UI pro procházení, korekci a výběr extrahovaných datových částí – a **Extraction Learning Pipeline**, která zpětnou vazbu z manuálních oprav využívá k vylepšení budoucích extrakcí.

### Datový model

#### Tabulka `sink_corrections` (overlay oprav nad immutable `parsed_tables`)
```sql
sink_corrections (
  id              UUID PK DEFAULT gen_random_uuid(),
  parsed_table_id UUID NOT NULL REFERENCES parsed_tables(id),
  org_id          UUID NOT NULL,
  row_index       INT,              -- NULL = header correction
  col_index       INT,
  original_value  TEXT,
  corrected_value TEXT,
  correction_type VARCHAR NOT NULL,  -- CELL_VALUE, HEADER_RENAME, ROW_DELETE, ROW_ADD, COLUMN_TYPE
  corrected_by    UUID NOT NULL,
  corrected_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  metadata        JSONB
)
```
+ RLS policy na `org_id`

#### Tabulka `sink_selections` (výběr sinků pro report/dashboard)
```sql
sink_selections (
  id              UUID PK DEFAULT gen_random_uuid(),
  parsed_table_id UUID NOT NULL REFERENCES parsed_tables(id),
  org_id          UUID NOT NULL,
  period_id       UUID,
  report_type     VARCHAR,
  selected        BOOLEAN DEFAULT true,
  priority        INT DEFAULT 0,
  selected_by     UUID,
  selected_at     TIMESTAMPTZ DEFAULT now(),
  note            TEXT,
  UNIQUE (parsed_table_id, period_id, report_type)
)
```
+ RLS policy na `org_id`

#### Tabulka `extraction_learning_log` (zpětná vazba pro AI learning)
```sql
extraction_learning_log (
  id                UUID PK DEFAULT gen_random_uuid(),
  file_id           UUID,
  parsed_table_id   UUID REFERENCES parsed_tables(id),
  org_id            UUID NOT NULL,
  source_type       VARCHAR,       -- PPTX, EXCEL, PDF
  slide_index       INT,
  error_category    VARCHAR,       -- MERGED_CELLS, WRONG_HEADER, MISSING_ROW, VALUE_FORMAT, SPLIT_TABLE
  original_snippet  JSONB,
  corrected_snippet JSONB,
  confidence_score  FLOAT,
  created_at        TIMESTAMPTZ DEFAULT now(),
  applied           BOOLEAN DEFAULT false
)
```
+ RLS policy na `org_id`

### Požadavky

#### Sink Browser (engine-data:query – REST endpointy pro frontend)
- `GET /api/query/sinks` – Procházení všech sinků s filtrací (org, file, sheet, source_type, date range, scope). Stránkovaný výstup.
- `GET /api/query/sinks/{parsed_table_id}` – Detail sinku s automaticky aplikovanými korekcemi (correction overlay).
- `GET /api/query/sinks/{parsed_table_id}/corrections` – Historie všech oprav pro daný sink.
- `GET /api/query/sinks/selections?period_id=X` – Seznam sinků vybraných pro danou periodu/report.
- `GET /api/query/sinks/{parsed_table_id}/preview` – Náhled dat s aplikovanými korekcemi (read-only view pro dashboard/report).

#### Manual Corrections (engine-data:sink-tbl – write operace, proxy přes query REST)
- `POST /api/query/sinks/{parsed_table_id}/corrections` – Uložení opravy (buňka, hlavička, řádek). Originální `parsed_tables` zůstávají **immutable** – opravy se ukládají jako overlay v `sink_corrections`.
- `POST /api/query/sinks/{parsed_table_id}/corrections/bulk` – Hromadné opravy (více buněk najednou).
- `DELETE /api/query/sinks/{parsed_table_id}/corrections/{correction_id}` – Zrušení konkrétní opravy (revert na originál).
- Při čtení dat se korekce aplikují on-the-fly přes query vrstvu.
- Každá korekce je auditována (kdo, kdy, co, proč) – napojení na FS16.

#### Sink Selection (výběr sinků pro report)
- `PUT /api/query/sinks/{parsed_table_id}/selection` – Označení/odznačení sinku pro použití v reportu/dashboardu.
- Uživatel může přidat poznámku proč je sink vybrán/vyloučen.
- Priorita (pořadí) sinků pro případ více zdrojů stejných dat.
- engine-data:dashboard (FS11) a processor-generators:pptx (FS18) musí respektovat `sink_selections` – pouze vybrané sinky s aplikovanými korekcemi se promítnou do výstupů.

#### Extraction Learning Pipeline (engine-data:template + processor-atomizers)
- Při uložení korekce se automaticky vytvoří záznam v `extraction_learning_log` s kategorizací typu chyby (`error_category`).
- engine-data:template vystavuje nový gRPC endpoint `GetLearningHints(file_type, header_pattern)` – vrátí historické korekce pro podobné extrakce.
- engine-orchestrator volá `GetLearningHints` **PŘED** uložením nově extrahovaných dat do sinku.
- processor-atomizers:pptx a processor-atomizers:xls přijímají volitelný `learning_hints` parametr – hints obsahují vzory oprav pro podobné tabulky (např. "Pro tabulky s hlavičkou obsahující 'Náklady' na slidu typu X typicky dochází k chybě sloučených buněk → aplikuj korekci Z").
- Confidence threshold v MetaTable logic se dynamicky upravuje na základě learning logu.
- **UI feedback:** Vizuální indikátor u opravené buňky "Systém se z této opravy naučí" + dropdown pro kategorizaci typu chyby.

### Frontend – Sink Browser UI

- **Nová stránka `/sinks`** (nebo nový tab v existujícím FileViewer):
  - **Sink List View:** Tabulka všech sinků s filtry (soubor, sheet, typ zdroje, datum, počet oprav). FluentUI DataGrid.
  - **Sink Detail View:** Zobrazení dat s inline editací buněk. Kliknutí na buňku → editace hodnoty. Opravené buňky vizuálně odlišeny (highlight barva + ikona). Tooltip s historií oprav.
  - **Selection Panel:** Checkbox u každého sinku + drag & drop pro řazení priority. Poznámka k výběru.
  - **Correction Summary:** Přehled všech oprav s možností bulk revert.
  - **Error Category Picker:** Při opravě uživatel volitelně kategorizuje typ chyby (MERGED_CELLS, WRONG_HEADER, MISSING_ROW, VALUE_FORMAT, SPLIT_TABLE).

### Dopad na existující Feature Sets

| FS | Dopad |
|---|---|
| **FS05 (Sinks)** | 3 nové tabulky + RLS policies, nové write operace v sink-tbl |
| **FS06 (Query)** | Nové REST endpointy s correction overlay logikou |
| **FS09 (Frontend)** | Nová stránka Sink Browser |
| **FS11 (Dashboards)** | Dashboardy musí respektovat `sink_selections` a aplikovat korekce |
| **FS15 (Schema Mapping)** | Rozšíření learning pipeline o correction feedback |
| **FS18 (PPTX Generator)** | Generátor musí číst pouze selected sinky s applied corrections |

### Acceptance kritéria

- Uživatel může procházet všechny sinky nezávisle na zdrojovém souboru s filtrací a stránkováním.
- Inline editace buňky v Sink Detail View uloží korekci bez modifikace originálního záznamu v `parsed_tables`.
- Opravená buňka je vizuálně odlišena od originálních dat (highlight + ikona).
- Uživatel může vybrat/odebrat sink pro report jedním kliknutím.
- Dashboard a PPTX generátor zobrazují/používají pouze vybrané sinky s aplikovanými korekcemi.
- Při importu nového souboru s podobnou strukturou systém aplikuje hints z `extraction_learning_log` a dosáhne měřitelně nižšího počtu chyb (metrika: počet manuálních oprav klesá v čase).
- Historie oprav je plně auditovatelná (kdo, kdy, co změnil, jaký byl originál).
- RLS zajišťuje, že uživatel vidí pouze sinky a korekce své organizace.

### Nové DB tabulky

| Tabulka | Účel | Effort |
|---|---|---|
| `sink_corrections` | Overlay oprav nad immutable parsed_tables | S |
| `sink_selections` | Výběr sinků pro report/dashboard/výstup | S |
| `extraction_learning_log` | Zpětná vazba pro AI learning z manuálních oprav | S |

### Effort odhad

| Komponenta | Effort |
|---|---|
| DB migrace (3 tabulky + RLS) | **S** |
| Entity classes + repositories (sink-tbl) | **S** |
| Correction service + gRPC endpoints (sink-tbl) | **M** |
| Query endpoints s correction overlay (query) | **M** |
| Selection service + endpoints | **S** |
| Frontend – Sink List View | **M** |
| Frontend – Sink Detail + inline editing | **L** |
| Frontend – Selection panel | **S** |
| Extraction learning log + template integration | **M** |
| Atomizer hints integration | **M** |
| **Celkem** | **L–XL** |

---

### FS26 – Report Generation UI & Export

**Priorita: VYSOKÁ**

**Pokryvajici microservices:** frontend, engine-reporting (lifecycle, pptx-template), processor-generators (pptx, xls)

Kompletni UI workflow pro tvorbu reportu, generovani PPTX ze sablon a export dat:

- **Create Report dialog:** Tlacitko "New Report" na /reports strance s vyberem organizace, periody a typu reportu (OPEX, CAPEX, Financial, General). Vytvori report ve stavu DRAFT.
- **Generate PPTX z reportu:** Po schvaleni reportu (status APPROVED) se na detailu reportu zobrazi tlacitko "Generate PPTX", ktere spusti asynchronni generovani. Frontend poluje stav jobu a po dokonceni nabidne "Download PPTX".
- **Univerzalni sablony:** PPTX sablony s placeholdery (`{{variable}}`, `{{TABLE:name}}`, `{{CHART:name}}`) se nahraji jednou pres /templates. Pri generovani se data meni podle batche/periody — sablona zustava stejna.
- **Batch generovani:** Hromadne generovani PPTX pro vice schvalenych reportu najednou (/batch-generation).
- **Excel export (planovano):** REST endpoint na engine-reporting, ktery zavola processor-generators Excel renderer. UI tlacitko "Export Excel" na /reports.
- **PDF export (planovano):** Konverze PPTX → PDF pres LibreOffice headless nebo dedickovany PDF renderer.

**Datovy tok:**
```
Report (APPROVED) → Template (PPTX s placeholdery) → processor-generators
                                                         ↓
                                                  Vyplneny PPTX/Excel
                                                         ↓
                                                  Download pres frontend
```

## FS27 – Live Excel Export & External Sync
**Priorita: VYSOKÁ**

**Pokrývající microservices:** engine-integrations:excel-sync, engine-data:query, processor-generators:xls, frontend

**Tech Stack:** Java 21 + Spring Boot (engine-integrations), Python + FastAPI (processor-generators:xls), React (frontend)

### Business kontext

Holdingový controlling dnes udržuje řadu Excel souborů (na SharePointu, sdílených discích nebo lokálně), do kterých manuálně přepisuje data z různých zdrojů. Tyto soubory obsahují manuálně vytvořené souhrny, grafy a pivotní tabulky, které slouží jako podklad pro management reporting. Ruční aktualizace datových listů je časově náročná a náchylná k chybám.

FS27 zavádí **Live Excel Export** – mechanismus, který automaticky aktualizuje definovaný list v cílovém Excel souboru při každém nahrání nových dat do RA Toolu. Uživatel nakonfiguruje "Excel Export Flow" podobně jako dashboard (FS11) – definuje SQL dotaz do DB, cílový soubor a cílový sheet. Ostatní listy v souboru (manuální souhrny, grafy, pivotní tabulky) zůstávají nedotčeny.

### Klíčové funkce

- **Export Flow Definition:** Uživatel (Admin/Editor) vytváří Export Flow přes UI. Konfigurace zahrnuje:
  - **SQL dotaz** (nebo UI builder jako v FS11) – definuje, jaká data se exportují.
  - **Cílový soubor** – cesta na síťovém/lokálním disku NEBO SharePoint URL.
  - **Cílový sheet** – název listu v Excelu, který bude přepsán novými daty. Ostatní listy zůstávají beze změny.
  - **Pojmenování souboru** – buď uživatelem definované fixní jméno, nebo automatické odvození z Batch Name (pattern: `{batch_name}_export.xlsx`).
  - **Trigger typ** – automatický (při každém importu dat do RA Toolu) nebo manuální (tlačítko "Export Now").

- **Partial Sheet Update (processor-generators:xls):** Rozšíření existujícího Excel generátoru o schopnost:
  - Otevřít existující Excel soubor (ne vytvářet nový).
  - Přepsat obsah **pouze definovaného listu** (clear + write).
  - Zachovat všechny ostatní listy včetně formátování, grafů, vzorců a pivotních tabulek.
  - Pokud cílový sheet neexistuje, vytvořit ho.

- **Storage Connectors (engine-integrations:excel-sync):**
  - **SharePoint connector:** Microsoft Graph API (`/drives/{drive-id}/items/{item-id}/content`) pro čtení a zápis Excel souborů na SharePoint Online. OAuth2 app-only credentials uložené v KeyVault.
  - **Local/Network path:** Přímý zápis na lokální nebo síťový disk (UNC path `\\server\share\file.xlsx`). Vyžaduje, aby kontejner měl namountovaný volume.
  - Konfigurace konektoru je součástí Export Flow Definition.

- **Trigger mechanismus:**
  - engine-orchestrator publikuje event `data-imported` (Dapr PubSub) po úspěšném zpracování a uložení dat.
  - engine-integrations:excel-sync odebírá tento event, filtruje podle org_id/batch_id a spouští příslušné Export Flows.
  - Podporuje i manuální trigger přes REST endpoint (frontend tlačítko "Export Now").

- **Frontend UI:**
  - Nová stránka `/export-flows` (nebo tab v existujícím Dashboards).
  - **List View:** Přehled nakonfigurovaných Export Flows s posledním stavem (success/fail/pending), časem posledního exportu.
  - **Create/Edit dialog:** Formulář pro definici Export Flow (SQL query editor, cílový soubor, sheet name, trigger typ, pojmenování).
  - **Export History:** Log všech provedených exportů s výsledkem a případnou chybovou hláškou.
  - **Manual Export:** Tlačítko "Export Now" pro okamžité spuštění.

### Datový tok

```
Data Import (engine-orchestrator)
       ↓ Dapr PubSub: "data-imported"
engine-integrations:excel-sync
       ↓ SQL query přes engine-data:query (Dapr gRPC)
       ↓ Data result (JSON)
processor-generators:xls (Dapr gRPC)
       ↓ Partial sheet update → updated Excel binary
engine-integrations:excel-sync
       ↓ Write to target (SharePoint API / network path)
       ↓ Status event → frontend notification
```

### DB schéma

#### Tabulka `export_flow_definitions`
```sql
export_flow_definitions (
  id                UUID PK DEFAULT gen_random_uuid(),
  org_id            UUID NOT NULL,
  name              VARCHAR(255) NOT NULL,
  description       TEXT,
  sql_query         TEXT NOT NULL,
  target_type       VARCHAR(20) NOT NULL,   -- 'SHAREPOINT' | 'LOCAL_PATH'
  target_path       TEXT NOT NULL,           -- SharePoint URL nebo UNC/local path
  target_sheet      VARCHAR(255) NOT NULL,   -- název cílového listu
  file_naming       VARCHAR(20) DEFAULT 'CUSTOM', -- 'CUSTOM' | 'BATCH_NAME'
  custom_file_name  VARCHAR(255),
  trigger_type      VARCHAR(20) DEFAULT 'AUTO',   -- 'AUTO' | 'MANUAL'
  trigger_filter    JSONB,                   -- filtr: batch_id, org_id, source_type
  sharepoint_config JSONB,                   -- tenant_id, drive_id, client_id (secret v KeyVault)
  is_active         BOOLEAN DEFAULT true,
  created_by        UUID NOT NULL,
  created_at        TIMESTAMPTZ DEFAULT now(),
  updated_at        TIMESTAMPTZ DEFAULT now()
)
```
+ RLS policy na `org_id`

#### Tabulka `export_flow_executions`
```sql
export_flow_executions (
  id                UUID PK DEFAULT gen_random_uuid(),
  flow_id           UUID REFERENCES export_flow_definitions(id),
  org_id            UUID NOT NULL,
  trigger_source    VARCHAR(20),   -- 'AUTO' | 'MANUAL'
  trigger_event_id  UUID,          -- reference na source event (batch_id)
  status            VARCHAR(20),   -- 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  rows_exported     INT,
  target_path_used  TEXT,
  error_message     TEXT,
  started_at        TIMESTAMPTZ DEFAULT now(),
  completed_at      TIMESTAMPTZ
)
```
+ RLS policy na `org_id`

### Požadavky

#### Export Flow Management (engine-integrations:excel-sync – REST endpointy pro frontend)
- `GET /api/export-flows` – Seznam Export Flows pro aktuální organizaci. Stránkovaný výstup.
- `GET /api/export-flows/{id}` – Detail Export Flow včetně posledního stavu exekuce.
- `POST /api/export-flows` – Vytvoření nového Export Flow.
- `PUT /api/export-flows/{id}` – Úprava existujícího Export Flow.
- `DELETE /api/export-flows/{id}` – Smazání Export Flow (soft delete – deaktivace).
- `POST /api/export-flows/{id}/execute` – Manuální spuštění Export Flow.
- `GET /api/export-flows/{id}/executions` – Historie exekucí daného flow.
- `POST /api/export-flows/{id}/test` – Dry-run: provede SQL dotaz a vrátí preview dat bez zápisu do souboru.

#### Partial Sheet Update (processor-generators:xls – rozšíření)
- Nový gRPC endpoint `UpdateSheet(ExcelBinary, SheetName, Data)` – přijme binární obsah existujícího Excel souboru, název sheetu a data (JSON rows), přepíše obsah daného sheetu a vrátí aktualizovaný Excel binary.
- Pokud sheet neexistuje, vytvoří nový.
- Zachová všechny ostatní sheety, formátování, grafy a vzorce.

### Dopad na existující Feature Sets

| FS | Dopad |
|---|---|
| **FS04 (Orchestrator)** | Nový PubSub event `data-imported` po úspěšném uložení dat |
| **FS06 (Query)** | Exekuce SQL dotazů z Export Flow definic (existující funkcionalita) |
| **FS11 (Dashboards)** | Sdílený SQL query builder UI komponent |
| **FS13 (Notifications)** | Notifikace o úspěchu/selhání exportu |
| **FS23 (Integrations)** | Rozšíření engine-integrations o nový modul excel-sync |

### Acceptance kritéria

- Admin/Editor může vytvořit Export Flow s SQL dotazem, cílovým souborem a sheet name.
- Při automatickém triggeru (nahrání dat) se cílový Excel soubor aktualizuje do 60 s.
- V cílovém Excelu je přepsán **pouze definovaný list** – ostatní listy zůstávají 100% beze změny (včetně grafů, vzorců, formátování).
- Uživatel může zvolit pojmenování souboru: fixní jméno nebo automatické z Batch Name.
- SharePoint connector se autentizuje přes OAuth2 (app-only) s credentials v KeyVault.
- Local/Network path zápis funguje přes namountovaný Docker volume.
- Manuální export (tlačítko "Export Now") provede okamžitý export.
- Historie exportů je dostupná v UI s detaily (čas, počet řádků, status, chyba).
- Dry-run mode vrátí preview dat bez skutečného zápisu.
- RLS zajišťuje, že uživatel vidí pouze Export Flows své organizace.
- Selhání exportu nevyvolá chybu v hlavním processing pipeline (fire-and-forget s logováním).

### Nové DB tabulky

| Tabulka | Účel | Effort |
|---|---|---|
| `export_flow_definitions` | Konfigurace Export Flows (SQL, target, trigger) | S |
| `export_flow_executions` | Historie a stav provedených exportů | S |

### Effort odhad

| Komponenta | Effort |
|---|---|
| DB migrace (2 tabulky + RLS) | **S** |
| engine-integrations:excel-sync (flow management, triggers) | **L** |
| SharePoint connector (Microsoft Graph API, OAuth2) | **M** |
| Local/Network path writer | **S** |
| processor-generators:xls – partial sheet update | **M** |
| Frontend – Export Flow List & Create/Edit UI | **M** |
| Frontend – Execution History & Manual Export | **S** |
| engine-orchestrator – PubSub event `data-imported` | **S** |
| **Celkem** | **L** |

---

### FS99 – DevOps & Observability
**Priorita: VYSOKÁ**

**Pokrývající microservices:** CI/CD, Observability stack

- **CI/CD:** Pipeline pro Linting → Unit Testy → Integration Testy → Docker Build → Push to Registry. Oddělená pipeline pro GraalVM Native Image build (release).
- **OpenTelemetry tracing:** E2E trace přes celý stack: Frontend → API Gateway → engine-orchestrator → Atomizer → Sink. Jaeger/Tempo jako trace backend.
- **Centralizované logy:** Loki nebo ELK stack. Structured JSON logging ze všech služeb.
- **Metriky:** Prometheus scrape z každé služby. Grafana dashboardy: chybovost, engine-orchestrator workflow queue, Atomizer latence, DB connection pool.
- **Health Metrics Aggregation (engine-orchestrator):** Vlastní business-level health metrics endpoint na orchestrátoru:
  - `GET /api/v1/health-metrics` – Agregované metriky o stavu orchestrace (počet běžících workflows, průměrná latence, error rate, queue depth). Volán z engine-core:admin přes Dapr service invocation pro zobrazení v Admin Health Dashboard.
- **Local Dev:** `tilt up` spustí kompletní topologii v lokálním K8s (Kind) nebo Docker Compose s hot-reloadem pro React a Python služby.

---

## 7. Katalog Deployment Units & Modulů

| # | Deployment Unit | Moduly | Popis / Odpovědnost | Feature Sets | Tech Stack |
|---|---|---|---|---|---|
| 1 | **frontend** | — | React SPA – upload, viewer, dashboardy, form filler, sink browser, notifikace (WebSocket/SSE), MSAL auth | FS09, FS11, FS25 | React 18 + Vite + TS + FluentUI |
| 2 | **router** | — | Nginx API Gateway – Host-based routing, Azure Front Door (WAF + SSL), rate limiting, ForwardAuth | FS01 | Nginx (config) |
| 3 | **engine-core** | auth, admin, batch, versioning, audit | Validace Entra ID tokenů, RBAC, KeyVault, API keys, holdingová hierarchie, verzování dat, diff tool, auditní logy, Failed Jobs UI | FS01, FS07, FS08, FS14, FS16 | Java 21 + Spring Boot |
| 4 | **engine-ingestor** | ingestor, scanner | Streaming upload, MIME validace, ClamAV antivirová kontrola, sanitizace, Blob Storage, trigger engine-orchestrator | FS02 | Java 21 + Spring Boot + ClamAV |
| 5 | **engine-orchestrator** | — | Workflow engine (Spring State Machine), Saga Pattern, Type-Safe Contracts, gRPC routing, Redis state, exponential backoff retry, DLQ | FS04 | Java 21 + Spring Boot |
| 6 | **engine-data** | sink-tbl, sink-doc, sink-log, query, dashboard, search, template | Sinky pro strukturovaná data/dokumenty/logy, CQRS read model, Redis cache, BI dashboard agregace, full-text + vector search, Schema Mapping Registry, Sink Browser & Corrections | FS05, FS06, FS11, FS15, FS25 | Java 21 + Spring Boot |
| 7 | **engine-reporting** | lifecycle, period, form, pptx-template, notification | Stavový automat reportů, správa period a deadlinů, dynamic form builder, PPTX šablony, in-app + e-mail notifikace | FS17, FS18, FS19, FS20, FS13, FS26 | Java 21 + Spring Boot |
| 8 | **engine-integrations** | servicenow, excel-sync | ServiceNow API integrace, OAuth2, scheduled sync, report distribution, Live Excel Export & External Sync (SharePoint / local path) | FS23, FS27 | Java 21 + Spring Boot |
| 9 | **processor-atomizers** | pptx, xls, pdf, csv, ai, cleanup | Bezstavové extraktory dat z PPTX/Excel/PDF/CSV, LiteLLM AI gateway, cleanup worker, extraction learning hints | FS03, FS10, FS12, FS25 | Python + FastAPI |
| 10 | **processor-generators** | pptx, xls, mcp | Generování PPTX/Excel reportů ze šablon, partial sheet update pro Live Excel Export, MCP Server pro AI agenty (OBO flow) | FS18, FS23, FS12, FS27 | Python + FastAPI |

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
| **P1** | MVP Core | router, engine-core:auth, engine-ingestor, engine-ingestor:scanner, engine-orchestrator, processor-atomizers:pptx, engine-data:sink-tbl, engine-data:sink-doc, engine-data:sink-log, frontend (základní) | FS01, FS02, FS03-PPTX, FS04, FS05, FS09-basic | Funkční upload + extrakce PPTX + základní viewer |
| **P2** | Extended Parsing | processor-atomizers:xls, processor-atomizers:pdf, processor-atomizers:csv, processor-atomizers:cleanup, engine-data:query, engine-data:dashboard | FS03-rest, FS10, FS06 | Plná podpora formátů + BI dashboardy |
| **P3a** | Intelligence & Admin | engine-core:admin, engine-core:batch, processor-atomizers:ai, processor-generators:mcp, engine-data:template | FS07, FS08, FS12, FS15 | Holdingová hierarchie + AI integrace + schema mapping |
| **P3b** | Lifecycle + Period Mgmt  |engine-reporting:lifecycle, engine-reporting:period | | Řízení OPEX cyklu, deadliny, stavový automat |
| **P3c** | Form Builder – centrální (engine-reporting:form, Excel export/import) | | | Sběr dat bez Excelu po e-mailu |
| **P4a** | Enterprise Features | engine-reporting:notification, engine-core:versioning, engine-core:audit, engine-data:search, engine-data:dashboard (extended) | FS11, FS13, FS14, FS16 | Plná enterprise výbava: versioning, diff tool, compliance |
| **P4b** | PPTX Generator (engine-reporting:pptx-template, processor-generators:pptx) | | |  Automatické generování standardizovaných reportů |
| **P5** | DevOps Maturity | Observability stack (Prometheus, Grafana, Loki, OTEL), CI/CD pipelines | FS99 | Production-ready: monitoring, tracing, automated 
| **P6** | Advanced Period Mgmt (deadliny, eskalace, as-is srovnání) | | | Plná správa reportingových cyklů |pipelines |
| **P7** | FS22 Advanced Comparison (placeholder) | | |  Granulární srovnání period |
| **P8** | Sink Browser & Corrections | engine-data (sink-tbl, query, template), processor-atomizers, frontend | FS25 | Procházení sinků, manuální korekce, výběr dat pro report, extraction learning |
| **P9** | Live Excel Export & External Sync | engine-integrations:excel-sync, processor-generators:xls, engine-data:query, frontend | FS27 | Automatická aktualizace externích Excel souborů (SharePoint / síťový disk) při importu dat |





---

## 9. Risk Register

| # | Riziko | Pravd. | Dopad | Mitigace | Owner |
|---|---|---|---|---|---|
| R1 | Payload Size: přenášení velkých JSONů (base64 obrázky) způsobí OOM crash | M | KRITICKÝ | Atomizery NIKDY neposílají binary inline. Vždy ukládají do Blob a vracejí URL. gRPC streaming pro velké payloady. | processor-atomizers, engine-orchestrator |
| R2 | Auth Token Propagation: desynchronizace tokenů mezi FE → GW → engine-orchestrator → Backend | M | VYSOKÝ | Strict Token Propagation policy. Dapr middleware automaticky propaguje `Authorization` header. E2E testy pokrývají celý auth flow. | router, engine-core:auth |
| R3 | Scope Creep: postupné přidávání features způsobí nerealistické termíny | H | STŘEDNÍ | Striktní fázový rollout (P1–P5). FS kategorizovány jako MUST/SHOULD/COULD. Scope freeze pro každou fázi. | PM / Architekt |
| R4 | Komplexita orchestrátoru: JSON workflow definice a Saga patterns se stanou neudržitelnými | M | STŘEDNÍ | Workflow definitions verzovány v Gitu jako JSON. Code review povinný. Type-Safe Contracts garantují kompatibilitu. Unit testy pro každý workflow step. | engine-orchestrator |
| R5 | PostgreSQL RLS konfigurace: chybná policy způsobí cross-tenant data leak | L | KRITICKÝ | Automatické integráční testy ověřují RLS po každé DB migraci. Minimální oprávnění pro app user (no superuser). | engine-data:sink-tbl |
| R6 | Vendor Lock-in: Azure specifické závislosti (Blob, Entra ID, KeyVault) | M | STŘEDNÍ | Abstrakční vrstva pro storage (interface → Azure/S3). Auth abstrahován přes OIDC standard. KeyVault nahraditelný HashiCorp Vault. | Architekt |
| R7 | Výkonnostní bottleneck: serializační overhead Dapr sidecar pro high-throughput Atomizery | L | STŘEDNÍ | Load testy v P1. Možnost přímé HTTP komunikace Atomizer↔Sink bez Dapr pro vysoce výkonné paths. | processor-atomizers |

*Legenda: Pravděpodobnost: H = Vysoká, M = Střední, L = Nízká*

---

## 10. Závislosti & Předpoklady

### 10.1 Interní závislosti

| Závislá služba | Závisí na | Typ závislosti |
|---|---|---|
| engine-ingestor | engine-core:auth, engine-ingestor:scanner, Blob Storage | HARD – bez auth a skenu nelze přijmout soubor |
| engine-orchestrator | engine-ingestor (event), processor-atomizers, engine-data (sink modules), Redis | HARD – orchestrátor bez zdrojů, sinks a state store nefunguje |
| engine-data:sink-tbl/DOC | PostgreSQL (RLS, Flyway migrace) | HARD – DB musí být migrována před nasazením |
| engine-data:template | engine-orchestrator (gRPC volání), PostgreSQL (mapování history) | SOFT – engine-orchestrator může fungovat bez TMPL (bez normalizace) |
| processor-generators:mcp | engine-core:auth (OBO flow), engine-data:query | HARD – AI nesmí fungovat bez autorizace |

### 10.2 Externí závislosti

- **Azure Entra ID tenant:** Nakonfigurovaná App Registration s RBAC skupinami.
- **Azure KeyVault:** Provisionovaný vault s oprávněními pro MSI aplikace.
- **Azure Blob Storage nebo S3:** Bucket/container s odpovídajícími CORS pravidly.
- **SMTP server:** Pro e-mailové notifikace (FS13).
- **LiteLLM / OpenAI API:** Pro AI Gateway (FS03, FS12). Potřeba API klíč a quota.

---

## 11. Kommunikační matice (Dapr)

> **Princip:** Interní služby komunikují **výhradně** přes Dapr gRPC. REST se používá **pouze** pro edge služby komunikující s frontendem přes API Gateway.

| Caller | Callee | Protokol | Typ | Poznámka |
|---|---|---|---|---|
| frontend | router | REST (HTTPS) | Sync | Frontend → API Gateway (jediný vstupní bod) |
| router | engine-core:auth | REST (auth_request) | Sync | Nginx auth_request – REST vyžadován Nginxem |
| router | engine-ingestor | REST | Sync | Frontend-facing upload endpoint |
| router | engine-data:query | REST | Sync | Frontend-facing read API |
| router | engine-data:dashboard | REST | Sync | Frontend-facing dashboard API |
| router | engine-core:admin | REST | Sync | Frontend-facing admin API |
| router | engine-data:query (AI/MCP proxy) | REST | Sync | Frontend-facing AI analýza, quota, MCP health (FS12) |
| router | engine-data:template (Schema Mapping) | REST | Sync | Frontend-facing mapovací šablony a slide metadata (FS15) |
| engine-core:admin | engine-orchestrator | Dapr gRPC | Sync | Health metrics agregace (FS99) |
| engine-ingestor | engine-ingestor:scanner | Dapr gRPC | Sync | Interní: AV scan před uložením |
| engine-ingestor | engine-orchestrator | Dapr Pub/Sub | Async | Event `file-uploaded` → trigger workflow |
| engine-orchestrator | processor-atomizers | Dapr gRPC | Sync | Interní: orchestrátor volá Atomizery |
| engine-orchestrator | engine-data (sink modules) | Dapr gRPC | Sync | Interní: orchestrátor volá Sinky |
| engine-orchestrator | engine-data:template | Dapr gRPC | Sync | Interní: schema mapping před uložením |
| engine-orchestrator | engine-reporting:notification | Dapr Pub/Sub | Async | Event-driven notifikace |
| engine-orchestrator | Redis | TCP | State mgmt | Running workflow state (nízká latence) |
| engine-reporting:lifecycle | engine-orchestrator | Dapr Pub/Sub | Async | Event `report.status_changed` |
| engine-reporting:notification | frontend | WebSocket / SSE | Push | Real-time notifikace do prohlížeče |
| engine-data:query | PostgreSQL | TCP | Read | CQRS read model |
| engine-data:query | Redis | TCP | Cache | TTL 5 min |
| engine-data (sink modules) | PostgreSQL | TCP | Write | Přímý DB přístup |
| engine-core:audit | PostgreSQL | TCP | Write | Append-only |

---

## 12. Glossary

| Termín | Definice |
|---|---|
| **Atomizer** | Bezstavový mikroservice specializovaný na extrakci dat z konkrétního formátu (PPTX, Excel, PDF, CSV). Vždy voláni přes engine-orchestrator (gRPC). |
| **Sink** | Write-optimalizovaná API vrstva pro trvalé uložení zpracovaných dat do databáze. Implementuje CQRS write side. |
| **Batch** | Sada souborů ze stejného reportovacího období (např. Q2/2025) seskupená pro hromadné zpracování a konsolidaci. |
| **RLS** | Row-Level Security – PostgreSQL mechanismus zajišťující, že uživatel vidí pouze řádky patřící jeho organizaci, přímo na úrovni DB enginu. |
| **DLQ** | Dead Letter Queue – tabulka `failed_jobs`, kam engine-orchestrator ukládá informace o fatálně selhaných workflow pro manuální reprocessing. |
| **CQRS** | Command Query Responsibility Segregation – oddělení write modelu (Sinks) od read modelu (engine-data:query, engine-data:dashboard) pro optimalizaci výkonu. |
| **Schema Mapping** | Konfigurovatelné pravidlo pro normalizaci názvů sloupců z různých zdrojových souborů do jednotného interního datového modelu. |
| **Dapr Sidecar** | Kontejner běžící vedle každé mikroslužby, který abstrahuje inter-service komunikaci, state management a PubSub. |
| **OBO flow** | On-Behalf-Of OAuth flow: AI agent (processor-generators:mcp) používá token uživatele pro volání downstream API, nikoli vlastní servisní identitu. |
| **GraalVM Native Image** | Kompilace Java aplikace do nativního binárního souboru se sub-sekundovým startem – vhodné pro scale-to-zero Atomizery. |
| **ForwardAuth** | Nginx middleware (auth_request) přesměrovávající každý příchozí request na engine-core:auth pro validaci tokenu PŘED předáním backend službě. |
| **OPEX report** | Operating Expenditure report – finanční výkaz provozních nákladů, primární datový typ zpracovávaný touto platformou. |

---

*PPTX Analyzer & Automation Platform – Project Charter v5.3 | Duben 2026 | Interní dokument*


## ChangeLog

### v5.3 – 2026-04-28 – Multi-Backend Storage (POSTGRES / SPARK / BLOB) + Frontend DataSource Switcher

#### Kontext
Upgrade připravující platformu na soubežný provoz tří storage backendů pro tabulková data. Nová architektura umožňuje přepnout uložiště pro konkrétní organizaci/typ souboru bez restartu služby, a zároveň zobrazit v UI ze kterého zdroje data pocházejí.

#### Backend – engine-data:sink-tbl

| Soubor | Změna |
|--------|-------|
| `SparkTableStorageBackend.java` | Oprava viditelnosti konstanty `BACKEND_TYPE` → `public static final` |
| `BlobTableStorageBackend.java` | **Nová třída** – 3. storage backend; serializuje záznamy jako JSON a nahrává je do Azure Blob Storage (`{orgId}/{fileId}/{sheet}.json`). Implementuje `TableStorageBackend` (bulkInsert + deleteByFileId). |
| `engine-data/pom.xml` | Přidána managed dependency `azure-storage-blob:12.28.1` |
| `sink-tbl/pom.xml` | Přidána dependency `azure-storage-blob` pro `BlobTableStorageBackend` |
| `StorageRoutingAdminController.java` | Rozšířena validace backendu o hodnotu `BLOB` |

#### Backend – engine-data:query

| Soubor | Změna |
|--------|-------|
| `ParsedTableEntity.java` (query read-only) | Přidáno pole `storageBackend` mapované na sloupec `storage_backend` |
| `QryParsedTableRepository.java` | Přidány metody pro filtrování podle `storageBackend` (4 varianty + `countByOrgIdAndStorageBackend`) |
| `SinkListItemDto.java` | Přidáno pole `String storageBackend` do record |
| `SinkQueryService.java` | `listSinks()` rozšířeno o parametr `storageBackend`; větvení dotazu pro filtrování; `toListItem()` mapuje `storageBackend` z entity |
| `SinkBrowserController.java` | `GET /api/query/sinks` – přidán query param `?storage_backend=`; injektován `DataSourceStatsService`; přidán endpoint `GET /api/query/sinks/data-source-stats` |
| `DataSourceStatsService.java` | **Nová třída** – agreguje statistiky ze tří backendů: počet řádků v Postgres, dostupnost a počet blobů v Azure Blob, dostupnost Spark API (health probe) |
| `query/pom.xml` | Přidána dependency `azure-storage-blob` pro `DataSourceStatsService` |

#### Backend – engine-data:app (DB migrace)

| Migrace | Popis |
|---------|-------|
| `V11_0_3__sinktbl_add_blob_backend.sql` | Rozšiřuje `CHECK` constraint na `storage_routing_config.backend` o hodnotu `'BLOB'`; odstraňuje případný CHECK na `parsed_tables.storage_backend` |

#### Backend – engine-data:app (Application)

| Soubor | Změna |
|--------|-------|
| `EngineDataApplication.java` | Přidána anotace `@EnableScheduling` (potřebná pro `StorageRoutingService.refreshRules()`) |

#### Shared TypeScript typy – @reportplatform/types

| Soubor | Změna |
|--------|-------|
| `sinks.ts` | Přidán typ `StorageBackend = 'POSTGRES' \| 'SPARK' \| 'BLOB'`; do `SinkListItem` přidáno volitelné pole `storageBackend?`; do `SinkListFilters` přidáno pole `storage_backend?: StorageBackend \| 'ALL'` |
| `storageRouting.ts` | **Nový soubor** – typy `DataSourceMode`, `StorageRoutingRule`, `UpsertRoutingRuleRequest`, `DataSourceStats` |
| `index.ts` | Exportován nový modul `storageRouting` |

#### Frontend – API a hooks

| Soubor | Popis |
|--------|-------|
| `api/storageRouting.ts` | **Nový soubor** – klientské funkce: `listRoutingRules`, `upsertRoutingRule`, `deleteRoutingRule`, `refreshRoutingRules`, `getDataSourceStats` |
| `hooks/useStorageRouting.ts` | **Nový soubor** – TanStack Query hooks: `useStorageRoutingRules`, `useUpsertRoutingRule`, `useDeleteRoutingRule`, `useRefreshRoutingRules`, `useDataSourceStats` (auto-refresh 60 s) |

#### Frontend – nové komponenty

| Komponenta | Popis |
|-----------|-------|
| `DataSource/DataSourceSwitcher.tsx` | **Nová komponenta** – přepínač zdroje dat (ALL / POSTGRES / SPARK / BLOB) s count badges, ikonami dostupnosti (✓/✗) a tooltips |
| `Admin/StorageRoutingPanel.tsx` | **Nová komponenta** – admin tabulka pravidel routování s možností přidání/smazání pravidla a force-refresh cache; chráněno: globální výchozí pravidlo nelze smazat |

#### Frontend – integrace do stránek

| Stránka | Změna |
|---------|-------|
| `SinkBrowserPage.tsx` | Integrován `DataSourceSwitcher` nad filtry; přidán sloupec `Storage` (badge POSTGRES/SPARK/BLOB) do tabulky; stav `dataSourceMode` řídí query param `storage_backend` |
| `AdminPage.tsx` | Přidána záložka **Storage Routing** s komponentou `StorageRoutingPanel` |
| `FileDetailPage.tsx` | V pravém sidebaru přidána sekce **Uložená data** – zobrazuje počty sheets per storage backend (dle dat ze `useSinks`) |

#### Dokumentace

| Soubor | Popis |
|--------|-------|
| `docs/spark-migration-guide.md` | Existující průvodce migrací; platí i pro BLOB backend – viz sekce „Monitoring" a „Pre-Switch Checklist" |

---

### v5.3.1 – 2026-04-28 – Security Hardening, Error Handling & Build Fixes

#### Kontext
Čtyři průchody cross-cutting oprav zaměřených na bezpečnost (SQL injection v RLS interceptorech), robustnost (tiché výjimky → log.warn, unsafe collection access), správné HTTP status kódy a vynucení multi-tenant validace. Bez nových feature sets.

#### Security – SQL Injection v RLS interceptorech

Všechny `set_config()` volání přešly z string concatenace na JDBC parametry. Oprava se týká všech vrstev kde EntityManager volá PostgreSQL session proměnné.

| Soubor | Změna |
|--------|-------|
| `engine-data/dashboard/config/RlsInterceptor.java` | `"set_config('app.current_org_id', '" + orgId + "'"` → `.setParameter("orgId", orgId)` a `.setParameter("role", sanitizedRole)` |
| `engine-reporting/common/config/RlsInterceptor.java` | Stejné + opraveny parametry `userId` a `primaryRole` |
| `engine-data/query/config/RlsInterceptor.java` | Stejné (předchozí kolo) |
| `engine-data/query/service/SinkQueryService.java` | `SET LOCAL` → `set_config()` s parametrem |
| `engine-data/query/service/NamedQueryService.java` | Stejné |
| `engine-data/query/service/QueryService.java` | Stejné |
| `engine-data/dashboard/service/AggregationService.java` | `set_config` přes `JdbcTemplate.execute(ConnectionCallback)` s `PreparedStatement` |

#### Security & Robustnost – Validace vstupů a HTTP status kódy

| Soubor | Změna |
|--------|-------|
| `engine-core/admin/controller/AdminController.java` | `catch (IllegalArgumentException ignored)` při UUID parsování `X-Org-Id` → `log.warn` + vrátí `400 Bad Request` |
| `engine-core/admin/controller/PromotionController.java` | `listCandidates` tiché `200+empty` catch → `500`; `rejectCandidate` vždy 204 → UUID chyba `400`, service chyba `500`; `triggerMigration` stejné |
| `engine-core/batch/controller/BatchController.java` | `UUID.randomUUID()` fallback pro `holdingId` odstraněn → `400` pokud chybí nebo neplatný formát |
| `engine-core/versioning/controller/VersionController.java` | Version-not-found `200+empty` → `404`; generic catch `200+empty` → `500`; `getDiff` IllegalArgumentException → `404` |
| `engine-integrations/servicenow/controller/IntegrationController.java` | `listConnections` required; `createConnection` `instanceUrl`/`credentialsRef` required; odstraněn `stub.service-now.com`; `deleteConnection` IllegalArgumentException → `404` |
| `engine-reporting/form/controller/FormController.java` | Odstraněn `"default-org"` fallback → `400`; `importExcel` catch byl `200 IMPORTED` → `500 FAILED` |
| `engine-reporting/form/controller/FormResponseController.java` | Odstraněn `"default-org"` fallback → `400` |
| `engine-reporting/lifecycle/controller/ReportController.java` | Odstraněn `"default-org"` + `UUID.randomUUID()` pro `periodId` → `400` |
| `engine-data/dashboard/controller/DashboardController.java` | `createDashboard` a `updateDashboard` – přidána null orgId validace → `400` |
| `engine-data/query/controller/FileQueryController.java` | Hardcoded 1×1 placeholder PNG bytes → `501 Not Implemented` s `log.warn` |

#### Robustnost – Tiché výjimky → log.warn

| Soubor | Změna |
|--------|-------|
| `engine-reporting/common/config/HeaderAuthenticationFilter.java` | `catch (Exception ignored)` při JWT subject extrakci → `log.warn`; přidán `Logger` |
| `engine-data/common/config/HeaderAuthenticationFilter.java` | Stejné |
| `engine-integrations/servicenow/service/ItsmSyncService.java` | 2× `catch (Exception ignored)` (resolution time, age_days) → `log.warn` s kontextem |
| `engine-integrations/servicenow/service/ProjectFetchService.java` | `catch (DateTimeParseException ignored)` → `log.warn`; `catch (NumberFormatException e) { return null; }` → `log.warn` |
| `engine-data/query/service/QueryService.java` | `catch (NumberFormatException ignored)` při parsování slide indexu → `log.warn` |
| `engine-data/dashboard/service/AggregationService.java` | `catch (NumberFormatException e) { return 0.0; }` → `log.warn` |
| `engine-data/dashboard/service/ComparisonService.java` | Stejné |
| `engine-integrations/servicenow/controller/ScheduleController.java` | Tiché fallback na defaultní cron `"0 0 0 * * ?"` → `logger.warn` |

#### Robustnost – Unsafe collection access

| Soubor | Změna |
|--------|-------|
| `engine-integrations/excel-sync/connector/SharePointConnector.java` | Trojité `json.get("value").get(0).get("id")` bez null checks → explicitní guard na `valueNode == null`, prázdné pole a chybějící `id` pole |

#### Exception Handlery – scoping a doplnění

| Soubor | Změna |
|--------|-------|
| `engine-core/admin/exception/GlobalExceptionHandler.java` | **Nový soubor** – `@RestControllerAdvice(basePackages="com.reportplatform.admin")`; IllegalArgumentException→400, NoSuchElementException→404, AccessDeniedException→403, Exception→500 |
| `engine-core/batch/exception/GlobalExceptionHandler.java` | **Nový soubor** – stejná struktura, scoped na `com.reportplatform.batch` |
| `engine-core/versioning/exception/GlobalExceptionHandler.java` | Přidán `basePackages`; IllegalArgumentException opraven na 400; přidán `NoSuchElementException→404` |
| `engine-core/audit/exception/GlobalExceptionHandler.java` | Přidán `basePackages`; přidán `NoSuchElementException→404`; přidán `log.warn` do IllegalArgumentException handleru |

#### Frontend – dynamická pole a opravy

| Soubor | Změna |
|--------|-------|
| `pages/ExcelImportPage.tsx` | Odstraněn hardcoded seznam 17 polí → dynamický `useQuery` volající `templatesApi.getFormFields(formId)` |
| `api/AiMcpProxyController.java` | Raw `Map.class` v RestTemplate volání → `ParameterizedTypeReference<Map<String,Object>>()` (3 volání) |

#### Build – opravy TypeScript chyb

| Soubor | Chyba → Oprava |
|--------|----------------|
| `components/Admin/StorageRoutingPanel.tsx` | Nepoužitý `React` import → odstraněn |
| `pages/FileDetailPage.tsx` | Nepoužitý `StorageBackend` type import → odstraněn |
| `pages/NamedQueryPage.tsx` | Nepoužitý `EditRegular` icon import → odstraněn |
| `pages/ProjectsPage.tsx` | Nepoužitý `Search24Regular`; odstraněn unused `connSearch`/`setConnSearch` state; type chyba `c.name` → `c.instance_url` (`ServiceNowConnection` nemá pole `name`) |
| `pages/TextTemplateEditorPage.tsx` | Nepoužitý `TextTemplate` type import → odstraněn |
| `pages/TextTemplateRenderPage.tsx` | Nepoužitý `Card` import → odstraněn |

#### Python – opravy tichých výjimek

| Soubor | Změna |
|--------|-------|
| `processor-atomizers/pptx/service/spatial_extractor.py` | `except Exception: pass` → `except (AttributeError, TypeError) as e: logger.debug(...)` |
| `processor-atomizers/pptx/service/pptx_service.py` | 2× `except OSError: pass` → `except OSError as e: logger.debug(...)` |

---

### v5.4 – 2026-04-28 – P8-W7 ServiceNow Project Sync + P8-W6/W9 Reporting Frontend + Stub/Mock Cleanup

#### Kontext
Implementace třetí vlny P8 rozšíření: synchronizace projektů ze ServiceNow (pm_project, pm_project_task, pm_project_budget_plan) s výpočtem RAG statusu a KPI metrik; kompletní frontend pro Reporting (Named Queries, Text Templates, Projekty); eliminace všech zbývajících STUB/MOCK odpovědí v backend controllerech.

---

#### Backend – engine-integrations (ServiceNow Project Sync)

| Soubor | Změna |
|--------|-------|
| `V2_0_2__snow_project_sync.sql` | **Nová migrace** – tabulka `snow_project_sync_config`: konfigurační záznamy per-connection (syncScope ALL/ACTIVE_ONLY/BY_MANAGER, filterManagerEmails, budgetCurrency, RAG prahy: ragAmberBudgetThreshold, ragRedBudgetThreshold, ragAmberScheduleDays, ragRedScheduleDays, syncEnabled, lastSyncedAt); unique constraint na `connection_id` |
| `ProjectSyncConfigEntity.java` | **Nová entita** – JPA mapping na `snow_project_sync_config`; BigDecimal thresholds; boolean syncEnabled |
| `ProjectSyncConfigDto.java` | **Nový record DTO** – full projekce entity včetně timestamps |
| `UpsertProjectSyncConfigRequest.java` | **Nový record** – patch-style request; `@NotBlank` na `budgetCurrency` |
| `ProjectSyncConfigRepository.java` | **Nový repository** – `findByConnectionId`, `existsByConnectionId` |
| `ProjectFetchService.java` | **Nová service** – stránkované fetchování ze SN API (pm_project + pm_project_task + pm_project_budget_plan); `enrichProjectRecord()` přidává `_kpi_*` pole (budgetUtilizationPct, costForecastAccuracy, scheduleVarianceDays, milestoneCompletionRate); `calculateRag()` vrací RED/AMBER/GREEN dle konfigurovatelných prahů; výsledky persistovány přes Dapr → ms-sink-tbl `/api/v1/projects/upsert` |
| `IntegrationController.java` | Rozšířen o injekci `ProjectFetchService`; přidány 3 endpointy: `GET /{connectionId}/project-sync`, `POST /{connectionId}/project-sync`, `POST /{connectionId}/project-sync/trigger` |

---

#### Backend – engine-data (Snow Projects Query Layer)

| Soubor | Změna |
|--------|-------|
| `V12_0_3__snow_project_tables.sql` | **Nová migrace** – tabulky `snow_projects` (KPI sloupce: budget_utilization_pct, schedule_variance_days, milestone_completion_rate, cost_forecast_accuracy, rag_status), `snow_project_tasks`, `snow_project_budgets`; RLS politiky na všech 3 tabulkách; seed 3 systémových Named Queries (Project RAG Dashboard, Project Budget Overview, Project Milestone Status) |
| `SnowProjectEntity.java` | **Nová entita** – read-only JPA mapping na `snow_projects` |
| `SnowProjectTaskEntity.java` | **Nová entita** – read-only JPA mapping na `snow_project_tasks` |
| `SnowProjectBudgetEntity.java` | **Nová entita** – read-only JPA mapping na `snow_project_budgets` |
| `SnowProjectRepository.java` | **Nový repository** – JPQL filtrovaný dotaz s RAG ordering (RED → AMBER → GREEN), filtry: orgId, connectionId, ragStatus, status, managerEmail; stránkování |
| `SnowProjectTaskRepository.java` | **Nový repository** – `findByOrgIdAndProjectId` |
| `SnowProjectBudgetRepository.java` | **Nový repository** – `findByOrgIdAndProjectId` |
| `SnowProjectDto.java` | **Nový record DTO** – kompletní projekce projektu; volitelné listy `tasks` a `budgets` (null v list view, naplněné v detail view) |
| `SnowProjectTaskDto.java` | **Nový record DTO** – projekce task entity |
| `SnowProjectBudgetDto.java` | **Nový record DTO** – projekce budget entity |
| `SnowProjectController.java` | **Nový controller** – `GET /api/v1/data/snow/projects` (stránkované, filtrovatelné), `GET /api/v1/data/snow/projects/{id}` (full detail s tasks + budgets) |

---

#### Shared TypeScript typy – @reportplatform/types

| Soubor | Změna |
|--------|-------|
| `namedQueries.ts` | **Nový soubor** – typy `NamedQuery`, `CreateNamedQueryRequest`, `UpdateNamedQueryRequest`, `NamedQueryExecuteRequest`, `NamedQueryResult` |
| `textTemplates.ts` | **Nový soubor** – typy `TextTemplate`, `BindingEntry`, `DataBindings`, `OutputFormat`, `TemplateType`, `RenderRequest`, `RenderResponse` |
| `projects.ts` | **Nový soubor** – typy `SnowProject`, `SnowProjectTask`, `SnowProjectBudget`, `SnowProjectPage`, `ProjectSyncConfig`, `RagStatus`, `ProjectListFilters` |
| `index.ts` | Přidány exporty modulů `namedQueries`, `textTemplates`, `projects` |

---

#### Frontend – API klienti

| Soubor | Popis |
|--------|-------|
| `api/namedQueries.ts` | **Nový soubor** – `listNamedQueries`, `getNamedQuery`, `createNamedQuery`, `updateNamedQuery`, `deleteNamedQuery`, `executeNamedQuery` |
| `api/textTemplates.ts` | **Nový soubor** – `listTextTemplates`, `getTextTemplate`, `createTextTemplate`, `updateTextTemplate`, `deleteTextTemplate`, `renderTextTemplate` |
| `api/projects.ts` | **Nový soubor** – `listProjects`, `getProject`, `getProjectSyncConfig`, `upsertProjectSyncConfig`, `triggerProjectSync` |

---

#### Frontend – nové stránky

| Stránka | Popis |
|---------|-------|
| `NamedQueryPage.tsx` | Katalog Named Queries – tabulka se search + datasource filtrem; dialog pro vytvoření (name, datasource, SQL); dialog pro spuštění s JSON preview výsledku (limit 50 řádků) |
| `TextTemplateListPage.tsx` | Přehled text templates – tabulka s akcemi Edit a Render per řádek |
| `TextTemplateEditorPage.tsx` | Editor šablony – name/description/templateType/outputFormats; markdown editor obsahu; správa data bindings (přidat/odebrat/upravit binding napojený na Named Query) |
| `TextTemplateRenderPage.tsx` | Renderer šablony – výběr output formátu; automatická extrakce runtime parametrů z `{{input.X}}` ve vazebních params; tlačítko Download vygenerovaného souboru |
| `ProjectsPage.tsx` | Přehled ServiceNow projektů – stránkovaná tabulka s RAG badge, ProgressBar pro budget utilization, barevné schedule variance; filtry connection/RAG/status/manager; trigger sync tlačítko |
| `ProjectDetailPage.tsx` | Detail projektu – KPI card grid (budget utilization, schedule variance, milestone rate, cost forecast); TabList pro tasks a budgets; zvýrazňování po termínu; odkaz na generování reportu |

#### Frontend – modifikované soubory

| Soubor | Změna |
|--------|-------|
| `App.tsx` | Přidáno 7 nových routes: `/reporting/named-queries`, `/reporting/text-templates`, `/reporting/text-templates/new`, `/reporting/text-templates/:templateId/edit`, `/reporting/text-templates/:templateId/render`, `/projects`, `/projects/:projectId` |
| `AppLayout.tsx` | Přidána sekce **Reporting** v navigaci (Text Templates – `DocumentText24Regular`, Named Queries – `CodeBlock24Regular`, Projects – `BriefcaseRegular`) |

---

#### Eliminace STUB/MOCK odpovědí

Všechny catch bloky vracející falešné 200/201 odpovědi bez chybového kontextu nahrazeny správnými HTTP chybovými kódy.

| Soubor | Metoda | Stará odpověď | Nová odpověď |
|--------|--------|--------------|--------------|
| `IntegrationController.java` | `getConnection` catch | `200 { id: "stub-...", status: "UNKNOWN" }` | `404 NOT_FOUND` |
| `IntegrationController.java` | `createConnection` catch | `201 { id: "created-..." }` | `500 INTERNAL_SERVER_ERROR` |
| `IntegrationController.java` | `updateConnection` catch | `200 { id: ..., status: "UPDATED" }` | `500 INTERNAL_SERVER_ERROR` |
| `IntegrationController.java` | `testConnection` catch | `200 { tested: true, latencyMs: 0 }` | `200 { success: false, message: e.getMessage() }` |
| `IntegrationController.java` | `triggerSync` catch | `202 { status: "SYNC_TRIGGERED" }` | `500 { status: "FAILED", message: e.getMessage() }` |
| `PromotionController.java` | `updateCandidate` catch | `200 {}` | `500 INTERNAL_SERVER_ERROR` |
| `PromotionController.java` | `approveCandidate` catch | `200 {}` | `500 INTERNAL_SERVER_ERROR` |
| `VersionController.java` | `createVersionForEntity` catch | `201 { entityId: ... }` | `500 INTERNAL_SERVER_ERROR` |
| `BatchController.java` | `createBatch` catch | `201 null body` | `500 INTERNAL_SERVER_ERROR` |
| `ReportController.java` | `createReport` catch | `201 null body` | `500 INTERNAL_SERVER_ERROR` |
| `ExcelImportPage.tsx` | upload error branch | mock `MappingSuggestion[]` vrácen při chybě | zůstane na upload stepu; zobrazí chybovou hlášku |

---

### v5.4.1 – 2026-04-28 – Security & Robustness Fixes (Code Review P8)

#### Kontext
Opravy nalezené při code review Phase 8 změn. Žádné nové feature sets – výhradně bezpečnostní, robustnostní a testovací opravy identifikované při analýze diff větve.

---

#### [HIGH] Security – SQL Injection v `NamedQueryService.assertReadOnly`

| Soubor | Změna |
|--------|-------|
| `engine-data/query/service/NamedQueryService.java` | Blocklist `String[] forbidden = {"DELETE ", "INSERT ", …}` (jednoduché `contains` + trailing space) nahrazen 12 pre-kompilovanými `Pattern` s word-boundary `\b`. Catchuje `DELETE\nFROM`, `DELETE\tFROM`, víceřádkové DML i DML ve WITH-CTE. Přidána klíčová slova `EXECUTE`, `EXEC`, `CALL`, `GRANT`, `REVOKE`. |
| `engine-data/query/service/NamedQueryService.java` | `assertReadOnly()` voláno nově i při `create()` a `update()` (storage-time validace) – nejen při `execute()` (runtime). Přidána statická konstanta `FORBIDDEN_SQL_PATTERNS = List.of(...)`. |

---

#### [MEDIUM] Security – Unencoded `orgId` v URL (`AiMcpProxyController`)

| Soubor | Změna |
|--------|-------|
| `engine-data/query/controller/AiMcpProxyController.java` | Endpoint `GET /ai/quota`: přímá string concatenace `"?orgId=" + orgId` → `UriComponentsBuilder.fromUriString(...).queryParamIfPresent("orgId", Optional.ofNullable(orgId)).toUriString()`. Eliminuje potenciální URL injection při nestandardním formátu orgId. |
| `engine-data/query/controller/AiMcpProxyController.java` | Timeouty RestTemplate hardcoded na 5s/30s → konfigurovatelné přes `dapr.proxy.connect-timeout-seconds` a `dapr.proxy.read-timeout-seconds` (výchozí hodnoty zachovány). `RestTemplate` inicializován v `@PostConstruct` po injekci `@Value` polí. |

---

#### [MEDIUM] Robustnost – `ItsmSyncService`: JSON parsing, transakce, timezone

| Soubor | Změna |
|--------|-------|
| `engine-integrations/servicenow/service/ItsmSyncService.java` | `parseJsonArray(String json)`: ruční regex-split JSON pole (`replaceAll + split(",")`) nahrazen `objectMapper.readValue(json, new TypeReference<List<String>>() {})`. Správně zpracovává escaped znaky, whitespace a prázdné pole. `ObjectMapper` přidán do konstruktoru. |
| `engine-integrations/servicenow/service/ItsmSyncService.java` | `syncGroup()` změněno na `@Transactional(propagation = REQUIRES_NEW)` – každá skupina ITSM sync běží v nezávislé transakci. `syncAllGroupsForConnection()` zbaveno `@Transactional` a volá `self.syncGroup(...)` přes self-injection (`@Lazy @Autowired ItsmSyncService self`) tak, aby Spring proxy zachytil anotaci `REQUIRES_NEW`. Chyba v jedné skupině je zalogována (`log.error`) a ostatní skupiny pokračují (best-effort sémantika). |
| `engine-integrations/servicenow/service/ItsmSyncService.java` | `enrichItsmRecord()`: parsování SN datetime z `value.replace(" ", "T") + "Z"` (předpokládá UTC suffix bez validace) → nový helper `parseSnDatetime(String)` s primárním `DateTimeFormatter("yyyy-MM-dd HH:mm:ss", ZoneOffset.UTC)` a fallbackem na `Instant.parse()` pro ISO-8601. Přidána statická konstanta `SN_DATETIME_FORMATTER`. |

---

#### [LOW] Dokumentace kontraktu – `SparkTableStorageBackend` návratová hodnota

| Soubor | Změna |
|--------|-------|
| `engine-data/sink-tbl/backend/TableStorageBackend.java` | Javadoc `bulkInsert()` rozšířen: explicitně dokumentuje rozdíl mezi synchronním POSTGRES backendem (vrací počet durably persistovaných záznamů) a asynchronním SPARK backendem (vrací `records.size()` = počet odeslaných událostí, skutečná persistence probíhá mimo JVM). Stejné pro `deleteByFileId()`. |
| `engine-data/sink-tbl/backend/SparkTableStorageBackend.java` | Javadoc `bulkInsert()` upřesněn: „submitted for async ingestion", odkaz na interface kontrakt. |

---

#### [LOW] Robustnost – `NamedQueryController`: Exception handlery

| Soubor | Změna |
|--------|-------|
| `engine-data/query/controller/NamedQueryController.java` | Přidány 3 `@ExceptionHandler` metody: `IllegalArgumentException` → `400 Bad Request` (neplatný UUID v `X-Org-Id` nebo SQL validace), `NoSuchElementException` → `404 Not Found`, `IllegalStateException` → `409 Conflict` (pokus o modifikaci system query nebo spuštění neaktivní query). Bez těchto handlerů všechny vyhazovaly `500 Internal Server Error`. |

---

#### Testy – nové unit testy

| Soubor | Pokrytí |
|--------|---------|
| `engine-data/query/src/test/…/NamedQueryServiceTest.java` | **Nový soubor** – 15 testů. `AssertReadOnly_Rejected`: 18 SQL vzorů (DML keywords, nested DML v CTE, multi-statement, prázdný SQL). `AssertReadOnly_Allowed`: 5 bezpečných SELECT vzorů včetně identifikátorů obsahujících keyword (např. `update_count`, `deleted_at`). `Execute_Validation`: neaktivní query → `IllegalStateException`; entita s DML SQL → `IllegalArgumentException` při execute. |
| `engine-data/sink-tbl/src/test/…/StorageRoutingServiceTest.java` | **Nový soubor** – 9 testů pokrývajících všech 5 úrovní specificity pravidel: org+source → fallback na POSTGRES při prázdných pravidlech; org+source beats org+wildcard; org beats source; source beats global; case-insensitive source matching; null orgId; výjimka při absenci backendu. |
