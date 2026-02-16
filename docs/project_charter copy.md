# Project Charter: PPTX Analyzer & Automation Platform
Popis projektu: Webová aplikace pro bezpečný upload, analýzu a vizualizaci dat z PPTX prezentací (se zaměřením na OPEX a finanční reporty), postavená na Reactu a Pythonu, zabezpečená přes Azure Entra ID.

**1. Technická Architektura**
Source /docs/STANDARDS.MD

Infrastruture
Microservices Java, Python, React
Identity: Azure Entra ID + local dev.

**2. Project goals**


**3. Project Scope**
# Features sets
## FS01 - Infrastructure & Core
API Gateway (Brána):
- Jediný vstupní bod pro Frontend (např. Traefik, Nginx nebo Azure API Management).
- Routing requestů na správné microservices (/api/auth -> Auth Service, /api/files -> File Service).
- SSL Terminace a Rate Limiting.
Centralized Authentication (Auth Service):
- Integrace s Azure Entra ID (OIDC/OAuth2).
- Validace tokenů na úrovni Gateway nebo Middleware.
- Správa rolí a oprávnění (RBAC).
Service Discovery & Configuration:
- Dynamické zjišťování běžících kontejnerů.
- Centralizovaná správa konfigurace (proměnné prostředí, secrets).


## FSXX "Ingestor" (Dumb & Fast)
Přijme soubor a dá vědět n8n.
API: POST /upload
Funkce: 
- Stream upload do S3/Blob
- generování unikátního ID
- volání n8n Webhooku.
- validace typů souborů (MIME type check, magic numbers)
- vytváření metadat v databázi (kdo nahrál, kdy, velikost).

## FSXX The "Atomizers" (Stateless Extractors)
Specializované Python skripty zabalené v Dockeru. Nedrží stav, nemají databázi. Dostanou soubor, vrátí JSON.
- PPTX Atomizer: POST /extract/pptx -> Vrací { slides: count, SLideID, SlideHeader }
- PPTX slide : POST /extract/pptx + slide ID -> Vrací { "slide": [ { "type": "text", "content": "..." }, { "type": "table", "data": [[...]] } ] }
- Excel Atomizer: POST /extract/excel -> Vrací JSON seynamu listů
- Excel table: POST /extract/excel + ListID -> Vrací JSON reprezentaci listu a tabulek na listu.
- PDF/OCR Atomizer: Pro skenované dokumenty.
- Generování náhledů (thumbnails).
- volání LiteLLM brány pro AI konverzi
- PPTX Slide MetaTable - služba ze vstupnáho PowerPoint slide zkonvertuje tabulku dle metadat tabulky (eg - tabuka Row1Col1 obsahuje text "XXX", potom další 4 texty oddelené tab/mezerou jsou názvy dalších sloupců. Další řádky dekoduj dle techto Headers (dle tab/mezer) )

## FSXX The Orchestrator (n8n)
Business logika bez kódu.
Workflow:
- Node: HTTP Request (volá Atomizer).
- Node: Split In Batches (iteruje přes jednotlivé elementy slidů).
- Node: Filter (rozděluje Text vs. Tabulka).
- Node: HTTP Request (volá Storage APIs).


## FSXX The "Sinks" (Storage APIs)
Koncová API, která se starají jen o uložení konkrétního typu dat.
- Table API: Umí uložit JSON strukturu tabulky do PostgreSQL a udělat nad ní relační vazby.
- Document API (Vector DB): Přijme text, udělá Embeddings (např. přes OpenAI) a uloží do Qdrant/Pinecone.
- Log API: Ukládá historii zpracování (audit trail).

## FS03 Data Persistence
Metadata DB (PostgreSQL): Relace mezi uživateli, projekty a soubory.
Document Store (MongoDB / CosmosDB): Ukládání parsovaných JSON dat z PPTX/Excelu (protože struktura slidů je variabilní).
Blob Storage: Fyzické soubory (.pptx, .xlsx).
Cache (Redis): Mezipaměť pro často dotazované reporty a session data.

## FS02 Analytics & Query Service (Read Model):
- Služba optimalizovaná pro čtení (CQRS pattern).
- Agregace dat z parserů do reportů.
- Poskytování dat pro Dashboardy (JSON pro grafy).
- Full-text search (ElasticSearch / PostgreSQL FTS).


## FS04 Frontend
App Shell (Host):
- Základní layout, navigace, Sidebar.
- MSAL Auth Provider (přihlášení/odhlášení).
- Global Error Boundary.
Feature Modules:
- Dashboard Module: Widgety, přehledy.
- Upload Manager: Komponenta pro Drag&Drop a progress bary.
- Viewer Module: Vizualizace parsovaných dat (ReadOnly pohled na PPTX).
- Admin Module: Správa uživatelů (pokud je třeba nad rámec Azure).

## FS05 Observability (Monitoring)
Distributed Tracing (OpenTelemetry): Aby jsi viděl cestu requestu: Frontend -> Gateway -> Ingestion -> Queue -> Worker.
Centralized Logging (ELK Stack / Loki): Všechny logy z kontejnerů na jednom místě.
Metrics & Alerting (Prometheus + Grafana):
- Sledování CPU/RAM kontejnerů.
-Počet chyb 500.
- Délka fronty ke zpracování.

## FS06 DevOps & CI/CD
Containerization: Dockerfile pro každou službu.
Orchestration: Docker Compose (pro lokální vývoj) / Kubernetes (K8s) nebo Azure Container Apps (pro produkci).
Pipelines:
- Linting & Testing při každém Push.
- Build & Push Docker Image.
- Auto-deploy na DEV/STAGE prostředí.



