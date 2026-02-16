# Project Charter: PPTX Analyzer & Automation Platform
Version: 2.0 (Microservices + N8N Orchestration)
Status: DRAFT / INITIATION
Architecture Pattern: Event-Driven Microservices with Low-Code Orchestration
Docs Reference: docs/project_standards.md, docs/dod_criteria.md

# Executive Summary
Cílem projektu je vybudovat robustní, škálovatelnou platformu pro bezpečný příjem, analýzu a vizualizaci dat z nestrukturovaných souborů (PPTX, Excel, PDF). Systém se zaměřuje na automatizaci extrakce finančních dat (OPEX reporty) a jejich převod do strukturované podoby pro BI a analytiku.

Architektura je postavena jako sada mikroslužeb (Polyglot: Java/Python), kde business logiku a tok dat řídí N8N orchestrátor. Frontend je oddělená React aplikace zabezpečená přes Azure Entra ID.

# High-Level Architecture
Systém se skládá ze čtyř vrstev:

Presentation Layer: React Frontend (SPA).
Ingestion Layer: API Gateway + Ingestor Service (dumb & fast).
Processing Layer: N8N Orchestrator řídící bezstavové "Atomizers" (Python/Java Workers).
Persistence Layer: "Sinks" (Storage APIs) a databáze (Postgres, Mongo, Vector DB, Blob).

# Technical Standards & Constraints
Dle docs/STANDARDS.md a docs/dod_criteria.md.
Backend Core: Java 21 + Spring Boot 3 + GraalVM Native Image.
Backend AI/Data: Python + FastAPI + Pydantic.
Frontend: React 18 + Vite + TypeScript + Tailwind CSS.
Communication: Dapr Sidecars (gRPC internal, REST external).
Auth: Azure Entra ID, Token v2, Scope api://<client_id>/access_as_user.
Documentation: Každý modul má README.md s Mermaid diagramem a test-result.md.
Testing: Unit testy pro logiku, Mocking externích služeb.

## Technology Stack Versions
| Komponenta | Technologie | Verze (Min) |
| --- | --- | --- |
| **Runtime** | Python | 3.11+ |
| **Runtime** | Node.js | 20 (LTS) |
| **Runtime** | Java (JDK) | 21 (LTS) |
| **Container** | Docker | Latest |
| **Orchestrator** | n8n | Latest Stable |
| **DB** | PostgreSQL | 16 |
| **Vector DB** | Qdrant | Latest |


## Ports Allocation
| Služba | Port Host | Port Container | Debug Port |
| --- | --- | --- | --- |
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
- **Web Framework:** Spring Boot Web.
- **DB Access:** Spring Data JPA / Hibernate.
- **Testing:** JUnit 5 + Mockito.



# Definition of Done (DoD) Checklist
Před uzavřením jakékoliv Feature (FS) musí být splněno:
- Code Quality: Linting (ESLint/Black/Checkstyle) bez chyb.
- Tests: Unit testy pokrývají novou logiku, Happy path i Edge cases.
- Security: Žádné hardcoded secrets, Auth tokeny validovány.
- Docs: Aktualizované README a Mermaid diagramy.
- Build: CI pipeline prošla (Build + Test).

# Known Risks
N8N Payload Size: Přenášení velkých JSONů (base64 obrázky) přes N8N může způsobit memory issues. Mitigace: Atomizers ukládají binárky do Blobu a vrací jen URL.
Auth Complexity: Synchronizace tokenů mezi Frontend -> Gateway -> N8N -> Backend. Mitigace: Strict Token Propagation policies.

# Scope & Feature Sets (FS)

## FS01: Infrastructure & Core
Focus: Základní kameny aplikace.
- API Gateway: (Traefik/APIM) Routing /api/auth, /api/upload, /api/query. SSL terminace, Rate limiting.
- Service Discovery: Implementace Dapr sidecars pro vzájemnou komunikaci služeb (mSD).
- Centralized Auth: Validace Azure Entra ID tokenů (v2.0) na úrovni Gateway/Middleware. RBAC role.

## FS02: The Ingestor (Input)
Služba optimalizovaná na rychlý příjem dat.
- Endpoint: POST /upload.
- Stream Upload: Přímý stream do Blob Storage (S3/Azure Blob).
- Validation: Kontrola MIME types a magic numbers.
- Trigger: Po úspěšném uložení volá N8N Webhook s file_id.
- Metadata: Zápis do Metadata DB (User, Timestamp, Size).

## FS03: The Atomizers (Stateless Extractors)
Sada izolovaných Python kontejnerů pro extrakci dat. Volány výhradně přes N8N.

- PPTX Structure Atomizer: POST /extract/pptx -> Vrací JSON strukturu prezentace (seznam SlideID, Headers).
- PPTX Content Atomizer: POST /extract/pptx/slide -> Extrahuje texty a tabulky z konkrétního slidu.
- PPTX Slide Atomizer: POST /extract/pptx/slide -> Vrací slide jako image (PNG) 800x600
- Excel Atomizer: POST /extract/excel -> Vrací seznam listů.
- Excel Table Atomizer: POST /extract/excel/sheet -> Konverze listu na JSON.
- PDF/OCR Atomizer: Služba pro skenované dokumenty.
- MetaTable Logic: Algoritmus pro rekonstrukci tabulek na základě vizuálních oddělovačů (tab/space) dle hlavičky.
- AI Gateway: Integrace LiteLLM pro sémantickou analýzu textu.

## FS04: The Orchestrator (N8N) (JSON workflow)
Business logika a workflow management.
- Pipeline Workflow: Webhook (New File) -> Get Metadata -> Router (Type) -> Call Atomizer.
- Batch Processing: Iterace přes jednotlivé slidy/listy (Node: Split In Batches).
- Filter Logic: Rozhodování, zda je element Tabulka (-> SQL Sink) nebo Text (-> Vector Sink).
- Error Handling: Retry logika pro failed atomizers.

## FS05: The Sinks (Storage APIs) & Persistence
Služby pro trvalé uložení zpracovaných dat.
- Table API (Java/Spring): Ukládání strukturovaných dat do PostgreSQL.
- Document API (Python/FastAPI): Ukládání nestrukturovaného JSONu (PostgreSQL) a Vector Embeddings (pgVector).
- Log API: Audit trail zpracování souboru.
- Databases:
    - PostgreSQL (Metadata & Relational Data).
    - Redis (Cache & Session).
Blob Storage (Physical Files).

## FS06: Analytics & Query (Read Model)
- CQRS Read API: Služba optimalizovaná pro rychlé čtení dat pro frontend.
- Dashboard Aggregation: Endpointy pro grafy a souhrny.
- Full-text Search: Integrace s ElasticSearch nebo PostgreSQL FTS.

## FS07: Frontend (React)
- Auth Integration: MSAL Provider, Login/Logout, Token Refresh (Axios Interceptor).
- Upload Manager: Drag&Drop zóna, Progress bar, automatický refresh seznamu po uploadu (React Query).
- Viewer Module: Read-only zobrazení parsovaných dat (slide by slide).
- Dashboard: Vizualizace finančních dat (grafy, tabulky).

## FS08: DevOps & Observability
- CI/CD: Pipelines pro Linting, Testy, Build Docker Image.
- Monitoring: OpenTelemetry tracing (Frontend -> Gateway -> N8N -> Atomizer).
- Logs: Centralizované logování (ELK/Loki).
- Metrics: Prometheus + Grafana (chybovost, délka fronty).