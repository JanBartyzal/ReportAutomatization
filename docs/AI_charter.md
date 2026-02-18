oto je Project Charter v3.1 (AI-Native Edition).

Každá sekce nyní obsahuje speciální blok 🤖 AI Implementation Spec. Ten obsahuje přesné názvy tabulek, knihoven, API kontrakty a logické kroky, které LLM (Claude/GPT) potřebuje k tomu, aby vygeneroval funkční kód bez halucinací.

Tento dokument můžete vzít a po částech (nebo jako celek do kontextového okna) dávat AI s příkazem: "Implementuj FS02 podle specifikace v sekci AI Implementation Spec."

Project Charter: PPTX Analyzer & Automation Platform
Version: 3.1 (AI-Native Implementation Spec)
Status: READY FOR CODING
Architecture: Event-Driven Microservices (Spring Boot + Python + N8N)
Docs Reference: docs/standards.md, docs/db_schema.md

1. Executive Summary & Core Tech Stack
Platforma pro automatizovaný příjem, bezpečnostní kontrolu, parsování (OCR/Extraction) a vizualizaci finančních dat z PPTX/Excel souborů.

🛠 Core Tech Context (Strict)
Java Backend: Java 21 LTS, Spring Boot 3.2+, Maven.

Python Backend: Python 3.11+, FastAPI, Pydantic v2.

Frontend: React 18, TypeScript, Vite, TanStack Query, Shadcn/UI.

Orchestration: N8N (Workflow automation).

Database: PostgreSQL 16 (Relational + JSONB + pgVector).

Async/Messaging: Redis (Cache/Queue) + Dapr (Sidecar pattern).

Storage: S3 Compatible (MinIO/Azure Blob).

2. Scope & Feature Sets (FS)
FS01: Infrastructure & Base
Focus: Kontejnerizace, Dapr, Gateway.

🤖 AI Implementation Spec
Docker Compose: Vytvořit docker-compose.yml definující služby: postgres, redis, n8n, traefik, minio.

Traefik Config:

EntryPoints: web (80), websecure (443).

Middleware: strip-prefix pro routing /api/v1/.

Database Init: Skript init.sql musí vytvořit DB analyzedb a zapnout extensions: CREATE EXTENSION IF NOT EXISTS vector;.

Java Base: Spring Boot projekt s spring-boot-starter-web, spring-boot-starter-data-jpa, lombok.

FS02: The Ingestor (Input Service)
Focus: Secure Upload, Validation, Event Trigger.

🤖 AI Implementation Spec
Service Name: ingest-service (Java)

Dependencies: org.apache.tika:tika-core:2.9.1 (MIME detection), io.minio:minio:8.5 (Storage).

Database Schema (Table: files_metadata):

id (UUID, PK), tenant_id (VARCHAR, Index), original_filename (VARCHAR), stored_filename (VARCHAR), bucket_name (VARCHAR), content_type (VARCHAR), size_bytes (BIGINT), status (ENUM: UPLOADED, SCANNING, SAFE, INFECTED), created_at (TIMESTAMP).

API Contract (POST /api/v1/ingest/upload):

Input: MultipartFile file, String tenantId.

Logic:

Detect MIME via Tika (reject if not application/vnd.openxmlformats-officedocument.* or pdf).

Stream to MinIO bucket raw-files/{tenantId}/{yyyy}/{MM}/{uuid}.ext.

Save metadata to DB (status=UPLOADED).

Call Async ClamAV Scan (Mock for now or implement ICAP).

Return 202 Accepted + { "fileId": "...", "status": "PENDING" }.

Dapr Output Binding: Publish event to topic file-uploaded: { "fileId": "...", "storagePath": "..." }.

FS03: The Atomizers (Stateless Workers)
Focus: Extrakce dat z binárek. Žádný stav, pouze Input -> Process -> Output JSON.

🤖 AI Implementation Spec
Service A: java-atomizer (Spring Boot)

Libs: org.apache.poi:poi-ooxml:5.2.5 (PPTX), org.apache.pdfbox:pdfbox:3.0.

Endpoint: POST /extract/pptx/structure

Input: { "fileUrl": "s3://...", "accessKey": "..." }

Logic: Download stream -> Parse XMLSlideShow -> Extract text/tables -> Return JSON.

JSON Output Structure:

JSON

{
  "slides": [
    { "index": 1, "title": "Opex Q1", "textBlocks": ["...", "..."], "hasTable": true }
  ]
}
Service B: python-atomizer (FastAPI)

Libs: pandas, openpyxl, python-pptx.

Endpoint: POST /extract/excel/sheet

Input: { "fileUrl": "...", "sheetName": "Sheet1" }

Logic: df = pd.read_excel(url, sheet_name=...) -> df.to_json(orient='records').

