# Project Charter: PPTX Analyzer & Automation Platform
**Version:** 3.0 (RC - Release Candidate)
**Status:** APPROVED / PRE-DEVELOPMENT
**Architecture Pattern:** Event-Driven Microservices with Low-Code Orchestration
**Docs Reference:** docs/project_standards.md, docs/dod_criteria.md

---

# 1. Executive Summary
Cílem projektu je vybudovat robustní, škálovatelnou platformu pro bezpečný příjem, analýzu a vizualizaci dat z nestrukturovaných souborů (PPTX, Excel, PDF). Systém se zaměřuje na automatizaci extrakce finančních dat (OPEX reporty) a jejich převod do strukturované podoby pro BI a analytiku.

Architektura je postavena jako sada mikroslužeb (Polyglot: Java/Python), kde business logiku a tok dat řídí N8N orchestrátor. Frontend je oddělená React aplikace zabezpečená přes Azure Entra ID.

# 2. High-Level Architecture
Systém se skládá ze čtyř vrstev:
1.  **Presentation Layer:** React Frontend (SPA).
2.  **Ingestion Layer:** API Gateway + Ingestor Service (dumb & fast, streaming).
3.  **Processing Layer:** N8N Orchestrator řídící bezstavové "Atomizers" (Python/Java Workers).
4.  **Persistence Layer:** "Sinks" (Storage APIs) a databáze (Postgres, Redis, Vector DB, Blob).


# 3. Technical Standards & Constraints
Dle `docs/STANDARDS.md` a `docs/dod_criteria.md`.

* **Backend Core:** Java 21 + Spring Boot 3.
* **Backend AI/Data:** Python + FastAPI + Pydantic.
* **Frontend:** React 18 + Vite + TypeScript + Tailwind CSS.
* **Communication:** Dapr Sidecars (gRPC internal, REST external).
* **Auth:** Azure Entra ID, Token v2, Scope `api://<client_id>/access_as_user`.
* **Documentation:** Každý modul má `README.md` s Mermaid diagramem a `test-result.md`.
* **Testing:** Unit testy pro logiku, Mocking externích služeb.

**NOTE:** Pokud není určeno jinak, primárním programovacím jazykem je **Java (Spring Boot)**.

## Technology Stack Versions
| Komponenta | Technologie | Verze (Min) |
| :--- | :--- | :--- |
| **Runtime** | Java (JDK) | 21 (LTS) |
| **Runtime** | Python | 3.11+ |
| **Runtime** | Node.js | 20 (LTS) |
| **Container** | Docker | Latest |
| **Cache** | Redis | Latest Stable |
| **Orchestrator** | n8n | Latest Stable |
| **DB** | PostgreSQL | 16 |
| **Vector DB** | PostgreSQL + pgVector | Latest |
| **Security Scan** | ClamAV / ICAP | Latest |
| **API Docs** | Swagger / OpenAPI 3.0 | Auto-generated |
| **Schema Registry** | Apicurio / JSON Schema Store | Centralizace schémat |
| **Migration Tool** | Flyway / Liquibase | Správa verzí DB schématu |
| **E2E Test** | Playwright | React + API + N8N flow |
| **Local Dev** | Tilt / Skaffold | K8s Dev Experience |

## Ports Allocation
| Služba | Port Host | Port Container | Debug Port |
| :--- | :--- | :--- | :--- |
| **Frontend (Vite)** | `3000` | `3000` | - |
| **API Gateway (Traefik)** | `8080` | `80` | - |
| **Auth Service** | `8081` | `8000` | `5005` |
| **File Ingestor** | `8082` | `8000` | `5006` |
| **PPTX Atomizer** | `8090` | `8000` | `5678` |
| **Excel Atomizer** | `8091` | `8000` | `5679` |
| **Sink: Table API** | `8100` | `8080` | `5005` |
| **n8n Webhook Listener** | `5678` | `5678` | - |
| **PostgreSQL** | `5432` | `5432` | - |
| **Redis** | `6379` | `6379` | - |

## Libraries

### Frontend (React)
- **Build Tool:** Vite
- **State Management:** TanStack Query (Server State), Zustand (Client State).
- **UI Framework:** Tailwind CSS + Radix UI / Shadcn.
- **HTTP Client:** Axios (s nastavenými interceptory).

### Backend - Python (FastAPI)
- **Web Framework:** FastAPI + Uvicorn.
- **Validace:** Pydantic v2.
- **HTTP Client:** Httpx (Async).
- **Testing:** Pytest.

### Backend - Java (Spring Boot)
- **Framework:** Spring Boot (Standard OpenJDK pro dlouhodobě běžící služby, GraalVM volitelně pro scale-to-zero parsery).
- **DB Access:** Spring Data JPA / Hibernate.
- **Testing:** JUnit 5 + Mockito.