Constraint: Handle NaN values (replace with null).

FS04: The Orchestrator (N8N)
Focus: Workflow logic & Error handling.

🤖 AI Implementation Spec
Workflow Definition (Logical Steps):

Webhook Trigger: POST /webhook/process-file (payload: fileId).

HTTP Request: GET metadata from ingest-service.

Switch/Router: IF mime=pptx THEN goto PPTX Branch ELSE goto Excel Branch.

PPTX Branch:

HTTP POST to java-atomizer (Get Structure).

Loop (SplitInBatches): For each Slide -> HTTP POST java-atomizer (Render Image) -> HTTP POST sink-service (Save Image URL).

Excel Branch:

HTTP POST python-atomizer (Get Sheets).

Loop: Extract Data -> HTTP POST sink-service (Save Data Rows).

Error Trigger: On Node Error -> Write to DB table workflow_errors -> Send Email (Mock).

FS05: The Sinks (Persistence Layer)
Focus: Ukládání strukturovaných dat.

🤖 AI Implementation Spec
Service Name: sink-service (Java)

Database Schema (Table: financial_data):

id (UUID, PK), file_id (UUID, FK), sheet_name (VARCHAR), row_index (INT), data_json (JSONB - raw row data), mapped_data (JSONB - schema normalized), tenant_id (VARCHAR).

Database Schema (Table: parsed_slides):

id (UUID), file_id (UUID), slide_index (INT), image_url (VARCHAR), ocr_text (TEXT), embedding (vector(1536)).

API Contract:

POST /api/v1/sink/data: Accepts Batch of JSON rows. Uses JdbcTemplate for Batch Insert (high performance) instead of JPA one-by-one.

FS07 & FS08: Admin, Security & RLS
Focus: Bezpečnost dat a multi-tenancy.

🤖 AI Implementation Spec
PostgreSQL RLS (Row Level Security):

Constraint: Všechny tabulky musí mít sloupec tenant_id (nebo org_id).

SQL Policy:

SQL

ALTER TABLE financial_data ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_policy ON financial_data
USING (tenant_id = current_setting('app.current_tenant')::VARCHAR);
Java Interceptor:

Vytvořit TenantInterceptor implements HandlerInterceptor.

Získat Tenant-ID z JWT tokenu (Azure AD claim).

Nastavit do DB Session: entityManager.createNativeQuery("SET app.current_tenant = '" + tenantId + "'").executeUpdate();.

FS09: Frontend (React)
Focus: UX a Real-time feedback.

🤖 AI Implementation Spec
Stack: Vite, React Query, Zustand (Store).

Components:

DropzoneArea.tsx: Použití react-dropzone.

ProcessingStatus.tsx: Progress bar napojený na WebSocket.

API Client:

Axios instance s Interceptors.

Automatický refresh tokenu přes MSAL (Microsoft Authentication Library) acquireTokenSilent.

Hooks:

useUploadFile: Mutation hook pro POST upload.

useFileStatus(fileId): Polling nebo WS subscription pro status N8N workflow.

FS14: Versioning & Reconciliation
Focus: Audit změn.

🤖 AI Implementation Spec
Concept: "Soft Append". Nikdy nepřepisovat řádky v financial_data.

Schema Update: Přidat sloupec version (INT) a batch_id (UUID) do financial_data.

Logic:

Při nahrání "opravného" Excelu se zvýší verze (MAX(version) + 1).

View: latest_financial_data view, které dělá SELECT DISTINCT ON (row_key) ... ORDER BY version DESC.

FS99: DevEx & Local Environment
Focus: Jak to spustit.

🤖 AI Implementation Spec
File: Tiltfile (pro nástroj Tilt) nebo scaffold.yaml.

Definition:

Build Java images (Maven Jib).

Build Python images.

Apply Kubernetes manifests (k8s/*.yaml).

Port forwarding pro 8080 (Gateway) a 5678 (N8N).

Live Update: Nastavit sync pro React složku, aby se změny projevily hned bez rebuildování containeru.

📝 Návod: Jak tento dokument použít s AI
Když budete zadávat práci AI (Claude, ChatGPT), používejte tento vzor promptu:

"Jsi Senior Java Developer. Implementuj FS02: The Ingestor z přiloženého Project Charteru.

Kontext:

Použij Spring Boot 3 a Java 21.

Dodrž přesně specifikaci v sekci '🤖 AI Implementation Spec' (názvy tabulek, API endpointy).

Vytvoř JPA Entitu FileMetadata a Controller IngestController.

Přidej metodu pro streamování do MinIO pomocí MinioClient.

Ignoruj zatím auth (mockuj uživatele)."

Tímto způsobem dostanete kód, který bude přesně pasovat do celkové skládačky.