---

# 4. Definition of Done (DoD) Checklist
Před uzavřením jakékoliv Feature (FS) musí být splněno:
- [ ] **Code Quality:** Linting (ESLint/Black/Checkstyle) bez chyb.
- [ ] **Tests:** Unit testy pokrývají novou logiku, Happy path i Edge cases.
- [ ] **Security:** Žádné hardcoded secrets, Auth tokeny validovány.
- [ ] **Docs:** Aktualizované README a Mermaid diagramy.
- [ ] **Build:** CI pipeline prošla (Build + Test).

# 5. Known Risks
1.  **N8N Payload Size:** Přenášení velkých JSONů (base64 obrázky) přes N8N může způsobit memory issues.
    * *Mitigace:* Atomizers ukládají binárky do Blobu a vrací jen URL.
2.  **Auth Complexity:** Synchronizace tokenů mezi Frontend -> Gateway -> N8N -> Backend.
    * *Mitigace:* Strict Token Propagation policies.

---

# 6. Scope & Feature Sets (FS)

## FS01: Infrastructure & Core
**Focus:** Základní kameny aplikace.
- Kontejnery s N8N, Traefik, Redis, Grafana a Prometheus.
- Minimal Base Image pro všechny mikroslužby (Java, Python).
- **API Gateway:** (Traefik/APIM) Routing `/api/auth`, `/api/upload`, `/api/query`. SSL terminace, Rate limiting.
- **Service Discovery:** Implementace Dapr sidecars pro vzájemnou komunikaci služeb (mSD).
- **Centralized Auth:** Validace Azure Entra ID tokenů (v2.0) na úrovni Gateway/Middleware. RBAC role.
- Připravený KeyVault, ze kterého další units načítají secrets.
- Přístup do aplikace na základě členství v AAD Group.

## FS02: The Ingestor (Input)
Služba optimalizovaná na rychlý příjem dat.
- **Endpoint:** `POST /upload`.
- **Stream Upload:** Přímý stream do Blob Storage (S3/Azure Blob).
- **Validation:** Kontrola MIME types a magic numbers.
- **Trigger:** Po úspěšném uložení volá N8N Webhook s `file_id`.
- **Metadata:** Zápis do Metadata DB (User, Timestamp, Size).
- **Security Scan:** Implementace ICAP protokolu nebo sidecar (ClamAV) pro antivirovou kontrolu. Infikované soubory jsou odmítnuty před zpracováním.
- **Sanitizace:** Proces odstranění maker a externích linků z Office dokumentů před vstupem do Atomizerů.

## FS03: The Atomizers (Stateless Extractors)
Izolované kontejnery pro extrakci dat. Volány výhradně přes N8N.
- **PPTX Structure Atomizer:** `POST /extract/pptx` -> Vrací JSON strukturu (SlideID, Headers).
- **PPTX Content Atomizer:** `POST /extract/pptx/slide` -> Extrahuje texty a tabulky.
- **PPTX Slide Atomizer:** `POST /extract/pptx/slide` -> Vrací slide jako image (PNG) 800x600.
- **Excel Atomizer:** `POST /extract/excel` -> Vrací seznam listů.
- **Excel Table Atomizer:** `POST /extract/excel/sheet` -> Konverze listu na JSON.
- **PDF/OCR Atomizer:** Služba pro skenované dokumenty.
- **MetaTable Logic:** Algoritmus pro rekonstrukci tabulek na základě vizuálních oddělovačů.
- **CSV Data Atomizer:** Konverze CSV na JSON.
- **AI Gateway:** Integrace LiteLLM pro sémantickou analýzu textu.
- **Cleanup Policy:** Automatický Cron/Sidecar proces pro mazání dočasných souborů (PNG, CSV) z Blobu po X hodinách.

## FS04: The Orchestrator (N8N JSON workflows)
Business logika a workflow management.
- **Pipeline Workflow:** Webhook (New File) -> Get Metadata -> Router (Type) -> Call Atomizer.
- **Batch Processing:** Iterace přes jednotlivé slidy/listy (Node: Split In Batches).
- **Filter Logic:** Rozhodování, zda je element Tabulka (-> SQL Sink) nebo Text (-> Vector Sink).
- **Error Handling:** Retry logika a Circuit Breaker pro failed atomizers.
- **Idempotence:** Všechny procesy jsou navrženy pro opakované spuštění bez duplikace dat.
- **Dead Letter Queue (DLQ):** Fatálně chybná workflows odesílají ID souboru a stacktrace do `failed_jobs` fronty.

## FS05: The Sinks (Storage APIs) & Persistence
- **Table API (Java/Spring):** Ukládání strukturovaných dat do PostgreSQL.
- **Document API (Java/Spring):** Ukládání nestrukturovaného JSONu (PostgreSQL) a Vector Embeddings (pgVector).
- **Log API:** Audit trail zpracování souboru.
- **Databases:** PostgreSQL (Metadata & Relational), Redis (Cache), Blob Storage (Files).

## FS06: Analytics & Query (Read Model)
- **CQRS Read API:** Služba optimalizovaná pro rychlé čtení dat pro frontend.
- **Dashboard Aggregation:** Endpointy pro grafy a souhrny.
- **Full-text Search:** Integrace s ElasticSearch nebo PostgreSQL FTS.

## FS07: Admin UI/Backend
- **Role:** Admin, Editor, Viewer.
- **Holdingová struktura:** Hierarchická oprávnění (Holding vs. Společnost).
- **Config:** Superadmin UI pro správu secrets (propis do KeyVault).
- **API Keys:** Generování klíčů pro servisní účty.
- **Failed Jobs UI:** Rozhraní pro správu DLQ a manuální "Reprocess" po opravě chyby.

## FS08: Organizace souborů
- Soubory obsahují metadata o organizaci v holdingové struktuře.
- **Batches:** Seskupování souborů (např. tag "Q2/2025") pro hromadné reporty.
- **Security:** Implementace **Row-Level Security (RLS)** přímo v PostgreSQL pro tvrdé oddělení dat organizací.

## FS09: Frontend (React)
- **Auth:** MSAL Provider, Token Refresh, Axios Interceptors.
- **Upload Manager:** Drag&Drop, Progress bar, React Query invalidace.
- **Viewer:** Read-only zobrazení parsovaných dat.
- **Feedback Loop:** Implementace **WebSocket / SSE** pro real-time notifikaci o stavu zpracování (Processing... 50%).
- **N8N Trigger:** Spouštění úloh na pozadí, uživatel nevidí N8N UI.

## FS10: Excel Parsing Logic
- Excel je parsován per-sheet do JSONB.
- **Partial Success State:** Pokud 1 z 10 listů selže, soubor je uložen jako "Partially Completed" s chybovým logem pro vadný list. Celý soubor se nezahazuje.

## FS11: Dashboards & Reporting
- Definice dashboardů (Admin/Editor) vs. konzumace (Viewer).
- Data source: SQL dotazy nad JSON tabulkami.
- Možnost definovat Group By / Sort přímo v UI.

## FS12: API & AI Integration
- Přístup přes API Key.
- **MCP Server:** Integrace AI Agentů.
- **Security Constraint:** MCP Server dědí token uživatele (On-Behalf-Of flow). AI nesmí mít globální SELECT oprávnění.
- **Cost Control:** Quotas na tokeny pro uživatele/firmu (Rate Limiting).

## FS13: Notification Center & Alerts
- **In-app:** Notifikace o dokončení/chybě ("Report Q2 Ready").
- **E-mail:** SMTP/SendGrid alerty pro kritické chyby nebo dokončení velkých dávek.
- **Settings:** Granulární nastavení odběru (Opt-in/Opt-out) pro různé typy událostí.

## FS14: Data Reconciliation & Versioning
- **Versioning:** Úprava dat nepřepisuje originál, tvoří `v2`.
- **Diff Tool:** UI nástroj pro zobrazení rozdílů mezi verzemi (např. "Změna OPEX: +500k").

## FS15: Template & Schema Mapping Registry
- **UI pro mapování:** Editor definuje pravidla (např. "Sloupec 'Cena' -> `amount_czk`").
- **Learning:** Systém navrhuje mapování na základě historie.
- **Integrace:** Služba je volána z N8N před finálním uložením do DB.

## FS16: Audit & Compliance Log
- **Immutable Logs:** Záznamy "kdo-kdy-co viděl/editoval".
- **Read Access Log:** Logování zobrazení citlivých reportů.
- **AI Audit:** Logování promptů a odpovědí AI pro kontrolu halucinací a úniků dat.
- **Export:** Možnost exportu pro externí audit.

## FS99: DevOps & Observability
- **CI/CD:** Pipelines pro Linting, Testy, Build. Oddělené pipeline pro Native Image (Release) a JVM (Dev).
- **Monitoring:** OpenTelemetry tracing (E2E).
- **Logs:** Centralizované logování (ELK/Loki).
- **Metrics:** Prometheus + Grafana.
- **Local Dev Experience:** Konfigurace **Tilt** nebo **Skaffold** pro spuštění celé topologie lokálně (K8s/Docker) s hot-reloadem.