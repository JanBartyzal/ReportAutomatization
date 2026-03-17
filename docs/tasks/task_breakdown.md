# Task Breakdown – PPTX Analyzer & Automation Platform

**Version:** 1.0
**Based on:** `project_charter.md` v4.0, `roadmap.md`
**Date:** 2026-03-09

---

## Konvence

| Úroveň | Rozsah | Popis |
|---|---|---|
| **Epic** | 5–15 MD | Logický blok v rámci Feature Set, typicky mapuje na 1 Function ID |
| **Story** | 1–3 MD | Uživatelský nebo technický příběh s acceptance criteria |
| **Task** | 0.5–1 MD | Atomická implementační jednotka – jeden PR |

### Formát Story

```
**Jako** [role] **chci** [akce] **abych** [hodnota]
AC: acceptance criteria (checklist)
API: endpoint kontrakt
DB: migrace / schéma
Effort: odhad v MD
Závislosti: prerequisite stories
```

### Definition of Ready (DoR)

Story je ready pro vývoj, pokud:

- [ ] AC jsou jednoznačná a testovatelná
- [ ] API kontrakt je schválen (pokud jde o API)
- [ ] DB migrace je definována (pokud jde o data)
- [ ] Závislosti jsou dokončeny nebo mají mock
- [ ] UI wireframe existuje (pokud jde o FE)

### Definition of Done (DoD)

Story je done, pokud:

- [ ] Kód prošel code review
- [ ] Linting bez chyb (ESLint / Black / Checkstyle)
- [ ] Unit testy: happy path + min. 2 edge cases
- [ ] Integrační test (pokud jde o API)
- [ ] Žádné hardcoded secrets
- [ ] Docker image builduje bez chyb
- [ ] Dokumentace aktualizována (README, OpenAPI)

---

## Phase 1 – MVP Core

**Cíl:** End-to-end průchod jednoho PPTX souboru – upload, parsování, uložení, základní viewer.

**Scope:** FS01, FS02, FS03-PPTX, FS04, FS05, FS09-basic

**Celkový effort:** ~213 MD (s AI asistencí ~88 MD)

| Unit ID | Function ID | Effort (MD) | AI (MD) |
|---|---|---|---|
| router | router | 1 | 1 |
| engine-core | engine-core:auth | 30 | 9 |
| engine-ingestorESTOR | engine-ingestor | 25 | 8 |
| engine-ingestorESTOR | engine-ingestor:scanner | 5 | 4 |
| processor-atomizers | processor-atomizers:pptx | 35 | 16 |
| engine-data | engine-data:sink-tbl | 12 | 5 |
| engine-data | engine-data:sink-doc | 10 | 5 |
| engine-data | engine-data:sink-log | 5 | 2 |
| engine-orchestrator | engine-orchestrator | 45 | 18 |
| frontend | frontend | 45 | 20 |

---

### FS01 – Infrastructure & Core

---

#### Epic: API Gateway Setup (router)

**Unit ID:** router | **Effort:** 1 MD

---

##### Story: Nginx Routing & auth_request

**Jako** DevOps **chci** nakonfigurovat Nginx jako API Gateway **abych** zajistil centrální vstupní bod s autentizací pro všechny služby.

**AC:**

- [ ] Nginx routuje `/api/auth/*` → engine-core:auth (port 8081)
- [ ] Nginx routuje `/api/upload/*` → engine-ingestor (port 8082)
- [ ] Nginx routuje `/api/files/*` → engine-data:query (port 8100, prepared for P2)
- [ ] `auth_request` modul volá engine-core:auth na každém requestu (kromě `/health`)
- [ ] Rate limiting (ngx_http_limit_req_module):
  - API endpointy: 100 req/s per IP, burst 20
  - Auth + Upload endpointy: 10 req/s per IP, burst 20
  - Překročení → `429 Too Many Requests`
- [ ] CORS headers: whitelist `https://*.company.cz` + `localhost:3000` (dev)
- [ ] SSL: Azure Front Door terminuje SSL (WAF + SSL = holding standard), Nginx komunikuje přes HTTP v interní síti
- [ ] Health check endpoint `/health` vrací `200`

**Tasks:**

- [ ] Vytvořit `nginx.conf` – upstream blocks, server blocks, Host-based routing
- [ ] `auth_request` konfigurace → `http://ms-auth:8000/api/auth/verify`
- [ ] `auth_request_set` pro propagaci headerů (`X-User-Id`, `X-Org-Id`, `X-Roles`)
- [ ] Rate limiting: `limit_req_zone` pro API (100r/s) a Auth/Upload (10r/s) zóny
- [ ] CORS: `add_header Access-Control-Allow-Origin` s whitelist logikou
- [ ] Docker Compose service definice s health check
- [ ] Azure Front Door konfigurace docs (WAF rules, SSL cert, origin group → Nginx)

**Effort:** 1 MD

---

#### Epic: Auth Service (engine-core:auth)

**Unit ID:** engine-core | **Effort:** 30 MD

---

##### Story: Spring Boot Base Setup

**Jako** vývojář **chci** připravit base projekt engine-core:auth **abych** měl základní strukturu pro auth službu.

**AC:**

- [ ] Spring Boot 3.x, Java 21, Gradle/Maven
- [ ] Dockerfile s JDK 21 slim base image
- [ ] Actuator health endpoint `/actuator/health`
- [ ] Structured JSON logging (Logback + JSON encoder)
- [ ] OpenTelemetry agent konfigurace (prepared, ne aktivní)
- [ ] Docker Compose service definice (port 8081:8000, debug 5005)

**Tasks:**

- [ ] `spring init` s web, security, actuator, jpa, flyway
- [ ] Dockerfile (multi-stage build: build → runtime)
- [ ] `application.yml` s profily (local, dev, prod)
- [ ] Logback konfigurace – JSON format
- [ ] Docker Compose entry

**Effort:** 2 MD

---

##### Story: Azure Entra ID Token Validation

**Jako** API Gateway **chci** validovat JWT tokeny na každém requestu **abych** zajistil, že pouze autentizovaní uživatelé mají přístup.

**AC:**

- [ ] Endpoint `POST /api/auth/verify` validuje Azure Entra ID JWT v2 token
- [ ] Ověřuje: issuer, audience (`api://<client_id>/access_as_user`), expiration, signature (JWKS)
- [ ] Vrací `200` s headery `X-User-Id`, `X-Org-Id`, `X-Roles` pro platný token
- [ ] Vrací `401` pro chybějící nebo neplatný token
- [ ] Vrací `403` pro token bez požadované role/skupiny
- [ ] JWKS keys cachovány s TTL (5 min refresh)

**API:**

```
POST /api/auth/verify
Headers:
  Authorization: Bearer <JWT>

→ 200 OK
  X-User-Id: <azure_oid>
  X-Org-Id: <org_uuid>
  X-Roles: Admin,Editor

→ 401 Unauthorized
  { "error": "INVALID_TOKEN", "detail": "Token expired" }

→ 403 Forbidden
  { "error": "INSUFFICIENT_PERMISSIONS", "detail": "Required role: Editor" }
```

**Tasks:**

- [ ] Spring Security – OAuth2 Resource Server konfigurace
- [ ] JWKS endpoint: `https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys`
- [ ] Custom `JwtAuthenticationConverter` – extrakce claims (`oid`, `tid`, `roles`, `groups`)
- [ ] auth_request controller – vrací headers s user context (Nginx `auth_request_set`)
- [ ] JWKS cache s konfigurovaným TTL
- [ ] Unit testy: platný token, expirovaný, špatný issuer, chybějící role
- [ ] Integration test s mock JWKS endpoint (WireMock)
- [ ] Azure Entra ID Tenant ID - součástí appconfig.json
- [ ] App Registration Client ID a scope URI - v KeyVault pod Managed identity
- [ ] Mapování Azure AD Security Groups → interní role - všechny role jsou namapované na AAD Security Groups (Admin, Viewer, Editor, HoldingAdmin)
- [ ] Je vyžadováno členství v konkrétní AAD Security Group (Conditional Access)

**Effort:** 5 MD | **Závislosti:** Base Setup

---

##### Story: RBAC Engine

**Jako** systém **chci** vyhodnotit oprávnění uživatele na základě role a organizace **abych** zajistil správné přístupové kontroly.

**AC:**

- [ ] RBAC rozlišuje role: `Admin`, `Editor`, `Viewer`, `HoldingAdmin`
- [ ] Každé oprávnění je scoped na `org_id`
- [ ] HoldingAdmin vidí data ze všech dceřiných organizací v holdingu
- [ ] API endpoint `GET /api/auth/me` vrací aktuální user context (role, organizace)
- [ ] Oprávnění vyhodnoceno při auth_request (Nginx) i při přímém volání z jiných služeb

**API:**

```
GET /api/auth/me
Headers:
  Authorization: Bearer <JWT>

→ 200 OK
{
  "user_id": "uuid",
  "email": "user@company.cz",
  "organizations": [
    { "org_id": "uuid", "org_name": "Subsidiary A", "role": "Editor" }
  ],
  "active_org_id": "uuid"
}
```

**DB:**

```sql
-- V001__create_rbac_tables.sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    parent_org_id UUID REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE user_roles (
    id SERIAL PRIMARY KEY,
    user_id UUID NOT NULL,          -- Azure AD oid
    org_id UUID NOT NULL REFERENCES organizations(id),
    role_id INT NOT NULL REFERENCES roles(id),
    assigned_at TIMESTAMP DEFAULT NOW(),
    assigned_by UUID,
    UNIQUE(user_id, org_id, role_id)
);

-- Seed roles
INSERT INTO roles (name, description) VALUES
    ('HoldingAdmin', 'Full access across all subsidiary organizations'),
    ('Admin', 'Full access within own organization'),
    ('Editor', 'Upload files, edit data within own organization'),
    ('Viewer', 'Read-only access within own organization');
```

**Tasks:**

- [ ] Flyway migrace V001 – organizations, roles, user_roles
- [ ] `OrganizationRepository` + `UserRoleRepository` (Spring Data JPA)
- [ ] `RbacService.hasPermission(userId, orgId, requiredRole)` → boolean
- [ ] `RbacService.getUserContext(userId)` → UserContext DTO
- [ ] Hierarchická logika: HoldingAdmin → `parent_org_id IS NULL` vidí všechny child orgs
- [ ] `/api/auth/me` endpoint
- [ ] Seed data migrace: základní role + testovací organizace
- [ ] Unit testy: oprávnění per role, hierarchie, cross-tenant denial

**Permission matice (rozhodnuto):**

| Role | Upload | Edit | View (own org) | View (cross-org) | Approve | Admin |
|---|---|---|---|---|---|---|
| **Admin** | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ (v rámci Org) |
| **Editor** | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Viewer** | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| **HoldingAdmin** | ❌ | ❌ | ✅ | ✅ (Read-only) | ✅ | ✅ (cross-org) |

**Organizační hierarchie (rozhodnuto):** Fixní 3 úrovně: **Holding → Společnost → Divize/Nákladové středisko**

- [ ] Seed data pro dev: testovací organizace a uživatelé
- [ ] Editor vidí data jiných Editorů ve stejné organizaci a všechny data z nižších org. jednotek pod organizací

**Effort:** 8 MD | **Závislosti:** Token Validation

---

##### Story: KeyVault Integration

**Jako** služba **chci** načítat secrets z Azure KeyVault **abych** neměl hardcoded credentials.

**AC:**

- [ ] Služba čte secrets z Azure KeyVault při startu
- [ ] Přístup přes Managed Service Identity (bez credentials)
- [ ] Fallback na environment variables pro lokální dev (`application-local.yml`)
- [ ] Secrets: DB connection string, Redis URL, SMTP credentials, AI API keys

**Tasks:**

- [ ] Závislost `azure-spring-cloud-starter-keyvault-secrets`
- [ ] `application-prod.yml` – KeyVault property source konfigurace
- [ ] `application-local.yml` – env variable fallback
- [ ] Dokumentace: seznam secrets, naming konvence v KeyVault

- [ ] Azure KeyVault URL
- [ ] Kompletní seznam secrets a jejich naming konvence v KeyVault
- [ ] MSI setup – Managed Identity

**Effort:** 2 MD

---

##### Story: PostgreSQL Connection & Flyway Setup

**Jako** služba **chci** se připojit k PostgreSQL a spravovat schéma přes Flyway **abych** měl verzované DB migrace.

**AC:**

- [ ] Služba se připojuje k PostgreSQL 16 přes connection pool (HikariCP)
- [ ] Flyway spouští migrace automaticky při startu
- [ ] Migrace pojmenovány `V{NNN}__{popis}.sql`
- [ ] PostgreSQL Docker Compose service s persisted volume
- [ ] pgVector extension nainstalována

**Tasks:**

- [ ] Docker Compose: PostgreSQL 16 service s volume
- [ ] `application.yml`: datasource konfigurace (URL, user, password)
- [ ] Flyway konfigurace – migration location, baseline
- [ ] Init script: `CREATE EXTENSION IF NOT EXISTS vector;`
- [ ] Init script: vytvoření app user s omezenými právy (ne superuser)
- [ ] Connection pool tuning (HikariCP – max pool size, idle timeout)

- [ ] PostgreSQL credentials pro lokální dev v appseting.json
- [ ] Prod: Azure Database for PostgreSQL Flexible Server
- [ ] Max connection pool size per service - bude cca 50 uživatelů.

**Effort:** 3 MD

---

##### Story: Redis Connection Setup

**Jako** služba **chci** se připojit k Redis **abych** mohl cachovat tokeny a rate limit countery.

**AC:**

- [ ] Redis klient nakonfigurován (Lettuce / Spring Data Redis)
- [ ] Token cache s TTL (zkrácení JWKS lookupů)
- [ ] Rate limit counter storage
- [ ] Redis Docker Compose service

**Tasks:**

- [ ] Docker Compose: Redis service
- [ ] Spring Data Redis závislost a konfigurace
- [ ] `CacheService` pro token a JWKS caching
- [ ] TTL konfigurace per cache type

**Effort:** 2 MD

---

##### Story: Docker Compose Full Topology + Dapr Sidecars (P1)

**Jako** vývojář **chci** spustit celou P1 topologii jedním příkazem **abych** mohl lokálně vyvíjet a testovat.

**AC:**

- [ ] `docker-compose up` spustí: Nginx, engine-core:auth, engine-ingestor, engine-ingestor:scanner, engine-orchestrator, processor-atomizers:pptx, engine-data:sink-tbl, engine-data:sink-doc, engine-data:sink-log, frontend, PostgreSQL, Redis, Azurite
- [ ] Každá interní služba má **Dapr sidecar** kontejner s konfigurovaným `app-id` a `app-protocol`
- [ ] Interní služby (processor-atomizers, engine-data (sink modules), engine-orchestrator, engine-data:template) komunikují přes Dapr gRPC
- [ ] Edge služby (engine-core:auth, engine-ingestor, engine-data:query) vystavují REST přes Docker network
- [ ] `.env` soubor s konfiguratelnými porty a credentials
- [ ] Hot-reload pro FE (Vite HMR), Java (Spring DevTools), Python (gRPC server reload)

**Tasks:**

- [ ] `docker-compose.yml` se všemi P1 službami + Dapr sidecars
- [ ] Dapr component definitions: `components/pubsub.yaml` (Redis Streams), `components/statestore.yaml`
- [ ] Dapr sidecar konfigurace per služba: `app-id`, `app-protocol: grpc/http`, `app-port`
- [ ] `.env.example` s dokumentovanými proměnnými
- [ ] Shared Docker network konfigurace
- [ ] Volume mappings pro hot-reload (source code mounts)
- [ ] Startup ordering (depends_on + healthcheck) – Redis a PG musí být ready před Dapr sidecars
- [ ] Dapr dashboard (localhost:8080/dapr) pro debugging service invocations

- [ ] Pro lokální dev stačí Docker Compose
- [ ] Služby sdílejí jednu PostgreSQL instanci (různá schémata)
- [ ] Dapr placement service pro actor model (pokud bude potřeba v budoucnu)

**Effort:** 6 MD

---

### FS02 – File Ingestor

---

#### Epic: Streaming Upload & Security (engine-ingestor, engine-ingestor:scanner)

**Unit ID:** engine-ingestorESTOR | **Effort:** 30 MD (ING: 25, SCAN: 5)

---

##### Story: Spring Boot Base Setup (engine-ingestor)

**Jako** vývojář **chci** připravit base projekt engine-ingestor **abych** měl základ pro ingestor službu.

**AC:**

- [ ] Spring Boot 3.x, Java 21
- [ ] Dockerfile, Docker Compose entry (port 8082:8000, debug 5006)
- [ ] Actuator health, structured logging, OTEL prepared

**Tasks:**

- [ ] Projekt scaffolding (stejná šablona jako engine-core:auth)
- [ ] Dockerfile + Docker Compose entry
- [ ] `application.yml` s profily

**Effort:** 1 MD

---

##### Story: Streaming Upload Endpoint

**Jako** Editor **chci** nahrát soubor přes API **abych** ho mohl nechat zpracovat platformou.

**AC:**

- [ ] `POST /api/upload` přijímá `multipart/form-data`
- [ ] Soubor streamován přímo do Azure Blob Storage – nikdy celý v paměti serveru
- [ ] Podporované MIME types: `.pptx`, `.xlsx`, `.pdf`, `.csv`
- [ ] MIME validace kontroluje jak Content-Type header, tak magic bytes (binární hlavičku)
- [ ] Nepovolený typ vrací `415 Unsupported Media Type`
- [ ] Soubor nad limit vrací `413 Payload Too Large`
- [ ] Response obsahuje `file_id` pro tracking

**API:**

```
POST /api/upload
Content-Type: multipart/form-data
Body:
  file: binary
  upload_purpose: "PARSE" | "FORM_IMPORT" (default: "PARSE")

→ 200 OK
{
  "file_id": "uuid",
  "filename": "report_q2.pptx",
  "size_bytes": 15234567,
  "mime_type": "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  "status": "UPLOADED",
  "blob_url": "https://storage.blob.core.windows.net/files/..."
}

→ 415 Unsupported Media Type
{ "error": "UNSUPPORTED_TYPE", "allowed": [".pptx",".xlsx",".pdf",".csv"] }

→ 413 Payload Too Large
{ "error": "FILE_TOO_LARGE", "max_size_mb": 50 }

→ 422 Unprocessable Entity
{ "error": "INFECTED", "details": "Eicar-Test-Signature" }
```

**DB:**

```sql
-- V002__create_files_table.sql
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id),
    user_id UUID NOT NULL,
    filename VARCHAR(500) NOT NULL,
    size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    upload_purpose VARCHAR(20) NOT NULL DEFAULT 'PARSE',
    blob_url TEXT NOT NULL,
    scan_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processing_status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    uploaded_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

ALTER TABLE files ENABLE ROW LEVEL SECURITY;
CREATE POLICY files_org_isolation ON files
    USING (org_id = current_setting('app.current_org_id')::UUID);

CREATE INDEX idx_files_org_id ON files(org_id);
CREATE INDEX idx_files_processing_status ON files(processing_status);
```

**Tasks:**

- [ ] Upload controller s `StreamingMultipartResolver` (ne `@RequestParam MultipartFile`)
- [ ] MIME validace service – allowlist + Apache Tika magic bytes detection
- [ ] Azure Blob Storage client – `BlobClient.upload(InputStream, length)`
- [ ] Blob naming: `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}`
- [ ] Flyway migrace V002 – files tabulka s RLS
- [ ] `FileMetadataRepository` – zápis záznamu po uploadu
- [ ] `org_id` a `user_id` extrakce z auth_request headers (`X-Org-Id`, `X-User-Id`)
- [ ] File size limit konfigurace (`spring.servlet.multipart.max-file-size`)
- [ ] Unit testy: MIME validace (povolený typ, zakázaný typ, spoofed extension)
- [ ] Integration test: upload → Blob → DB záznam

**Rozhodnutí (potvrzeno):**

- **Max file size:** 50 MB (PPTX), 100 MB (PDF s OCR)
- **Blob Storage:** Azure Blob Storage (lokálně Azurite v Dockeru)
- **Blob naming:** `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}` – potvrzeno

- [ ] Storage quotas je jedna celková

**Effort:** 8 MD | **Závislosti:** engine-core:auth (auth_request), PostgreSQL (migrace), Azure Blob Storage

---

##### Story: ClamAV Security Scan (engine-ingestor:scanner)

**Jako** systém **chci** skenovat každý nahraný soubor antivirem **abych** zabránil nahrání infikovaných souborů.

**AC:**

- [ ] ClamAV sidecar/container běží vedle engine-ingestor
- [ ] Každý soubor skenován PŘED uložením do Blob Storage
- [ ] Infikovaný soubor → `422 { error: "INFECTED", details: "..." }`
- [ ] EICAR test virus správně detekován
- [ ] Scan timeout: max 30 s per soubor
- [ ] ClamAV nedostupný → soubor odmítnut (fail-closed princip)
- [ ] `scan_status` v DB aktualizován: `PENDING` → `CLEAN` / `INFECTED`

**Tasks:**

- [ ] Docker Compose: `clamav/clamav` container s persisted virus DB volume
- [ ] ClamAV klient v engine-ingestor – clamd TCP socket (port 3310)
- [ ] `ScanService.scan(InputStream)` → `ScanResult(clean/infected, detail)`
- [ ] Orchestrace: receive file → scan → if clean → upload to Blob
- [ ] Timeout handling (30 s) + fail-closed logika
- [ ] Update `scan_status` v files tabulce
- [ ] Test: EICAR virus, clean file, ClamAV timeout, ClamAV unavailable

**Rozhodnutí (potvrzeno):**

- **ClamAV protokol:** clamd TCP socket (port 3310) – jednodušší na scaling v Kubernetes/ACA
- [ ] ClamAV virus DB update strategie freshclam sidecar s cron

**Effort:** 5 MD | **Závislosti:** Upload Endpoint

---

##### Story: File Sanitization

**Jako** systém **chci** automaticky odstranit potenciálně nebezpečný obsah z Office dokumentů **abych** zabránil spuštění maker.

**AC:**

- [ ] VBA makra (`vbaProject.bin`) odstraněna z `.pptx` a `.xlsx`
- [ ] Externí OLE odkazy (`externalLinks/`) odstraněny
- [ ] Originál uložen do Blobu s prefixem `_raw/` (audit trail)
- [ ] Sanitizovaná verze uložena jako primární `blob_url`
- [ ] Sanitizace nezmění viditelný obsah dokumentu (texty, tabulky, obrázky)

**Tasks:**

- [ ] `SanitizationService` – rozbalení Office XML (ZIP archiv)
- [ ] Odstranění `vbaProject.bin` a `externalLinks/` entries
- [ ] Re-packování ZIP → uložení jako sanitizovaná verze
- [ ] Originál uložen do `_raw/{blob_path}` v Blob Storage
- [ ] Unit test: soubor s makrem → makro odstraněno, obsah zachován
- [ ] Unit test: soubor bez makra → beze změny

**Rozhodnutí (potvrzeno):**

- **Retention policy:** Raw soubory (`_raw/`) uchovávat 90 dní (pro audit), pak smazat. Sanitizované trvale.
- [ ] Další typy k odstranění - ActiveX, embedded executables, external data connections atd.

**Effort:** 4 MD | **Závislosti:** Upload Endpoint

---

##### Story: Orchestrator Event Trigger

**Jako** Ingestor **chci** po úspěšném uploadu notifikovat engine-orchestrator orchestrátor **abych** spustil processing pipeline.

**AC:**

- [ ] Po úspěšném uploadu + clean scan → event přes Dapr PubSub na engine-orchestrator
- [ ] Payload (Type-Safe DTO): `FileUploadedEvent { file_id, type, org_id, blob_url, user_id, filename, size_bytes }`
- [ ] Fire-and-forget: Ingestor nečeká na odpověď engine-orchestrator
- [ ] Event doručen do 1 s od uložení
- [ ] Selhání publikace → log warning + retry (Dapr built-in retry)
- [ ] `processing_status` aktualizován na `PROCESSING`

**API (odchozí – Dapr PubSub):**

```
Topic: file-uploaded
Payload (FileUploadedEvent):
{
  "file_id": "uuid",
  "type": "pptx",
  "org_id": "uuid",
  "blob_url": "https://...",
  "user_id": "uuid",
  "filename": "report_q2.pptx",
  "size_bytes": 15234567
}
```

**Tasks:**

- [ ] Dapr PubSub component konfigurace (Redis Streams nebo RabbitMQ)
- [ ] `FileUploadedEvent` DTO (Type-Safe Contract)
- [ ] Dapr SDK – publish event na topic `file-uploaded`
- [ ] Error handling: log warning, Dapr retry policy
- [ ] Update `processing_status` → `PROCESSING` v DB

**Effort:** 2 MD | **Závislosti:** Upload Endpoint, engine-orchestrator Event Subscriber

---

### FS03 – PPTX Atomizer

---

#### Epic: PPTX Extraction Engine (processor-atomizers:pptx)

**Unit ID:** processor-atomizers | **Effort:** 35 MD

---

##### Story: gRPC Server Base Setup (processor-atomizers:pptx)

**Jako** vývojář **chci** připravit base projekt PPTX Atomizeru **abych** měl základ pro extraction službu.

**AC:**

- [ ] Python 3.11+, gRPC server (grpcio), Pydantic v2
- [ ] Dockerfile s python:3.11-slim base image
- [ ] Dapr sidecar konfigurace: `app-protocol: grpc`, `app-port: 50051`
- [ ] Health check přes gRPC Health Check Protocol (`grpc.health.v1`)
- [ ] Structured JSON logging
- [ ] Docker Compose entry (gRPC port 50051, debug 5678)
- [ ] **Žádné REST endpointy** – služba komunikuje výhradně přes Dapr gRPC

**Tasks:**

- [ ] Projekt scaffolding: `pyproject.toml` (poetry/pip), gRPC server
- [ ] Dockerfile (multi-stage: dependencies → runtime)
- [ ] Dependencies: `python-pptx`, `Pillow`, `httpx`, `pydantic`, `grpcio`, `grpcio-tools`, `grpcio-health-checking`
- [ ] Protobuf kompilace: `protoc` → Python stubs z `packages/protos/atomizer_pptx.proto`
- [ ] LibreOffice Headless v Dockerfile: `apt-get install libreoffice-impress`
- [ ] Dapr component konfigurace (dapr.yaml): `app-id: ms-atm-pptx`, `app-protocol: grpc`
- [ ] Config: environment variables (BLOB_STORAGE_URL, etc.)
- [ ] Docker Compose entry s Dapr sidecar

**Effort:** 2 MD

---

##### Story: PPTX Structure Extraction

**Jako** engine-orchestrator Orchestrátor **chci** získat strukturu PPTX souboru **abych** věděl, kolik slidů soubor obsahuje a jak je zpracovat.

**AC:**

- [ ] `gRPC ExtractStructure(ExtractRequest)` přijímá `{ file_id, blob_url, org_id }`
- [ ] Atomizer si stahuje soubor z Blob Storage sám (přes `blob_url`)
- [ ] Vrací `PptxStructureResponse` se seznamem slidů s metadaty
- [ ] Chybný soubor vrací gRPC `INVALID_ARGUMENT` status s detailem, nikdy `INTERNAL`
- [ ] Služba nemá žádné REST endpointy – pouze gRPC přes Dapr sidecar

**gRPC API (definice v `packages/protos/atomizer_pptx.proto`):**

```protobuf
service PptxAtomizerService {
  rpc ExtractStructure (ExtractRequest) returns (PptxStructureResponse);
}

message ExtractRequest {
  string file_id = 1;
  string blob_url = 2;
  string org_id = 3;
}

message PptxStructureResponse {
  string file_id = 1;
  int32 slide_count = 2;
  repeated SlideMetadata slides = 3;
}

message SlideMetadata {
  int32 slide_id = 1;
  string title = 2;
  string layout = 3;
  bool has_tables = 4;
  bool has_text = 5;
  bool has_images = 6;
  bool has_notes = 7;
}
```

**Tasks:**

- [ ] gRPC server setup – `grpcio` + `grpcio-tools` v FastAPI aplikaci (nebo standalone gRPC server)
- [ ] Dapr sidecar konfigurace – `app-protocol: grpc`, `app-port: 50051`
- [ ] `BlobDownloader` – async stažení souboru z Blob URL (`httpx`)
- [ ] `PptxParser.get_structure(file_path)` – python-pptx, iterace přes slidy
- [ ] Extrakce metadat per slide: title (first text frame), layout name, detection tabulek/textu/obrázků
- [ ] Protobuf response mapping
- [ ] Error handling: `try/except` → gRPC `INVALID_ARGUMENT` s popisem chyby
- [ ] Dočasný soubor – stáhnout do `/tmp`, po zpracování smazat
- [ ] Unit testy: validní PPTX, prázdný PPTX, poškozený soubor

**Effort:** 5 MD | **Závislosti:** Blob Storage, Proto definitions

---

##### Story: Slide Content Extraction (Text + Tables)

**Jako** engine-orchestrator Orchestrátor **chci** extrahovat texty a tabulky z konkrétního slidu **abych** je mohl uložit do DB.

**AC:**

- [ ] `gRPC ExtractSlideContent(SlideRequest)` přijímá `{ file_id, blob_url, slide_id }`
- [ ] Extrahuje všechny textové rámce, tabulky a poznámky ze slidu
- [ ] Tabulky strukturovány jako `TableData { headers, rows }`
- [ ] Nikdy nevrací inline binary data – pouze strukturovaná gRPC response

**gRPC API (definice v `packages/protos/atomizer_pptx.proto`):**

```protobuf
// Součást PptxAtomizerService
rpc ExtractSlideContent (SlideRequest) returns (SlideContentResponse);

message SlideRequest {
  string file_id = 1;
  string blob_url = 2;
  int32 slide_id = 3;
}

message SlideContentResponse {
  string file_id = 1;
  int32 slide_id = 2;
  repeated TextShape texts = 3;
  repeated TableData tables = 4;
  string notes = 5;
}

message TextShape {
  string shape_name = 1;
  string content = 2;
}

message TableData {
  int32 table_id = 1;
  repeated string headers = 2;
  repeated TableRow rows = 3;
}

message TableRow {
  repeated string cells = 1;
}
```

**Tasks:**

- [ ] `PptxParser.extract_slide(file_path, slide_id)` – iterace přes shapes
- [ ] Text extraction: `shape.text_frame.paragraphs` → concatenated text per shape
- [ ] Table extraction: `shape.table` → headers (first row) + data rows
- [ ] Notes extraction: `slide.notes_slide.notes_text_frame`
- [ ] Handling: slide_id mimo rozsah → gRPC `INVALID_ARGUMENT`
- [ ] Unit testy: slide s textem, slide s tabulkou, slide s notes, prázdný slide

**Effort:** 6 MD | **Závislosti:** Structure Extraction

---

##### Story: Slide Image Rendering

**Jako** engine-orchestrator Orchestrátor **chci** vyrenderovat slide jako PNG obrázek **abych** mohl zobrazit náhled ve frontend vieweru.

**AC:**

- [ ] `gRPC RenderSlideImage(SlideRequest)` renderuje slide jako PNG 1280×720 (720p)
- [ ] PNG uložen do Blob Storage, vrací `SlideImageResponse { artifact_url }`
- [ ] Nikdy nevrací binary data inline v gRPC response – pouze URL reference

**gRPC API (definice v `packages/protos/atomizer_pptx.proto`):**

```protobuf
// Součást PptxAtomizerService
rpc RenderSlideImage (SlideRequest) returns (SlideImageResponse);

message SlideImageResponse {
  string file_id = 1;
  int32 slide_id = 2;
  string artifact_url = 3;
  int32 width = 4;
  int32 height = 5;
}
```

**Tasks:**

- [ ] LibreOffice Headless rendering: `libreoffice --headless --convert-to png --outdir /tmp/{file_id}/ input.pptx`
- [ ] Rozlišení: 1280×720 (720p, ~200KB per slide) – dobrý kompromis mezi čitelností a velikostí
- [ ] LibreOffice jako sidecar container nebo součást processor-atomizers:pptx Docker image
- [ ] Upload PNG do Blob Storage (`artifacts/{file_id}/slide_{n}.png`)
- [ ] Vrácení `artifact_url` v gRPC response
- [ ] Cleanup: smazání lokálního PNG po uploadu
- [ ] Unit test: renderování, ověření rozměrů PNG (1280×720)

**Rozhodnutí (potvrzeno):**

- **Rendering engine:** LibreOffice Headless (`--convert-to png`) – python-pptx neumí věrně vykreslit SmartArty a grafy
- **Rozlišení:** 1280×720 (720p), ~200KB per slide
- **Artifact retention:** processor-atomizers:cleanup maže artefakty po 24h (konfigurovatelné)

**Effort:** 6 MD | **Závislosti:** Structure Extraction, Blob Storage

---

##### Story: MetaTable Logic

**Jako** systém **chci** rekonstruovat tabulkovou strukturu z nestrukturovaného textu na slidu **abych** zachytil data i z prezentací, které nepoužívají nativní PPTX tabulky.

**AC:**

- [ ] Algoritmus detekuje tabulkovou strukturu v textových rámcích (tabulátory, zarovnání)
- [ ] Porovnání s hlavičkovým řádkem pro odvození sloupců
- [ ] Výstup ve stejném formátu jako nativní tabulka (`headers` + `rows`)
- [ ] Pokud algoritmus nedokáže detekovat tabulku → vrací text as-is (graceful degradation)

**Tasks:**

- [ ] Heuristický parser: detekce oddělovačů (tab, multiple spaces, pipes)
- [ ] Header detection: první řádek s konzistentními oddělovači
- [ ] Row parsing: split na základě detekovaného vzoru
- [ ] Confidence score: pokud < 0.85 → vrátit jako plain text a označit příznakem `low_confidence`
- [ ] Unit testy: tab-separated text, space-aligned text, mixed content, low confidence scenario

**Rozhodnutí (potvrzeno):**

- **Confidence threshold:** > 0.85. Pokud nižší, uložit jako plain text s příznakem `low_confidence`
- [ ] MetaTable logic se volá jen na request, bude osučástí flow (flag `detect_meta_tables: true`)

**Effort:** 8 MD | **Závislosti:** Slide Content Extraction

---

##### Story: Error Handling & Resilience

**Jako** Atomizer **chci** robustně zpracovat chybné soubory **abych** nikdy nevrátil gRPC `INTERNAL` a vždy dal smysluplnou chybovou hlášku.

**AC:**

- [ ] Chybný/poškozený PPTX → gRPC `INVALID_ARGUMENT` s detailem `"INVALID_PPTX: ..."`
- [ ] Blob URL nedostupný → gRPC `UNAVAILABLE` s detailem `"BLOB_UNAVAILABLE: ..."`
- [ ] Timeout na stažení souboru → gRPC `DEADLINE_EXCEEDED` s detailem `"DOWNLOAD_TIMEOUT"`
- [ ] Memory limit: soubory > N MB odmítnuty → gRPC `RESOURCE_EXHAUSTED`
- [ ] Cleanup: dočasné soubory v `/tmp` vždy smazány (i při chybě)

**Tasks:**

- [ ] gRPC interceptor pro globální exception handling
- [ ] Mapování Python exceptions → gRPC status codes (`INVALID_ARGUMENT`, `UNAVAILABLE`, `DEADLINE_EXCEEDED`)
- [ ] Download timeout konfigurace (env variable)
- [ ] File size check před parsováním
- [ ] `finally` blok pro cleanup dočasných souborů
- [ ] Unit testy: každý error scenario
- [ ] Max file size pro Atomizer - stejny jako upload limit
- [ ] Download timeout 30 sec.

**Effort:** 3 MD

---

### FS04 – Custom Orchestrator (engine-orchestrator)

---

#### Epic: Workflow Engine & Processing Pipeline (engine-orchestrator)

**Unit ID:** engine-orchestrator | **Effort:** 45 MD

---

##### Story: Workflow Engine Setup

**Jako** DevOps **chci** nasadit engine-orchestrator jako custom orchestrátor **abych** měl workflow engine pro orchestraci processing pipeline.

**AC:**

- [ ] engine-orchestrator běží v Docker kontejneru (port 8095:8080, debug 5010)
- [ ] Workflow engine: Spring State Machine (finální rozhodnutí CTO)
- [ ] Dapr PubSub subscriber na topic `file-uploaded`
- [ ] gRPC server pro příjem příkazů, gRPC client pro volání Atomizerů a Sinků
- [ ] Workflow definice jako JSON soubory v `/resources/workflows/` (verzované v Gitu)
- [ ] Redis pro stav běžících flows (nízká latence)
- [ ] PostgreSQL pro stav paused/waiting flows (persistence)
- [ ] Actuator health endpoint, structured JSON logging, OTEL prepared

**Tasks:**

- [ ] Spring Boot 3.x, Java 21, Gradle projekt scaffolding
- [ ] Závislost: Spring State Machine
- [ ] gRPC server konfigurace (port 9090) + Dapr sidecar: `app-protocol: grpc`, `app-id: ms-orch`
- [ ] Dapr gRPC client pro volání služeb přes Dapr service invocation (`dapr-app-id` header):
  - processor-atomizers:pptx (`ms-atm-pptx`), engine-data:sink-tbl (`ms-sink-tbl`), engine-data:sink-doc (`ms-sink-doc`), engine-data:sink-log (`ms-sink-log`)
- [ ] Dapr PubSub subscriber na topic `file-uploaded`
- [ ] Redis client (Lettuce) pro workflow state management
- [ ] JSON workflow definition loader: `WorkflowDefinitionRegistry`
- [ ] Dockerfile (multi-stage build), Docker Compose entry s Dapr sidecar
- [ ] `application.yml` s profily (local, dev, prod)

- [ ] Spring State Machine – finální rozhodnutí CTO
- [ ] Redis: sdílená instance s engine-core:auth
- [ ] Všechna interní volání přes Dapr gRPC service invocation (ne přímé gRPC) – zajišťuje service discovery, mTLS, observability

**Effort:** 8 MD

---

##### Story: Type-Safe Contracts (DTOs & Interfaces)

**Jako** vývojář **chci** mít Type-Safe kontrakty pro veškerou komunikaci mezi engine-orchestrator a Atomizery/Sinky **abych** měl compile-time garance a jasné API.

**AC:**

- [ ] Java interfaces pro každý Atomizer a Sink (gRPC service definitions v `.proto` souborech)
- [ ] DTOs pro vstupy a výstupy každého workflow stepu (Protobuf messages)
- [ ] Žádné volné `Map<String, Object>` nebo raw JSON – vše typované
- [ ] Sdílený modul `orchestrator-contracts` s `.proto` soubory
- [ ] Automaticky generované Java třídy z `.proto` souborů (protoc plugin)

**Proto definice (příklad):**

```protobuf
// atomizer_pptx.proto
service PptxAtomizerService {
  rpc ExtractStructure (ExtractRequest) returns (PptxStructureResponse);
  rpc ExtractSlideContent (SlideRequest) returns (SlideContentResponse);
  rpc RenderSlideImage (SlideRequest) returns (SlideImageResponse);
}

message ExtractRequest {
  string file_id = 1;
  string blob_url = 2;
  string org_id = 3;
}

message PptxStructureResponse {
  string file_id = 1;
  int32 slide_count = 2;
  repeated SlideMetadata slides = 3;
}

message SlideMetadata {
  int32 slide_id = 1;
  string title = 2;
  string layout = 3;
  bool has_tables = 4;
  bool has_text = 5;
  bool has_images = 6;
}

// sink_table.proto
service TableSinkService {
  rpc BulkInsert (BulkInsertRequest) returns (BulkInsertResponse);
}

// sink_document.proto
service DocumentSinkService {
  rpc StoreDocument (StoreDocumentRequest) returns (StoreDocumentResponse);
}
```

**Tasks:**

- [ ] Vytvořit `orchestrator-contracts` modul (Gradle subproject)
- [ ] `.proto` soubory pro: PptxAtomizerService, ExcelAtomizerService, PdfAtomizerService, CsvAtomizerService
- [ ] `.proto` soubory pro: TableSinkService, DocumentSinkService, LogSinkService
- [ ] `.proto` soubory pro: TemplateMappingService
- [ ] Protoc Gradle plugin pro generování Java tříd
- [ ] Sdílení contracts modulu jako závislost do processor-atomizers a engine-data (sink modules)
- [ ] Exception types v Protobuf: `ParsingException`, `StorageException`, `VirusDetectedException`

**Effort:** 5 MD | **Závislosti:** Workflow Engine Setup

---

##### Story: PPTX Processing Workflow (Saga)

**Jako** engine-orchestrator **chci** zpracovat nově nahraný PPTX soubor end-to-end **abych** extrahoval data a uložil je do DB.

**Workflow (JSON definition):**

```json
{
  "name": "pptx-processing-pipeline",
  "version": 1,
  "trigger": { "type": "event", "topic": "file-uploaded", "filter": "type == 'pptx'" },
  "steps": [
    { "id": "extract-structure", "service": "PptxAtomizerService", "method": "ExtractStructure", "retry": { "maxAttempts": 3, "backoff": [1, 5, 30] } },
    { "id": "process-slides", "type": "parallel-for-each", "source": "extract-structure.slides", "maxParallel": 50, "steps": [
      { "id": "extract-content", "service": "PptxAtomizerService", "method": "ExtractSlideContent" },
      { "id": "render-image", "service": "PptxAtomizerService", "method": "RenderSlideImage" },
      { "id": "store-table", "condition": "extract-content.has_tables", "service": "TableSinkService", "method": "BulkInsert" },
      { "id": "store-document", "condition": "extract-content.has_text", "service": "DocumentSinkService", "method": "StoreDocument" },
      { "id": "log-step", "service": "LogSinkService", "method": "AppendLog" }
    ]},
    { "id": "update-status", "type": "internal", "action": "updateFileStatus", "status": "DONE" }
  ],
  "compensations": [
    { "step": "store-table", "action": "TableSinkService.DeleteByFileId" },
    { "step": "store-document", "action": "DocumentSinkService.DeleteByFileId" }
  ]
}
```

**AC:**

- [ ] Upload nového PPTX automaticky spustí workflow bez manuálního zásahu (Dapr PubSub event)
- [ ] Každý slide zpracován nezávisle (partial success) – 20-50 paralelních slide extractions
- [ ] Tabulky → engine-data:sink-tbl (gRPC), texty → engine-data:sink-doc (gRPC), logy → engine-data:sink-log (gRPC)
- [ ] Processing status aktualizován v DB: `PROCESSING` → `DONE` / `PARTIAL` / `FAILED`
- [ ] Saga Pattern: při selhání se spouští compensating actions (rollback uložených dat)
- [ ] Stav workflow v Redis (running), přesun do PostgreSQL při pause/wait

**Tasks:**

- [ ] JSON workflow definition: `pptx-processing-pipeline.json`
- [ ] `WorkflowEngine.execute(workflowDefinition, context)` – hlavní orchestrační loop
- [ ] `ParallelForEachStep` – paralelní zpracování slidů s konfigurovaným `maxParallel`
- [ ] Dapr gRPC service invocation: processor-atomizers:pptx (`ms-atm-pptx`) → `ExtractStructure`, `ExtractSlideContent`, `RenderSlideImage`
- [ ] Condition evaluator: `has_tables` → Sink TBL, `has_text` → Sink DOC
- [ ] Dapr gRPC service invocation: engine-data:sink-tbl (`ms-sink-tbl`) → `BulkInsert`, engine-data:sink-doc (`ms-sink-doc`) → `StoreDocument`, engine-data:sink-log (`ms-sink-log`) → `AppendLog`
- [ ] Status update: DB update `processing_status` → `DONE` / `PARTIAL` / `FAILED`
- [ ] Saga compensations: Dapr gRPC → `DeleteByFileId` na Sinks při fatálním selhání
- [ ] Redis state persistence: workflow context uložen po každém stepu

**Effort:** 12 MD | **Závislosti:** Type-Safe Contracts, processor-atomizers:pptx, engine-data (sink modules)

---

##### Story: Error Handling & Exponential Backoff

**Jako** engine-orchestrator **chci** správně zpracovat selhání Atomizerů a Sinků **abych** neztratil data a umožnil manuální reprocessing.

**AC:**

- [ ] Exponential backoff: 3 retry (1s, 5s, 30s), pak záznam do `failed_jobs`
- [ ] Specifické exception types: `ParsingException`, `StorageException`, `VirusDetectedException`
- [ ] `ParsingException` → retry (soubor mohl být dočasně nedostupný)
- [ ] `StorageException` → retry (DB/Blob mohly být dočasně nedostupné)
- [ ] `VirusDetectedException` → NO retry, okamžitý záznam do `failed_jobs`
- [ ] Fatální selhání → Saga compensating actions + záznam v `failed_jobs` tabulce
- [ ] Admin může zobrazit failed_jobs a spustit reprocessing

**DB:**

```sql
-- V003__create_failed_jobs_table.sql
CREATE TABLE failed_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id),
    workflow_name VARCHAR(255) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    exception_type VARCHAR(100) NOT NULL,  -- ParsingException, StorageException, etc.
    error_message TEXT,
    error_stacktrace TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    last_retry_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'FAILED',  -- FAILED, REPROCESSING, RESOLVED
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);
```

**Tasks:**

- [ ] `RetryPolicy` třída: exponential backoff (1s, 5s, 30s) s konfigurací per exception type
- [ ] Exception hierarchy: `OrchestratorException` → `ParsingException`, `StorageException`, `VirusDetectedException`
- [ ] gRPC error mapping: gRPC status codes → interní exception types
- [ ] `FailedJobService`: zápis do `failed_jobs` tabulky po vyčerpání retries
- [ ] Flyway migrace V003 – failed_jobs tabulka (rozšířená o `exception_type`, `max_retries`, `last_retry_at`)
- [ ] Reprocessing endpoint: `POST /api/orchestrator/reprocess/{file_id}` → re-trigger workflow
- [ ] Saga compensation executor: volání compensating actions při fatálním selhání

**Effort:** 7 MD | **Závislosti:** PPTX Processing Workflow

---

##### Story: Idempotence (Redis-based)

**Jako** systém **chci** garantovat idempotenci zpracování **abych** neměl duplicitní záznamy v DB při opětovném spuštění workflow.

**AC:**

- [ ] Opětovné spuštění workflow pro stejné `file_id` nevytvoří duplicitní záznamy
- [ ] Idempotence klíč: `file_id + step_hash` uložen v Redis s TTL (24h)
- [ ] Before each step: check Redis → pokud klíč existuje → skip
- [ ] Upsert logika v Sinks: `ON CONFLICT DO UPDATE` jako fallback

**Tasks:**

- [ ] `IdempotenceService`: Redis-based check `SETNX file_id:step_hash` s TTL
- [ ] `step_hash` = hash z `(workflow_name, step_id, file_id, slide_id)`
- [ ] Integrace do `WorkflowEngine` – before/after each step hooks
- [ ] Upsert logika v engine-data:sink-tbl a engine-data:sink-doc (ON CONFLICT DO UPDATE) jako safety net
- [ ] Processing status check: pokud `DONE` → skip (nebo `force` flag pro reprocessing)

**Effort:** 4 MD | **Závislosti:** PPTX Processing Workflow, Redis Setup

---

##### Story: Async Worker Layer

**Jako** engine-orchestrator **chci** asynchronně distribuovat práci přes message queue **abych** mohl škálovat zpracování na 20-50 paralelních slide extractions.

**AC:**

- [ ] Dapr Pub/Sub (nebo RabbitMQ / Azure Service Bus) pro distribuci work items
- [ ] Worker pool: konfigurovatelný počet paralelních workerů (default: 20, max: 50)
- [ ] Backpressure: pokud queue přeteče, nové soubory čekají (ne reject)
- [ ] Monitoring: Prometheus metriky pro queue depth, processing time, error rate

**Tasks:**

- [ ] Dapr PubSub component konfigurace pro worker topics (`slide-extract`, `slide-store`)
- [ ] `WorkerPool` třída: thread pool s konfigurovaným počtem workerů
- [ ] `SlideExtractionWorker`: odebírá z `slide-extract` topic, volá Atomizer via gRPC
- [ ] `SlideStorageWorker`: odebírá z `slide-store` topic, volá Sinks via gRPC
- [ ] Backpressure: `maxConcurrency` na Dapr subscription
- [ ] Prometheus metriky: `orch_queue_depth`, `orch_processing_duration_seconds`, `orch_error_total`

**Effort:** 5 MD | **Závislosti:** Workflow Engine Setup

---

##### Story: File Type Router

**Jako** engine-orchestrator **chci** routovat soubory dle typu na správný workflow definition **abych** v P2 mohl přidat Excel, PDF a CSV parsování.

**AC:**

- [ ] Router: `type == "pptx"` → `pptx-processing-pipeline.json`
- [ ] Ostatní typy: `type == "xlsx|pdf|csv"` → log "Not implemented yet" + status `UNSUPPORTED`
- [ ] Neznámý typ → error + záznam do failed_jobs
- [ ] Router logika v `WorkflowDefinitionRegistry` – mapování file type → workflow JSON

**Tasks:**

- [ ] `WorkflowDefinitionRegistry.getWorkflow(fileType)` → WorkflowDefinition
- [ ] Placeholder JSON definitions pro xlsx, pdf, csv (prázdné, jen log step)
- [ ] Unknown type handling: `UnsupportedFileTypeException` → failed_jobs

**Effort:** 2 MD | **Závislosti:** Workflow Engine Setup

---

##### Story: Workflow State Persistence

**Jako** systém **chci** persistovat stav workflow **abych** mohl obnovit zpracování po restartu služby.

**AC:**

- [ ] Running workflows: stav uložen v Redis (po každém stepu)
- [ ] Paused/waiting workflows: stav přesunut z Redis do PostgreSQL
- [ ] Po restartu engine-orchestrator: automatická obnova running workflows z Redis
- [ ] PostgreSQL tabulka `workflow_state` pro long-running flows

**DB:**

```sql
-- V003b__create_workflow_state.sql
CREATE TABLE workflow_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id),
    workflow_name VARCHAR(255) NOT NULL,
    current_step VARCHAR(255),
    state_data JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PAUSED',  -- PAUSED, WAITING, COMPLETED
    paused_at TIMESTAMP DEFAULT NOW(),
    resumed_at TIMESTAMP,
    UNIQUE(file_id, workflow_name)
);
```

**Tasks:**

- [ ] Redis state serializer: `WorkflowContext` → Redis hash
- [ ] `WorkflowStateRepository` (Spring Data JPA) pro PostgreSQL persistence
- [ ] State migration: Redis → PostgreSQL pro paused flows (po timeout)
- [ ] Startup recovery: scan Redis pro incomplete workflows → resume
- [ ] TTL na Redis state: 1h (pokud workflow nedoběhne, přesun do PG)

**Effort:** 5 MD | **Závislosti:** Workflow Engine Setup, Redis Setup

---

### FS05 – Sinks (Persistence)

---

#### Epic: Table API (engine-data:sink-tbl)

**Unit ID:** engine-data | **Effort:** 12 MD

---

##### Story: Spring Boot + gRPC Base Setup (engine-data:sink-tbl)

**AC:**

- [ ] Spring Boot 3.x, Java 21, gRPC server (grpc-spring-boot-starter)
- [ ] Dockerfile, Docker Compose entry (gRPC port 9090, debug 5005)
- [ ] Dapr sidecar konfigurace: `app-protocol: grpc`, `app-id: ms-sink-tbl`
- [ ] Actuator health, structured logging, OTEL prepared
- [ ] **Žádné REST endpointy** – služba komunikuje výhradně přes Dapr gRPC

**Effort:** 2 MD

---

##### Story: gRPC Bulk Insert

**Jako** engine-orchestrator Orchestrátor **chci** uložit strukturovaná tabulková data do PostgreSQL **abych** je měl k dispozici pro dashboardy a query.

**AC:**

- [ ] `gRPC BulkInsert(BulkInsertRequest)` přijímá strukturovaná tabulková data přes Dapr gRPC
- [ ] Data uložena jako JSONB v PostgreSQL
- [ ] RLS policy: uživatel vidí pouze záznamy svého `org_id`
- [ ] Upsert: duplicitní `file_id + slide_id + table_id` přepíše existující záznam
- [ ] Bulk insert: až 100 řádků v jednom requestu
- [ ] **Žádné REST endpointy** – služba komunikuje výhradně přes Dapr gRPC. Čtení dat přes engine-data:query (CQRS).

**gRPC API (definice v `packages/protos/sink_table.proto`):**

```protobuf
service TableSinkService {
  rpc BulkInsert (BulkInsertRequest) returns (BulkInsertResponse);
  rpc DeleteByFileId (DeleteByFileIdRequest) returns (DeleteResponse);
}

message BulkInsertRequest {
  string org_id = 1;
  string batch_id = 2;
  string file_id = 3;
  int32 slide_id = 4;
  int32 table_id = 5;
  string source_type = 6;  // FILE | FORM
  repeated string headers = 7;
  repeated TableRow rows = 8;
  map<string, string> metadata = 9;
}

message BulkInsertResponse {
  string record_id = 1;
  int32 rows_inserted = 2;
}

message DeleteByFileIdRequest {
  string file_id = 1;
}

message DeleteResponse {
  int32 rows_deleted = 1;
}
```

**DB:**

```sql
-- V004__create_table_data.sql
CREATE TABLE table_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id),
    batch_id UUID,
    file_id UUID REFERENCES files(id),
    slide_id INT,
    table_id INT,
    source_type VARCHAR(20) NOT NULL DEFAULT 'FILE',  -- FILE | FORM
    headers JSONB NOT NULL,
    rows JSONB NOT NULL,
    metadata JSONB,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(file_id, slide_id, table_id, version)
);

ALTER TABLE table_data ENABLE ROW LEVEL SECURITY;
CREATE POLICY table_data_org_isolation ON table_data
    USING (org_id = current_setting('app.current_org_id')::UUID);

CREATE INDEX idx_table_data_org_batch ON table_data(org_id, batch_id);
CREATE INDEX idx_table_data_file ON table_data(file_id);
```

**Tasks:**

- [ ] gRPC server setup v Spring Boot (grpc-spring-boot-starter)
- [ ] Dapr sidecar konfigurace: `app-protocol: grpc`, `app-port: 9090`
- [ ] Protobuf kompilace: `protoc` → Java stubs z `packages/protos/sink_table.proto`
- [ ] Flyway migrace V004 – table_data tabulka s RLS
- [ ] gRPC service implementace `TableSinkServiceImpl` (ne REST controller)
- [ ] `TableDataRepository` (Spring Data JPA)
- [ ] Upsert logika (ON CONFLICT DO UPDATE)
- [ ] RLS enforcement: `SET app.current_org_id = ?` před každým dotazem
- [ ] `DeleteByFileId` gRPC method – compensating action pro Saga rollback
- [ ] Request validation (Protobuf + custom validátor: headers a rows povinné)
- [ ] Unit testy: insert, upsert, RLS isolation
- [ ] Integration test: dva org_id nesmí vidět data druhého

**Effort:** 8 MD | **Závislosti:** PostgreSQL, Flyway, Proto definitions

---

##### Story: RLS Integration Test

**Jako** bezpečnostní tým **chci** ověřit, že RLS policy funguje správně **abych** garantoval, že nedojde ke cross-tenant data leaku.

**AC:**

- [ ] Test s dvěma organizacemi: org_A a org_B
- [ ] org_A vloží data → org_B je nevidí (SELECT vrací 0 řádků)
- [ ] org_B vloží data → org_A je nevidí
- [ ] HoldingAdmin (parent org) vidí data obou
- [ ] Test pokrývá: SELECT, UPDATE, DELETE isolation

**Tasks:**

- [ ] Testcontainers setup pro PostgreSQL
- [ ] Seed: 2 organizace (org_A, org_B) + holding parent
- [ ] Test: INSERT as org_A, SELECT as org_B → empty
- [ ] Test: INSERT as org_A, SELECT as holding → visible
- [ ] Test: UPDATE/DELETE isolation

**Effort:** 3 MD

---

#### Epic: Document API (engine-data:sink-doc)

**Unit ID:** engine-data | **Effort:** 10 MD

---

##### Story: gRPC Document Storage

**Jako** engine-orchestrator Orchestrátor **chci** uložit nestrukturované texty z prezentací **abych** je měl k dispozici pro full-text a sémantické vyhledávání.

**AC:**

- [ ] `gRPC StoreDocument(StoreDocumentRequest)` přijímá text s metadaty přes Dapr gRPC
- [ ] Data uložena do PostgreSQL (JSONB + full-text index)
- [ ] RLS policy per org_id
- [ ] Response: `StoreDocumentResponse { document_id }`
- [ ] **Žádné REST endpointy** – služba komunikuje výhradně přes Dapr gRPC. Čtení dat přes engine-data:query (CQRS).

**gRPC API (definice v `packages/protos/sink_document.proto`):**

```protobuf
service DocumentSinkService {
  rpc StoreDocument (StoreDocumentRequest) returns (StoreDocumentResponse);
  rpc DeleteByFileId (DeleteByFileIdRequest) returns (DeleteResponse);
}

message StoreDocumentRequest {
  string org_id = 1;
  string file_id = 2;
  int32 slide_id = 3;
  string content = 4;
  string content_type = 5;
  map<string, string> metadata = 6;
}

message StoreDocumentResponse {
  string document_id = 1;
}
```

**DB:**

```sql
-- V005__create_documents.sql
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id),
    file_id UUID REFERENCES files(id),
    slide_id INT,
    content TEXT NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    metadata JSONB,
    embedding vector(1536),  -- pgVector, filled async by processor-atomizers:ai
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY documents_org_isolation ON documents
    USING (org_id = current_setting('app.current_org_id')::UUID);

CREATE INDEX idx_documents_org ON documents(org_id);
CREATE INDEX idx_documents_content_fts ON documents USING gin(to_tsvector('simple', content));
```

**Tasks:**

- [ ] gRPC server setup (grpc-spring-boot-starter), Dapr sidecar: `app-protocol: grpc`, `app-id: ms-sink-doc`
- [ ] Protobuf kompilace z `packages/protos/sink_document.proto`
- [ ] Flyway migrace V005 – documents tabulka s RLS a FTS index
- [ ] gRPC service implementace `DocumentSinkServiceImpl` (ne REST controller)
- [ ] `DocumentRepository` (Spring Data JPA)
- [ ] RLS enforcement
- [ ] `DeleteByFileId` gRPC method – compensating action pro Saga rollback
- [ ] `embedding` sloupec zatím NULL – bude plněn v P2 (processor-atomizers:ai)
- [ ] Unit testy

**Effort:** 6 MD | **Závislosti:** PostgreSQL, Flyway, Proto definitions

---

##### Story: Vector Embedding Pipeline (Prepared)

**Jako** systém **chci** připravit infrastrukturu pro vector embeddings **abych** je mohl v P2 plnit přes processor-atomizers:ai.

**AC:**

- [ ] pgVector extension nainstalována a funkční
- [ ] `embedding` sloupec typu `vector(1536)` existuje v documents tabulce
- [ ] Index HNSW nebo IVFFlat připraven (ale prázdný)

**Tasks:**

- [ ] Ověřit pgVector extension v PostgreSQL
- [ ] `CREATE INDEX idx_documents_embedding ON documents USING hnsw (embedding vector_cosine_ops);`
- [ ] Placeholder service: `EmbeddingService.generateEmbedding()` → throws "Not implemented in P1"

**Rozhodnutí (potvrzeno):**

- **Embedding model:** OpenAI `text-embedding-3-small` (1536 dimenzí) – levnější a výkonnější než v2
- **Provider:** Azure Foundry AI Services (managed, multi-model)
- **Embedding dimension:** 1536 (potvrzeno, odpovídá `vector(1536)` sloupci)

**Effort:** 2 MD

---

#### Epic: Log API (engine-data:sink-log)

**Unit ID:** engine-data | **Effort:** 5 MD

---

##### Story: gRPC Append-Only Processing Log

**Jako** engine-orchestrator Orchestrátor **chci** logovat každý krok zpracování souboru **abych** měl audit trail pro debugging a monitoring.

**AC:**

- [ ] `gRPC AppendLog(AppendLogRequest)` přidává záznam do processing logu přes Dapr gRPC
- [ ] Append-only: žádné UPDATE ani DELETE
- [ ] Záznamy: `step_name`, `status`, `duration_ms`, `error_detail`
- [ ] **Čtení logů:** Přes engine-data:query REST endpoint `GET /api/logs/{file_id}` (CQRS read side, přístupné z frontendu přes API Gateway)
- [ ] **Žádné REST endpointy na engine-data:sink-log** – zápis výhradně přes Dapr gRPC

**gRPC API (definice v `packages/protos/sink_log.proto`):**

```protobuf
service LogSinkService {
  rpc AppendLog (AppendLogRequest) returns (AppendLogResponse);
  rpc GetLogs (GetLogsRequest) returns (GetLogsResponse);  // Interní čtení pro engine-data:query
}

message AppendLogRequest {
  string file_id = 1;
  string step_name = 2;
  string status = 3;  // SUCCESS, FAILED, SKIPPED
  int32 duration_ms = 4;
  string error_detail = 5;
  map<string, string> metadata = 6;
}

message AppendLogResponse {
  string log_id = 1;
}

message GetLogsRequest {
  string file_id = 1;
}

message GetLogsResponse {
  string file_id = 1;
  repeated LogEntry steps = 2;
}

message LogEntry {
  string step_name = 1;
  string status = 2;
  int32 duration_ms = 3;
  string error_detail = 4;
  string timestamp = 5;
  map<string, string> metadata = 6;
}
```

**REST endpoint pro frontend (v rámci engine-data:query, ne engine-data:sink-log):**

```
GET /api/logs/{file_id}  (přes API Gateway)

→ 200 OK
{
  "file_id": "uuid",
  "steps": [
    { "step_name": "extract_structure", "status": "SUCCESS", "duration_ms": 800, "timestamp": "..." },
    { "step_name": "extract_slide_1", "status": "SUCCESS", "duration_ms": 1100, "timestamp": "..." },
    { "step_name": "extract_slide_2", "status": "FAILED", "duration_ms": 500, "error_detail": "...", "timestamp": "..." }
  ]
}
```

**DB:**

```sql
-- V006__create_processing_logs.sql
CREATE TABLE processing_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id),
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, SKIPPED
    duration_ms INT,
    error_detail TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- App user has only INSERT + SELECT (no UPDATE, no DELETE)
-- GRANT INSERT, SELECT ON processing_logs TO app_user;

CREATE INDEX idx_processing_logs_file ON processing_logs(file_id);
```

**Tasks:**

- [ ] gRPC server setup (grpc-spring-boot-starter), Dapr sidecar: `app-protocol: grpc`, `app-id: ms-sink-log`
- [ ] Protobuf kompilace z `packages/protos/sink_log.proto`
- [ ] Flyway migrace V006 – processing_logs tabulka
- [ ] gRPC service implementace `LogSinkServiceImpl` (ne REST controller)
- [ ] `GetLogs` gRPC method pro interní čtení z engine-data:query
- [ ] App user permissions: INSERT + SELECT only
- [ ] Unit testy

**Effort:** 4 MD | **Závislosti:** PostgreSQL, Proto definitions

---

### FS09 – Frontend SPA (Basic)

---

#### Epic: Auth & Shell (frontend)

**Unit ID:** frontend | **Effort:** 10 MD

---

##### Story: React Project Setup

**Jako** vývojář **chci** připravit frontend projekt **abych** měl základ pro SPA aplikaci.

**AC:**

- [ ] React 18 + Vite + TypeScript + Tailwind CSS
- [ ] Projekt builduje a startuje (`npm run dev`, `npm run build`)
- [ ] ESLint + Prettier konfigurace
- [ ] Path aliases (`@/components`, `@/hooks`, `@/lib`)
- [ ] Docker Compose entry (port 3000, HMR)

**Tasks:**

- [ ] `npm create vite@latest` s React + TypeScript šablonou
- [ ] Tailwind CSS setup
- [ ] ESLint + Prettier konfigurace
- [ ] tsconfig paths
- [ ] Dockerfile pro produkční build (nginx)
- [ ] Docker Compose entry s volume mount pro HMR

- [ ] UI component library: FluentUI (based on MS)
- [ ] Monorepo přístup NX 

**Effort:** 2 MD

---

##### Story: MSAL Authentication

**Jako** uživatel **chci** se přihlásit přes firemní Microsoft účet **abych** měl přístup k platformě.

**AC:**

- [ ] MSAL Provider inicializován při startu aplikace
- [ ] Login flow: redirect na Azure AD login page
- [ ] Po přihlášení: JWT token uložen v MSAL cache
- [ ] Token automaticky přidán do každého API requestu (Axios interceptor)
- [ ] Token refresh: automatický silent refresh před expirací
- [ ] Logout: clear cache + redirect
- [ ] Neautentizovaný přístup → redirect na login

**Tasks:**

- [ ] `@azure/msal-browser` + `@azure/msal-react` dependencies
- [ ] MSAL konfigurace: `clientId`, `authority`, `redirectUri`
- [ ] `AuthProvider` wrapper komponenta
- [ ] Axios instance s interceptorem: `Authorization: Bearer <token>`
- [ ] Token refresh interceptor (retry na 401 se silent acquire)
- [ ] Login/Logout komponenty
- [ ] Protected route wrapper: `<AuthenticatedRoute>`
- [ ] Unit testy: mock MSAL provider

- [ ] Azure App Registration Client ID - KeyVault
- [ ] Redirect URI pro lokální dev a produkci  - KeyVault
- [ ] Scope: `api://<client_id>/access_as_user`

**Effort:** 5 MD

---

##### Story: App Shell & Navigation

**Jako** uživatel **chci** vidět základní layout aplikace **abych** se mohl orientovat v platformě.

**AC:**

- [ ] Layout: sidebar (navigation) + main content area + top bar (user info, logout)
- [ ] Navigace: Dashboard (P2), Upload, Files, Settings
- [ ] Responsive: desktop-first, ale použitelné na tabletu
- [ ] User info v top baru: jméno, role, organizace (z `/api/auth/me`)
- [ ] Loading state při načítání user context

**Tasks:**

- [ ] Layout komponenta: `<AppShell>`
- [ ] Sidebar s navigačními odkazy (React Router)
- [ ] Top bar s user info (`useQuery` → `/api/auth/me`)
- [ ] React Router setup: routes pro Upload, Files, Settings
- [ ] 404 page

- [ ] UI design / wireframe pro layout ve Figma
- [ ] Barevné schéma a branding bdue definován v configUI.json
- [ ] Dark mode v P1

**Effort:** 3 MD

---

#### Epic: Upload Manager (frontend)

**Unit ID:** frontend | **Effort:** 12 MD

---

##### Story: Drag & Drop Upload Zone

**Jako** Editor **chci** nahrát soubor přetažením do prohlížeče **abych** mohl rychle a pohodlně nahrávat reporty.

**AC:**

- [ ] Drag & drop zóna (react-dropzone) na Upload stránce
- [ ] Kliknutí na zónu otevře file picker
- [ ] Validace na FE: povolené typy (.pptx, .xlsx, .pdf, .csv), max size
- [ ] Nepovolený typ → chybová hláška bez odeslání na server
- [ ] Progress bar během uploadu (XHR upload events)
- [ ] Po úspěšném uploadu → success notifikace + refresh seznamu souborů

**Tasks:**

- [ ] `react-dropzone` integrace
- [ ] FE validace: file type a size
- [ ] Upload service: `uploadFile(file)` → Axios POST `/api/upload` s `onUploadProgress`
- [ ] Progress bar komponenta
- [ ] Success/error toast notifikace
- [ ] React Query invalidation (`queryClient.invalidateQueries(['files'])`)

**Effort:** 4 MD | **Závislosti:** MSAL Auth, engine-ingestor Upload Endpoint

---

##### Story: File List View

**Jako** Editor **chci** vidět seznam nahraných souborů **abych** měl přehled o stavu zpracování.

**AC:**

- [ ] Tabulka: filename, size, upload date, processing status, actions
- [ ] Status badge: `UPLOADED` (šedá), `PROCESSING` (žlutá), `DONE` (zelená), `FAILED` (červená), `PARTIAL` (oranžová)
- [ ] Řazení dle data uploadu (nejnovější nahoře)
- [ ] Klik na soubor → přechod na Viewer

**API (čtení z engine-ingestor nebo engine-data:query):**

```
GET /api/files?org_id={org_id}&page=1&size=20

→ 200 OK
{
  "files": [
    {
      "file_id": "uuid",
      "filename": "report_q2.pptx",
      "size_bytes": 15234567,
      "mime_type": "...",
      "processing_status": "DONE",
      "uploaded_at": "2026-03-09T10:30:00Z"
    }
  ],
  "total": 42,
  "page": 1,
  "size": 20
}
```

**Tasks:**

- [ ] Backend: `GET /api/files` endpoint v engine-ingestor (read from files table)
- [ ] FE: `FileList` komponenta s TanStack Table
- [ ] Status badge komponenta
- [ ] Pagination
- [ ] React Query hook: `useFiles(orgId, page)`
- [ ] Link na Viewer stránku

- [ ] file list endpoint v engine-ingestor
- [ ] Filtrování: dle typu souboru, dle data, dle statusu

**Effort:** 4 MD | **Závislosti:** Upload, Backend endpoint

---

#### Epic: Basic Viewer (frontend)

**Unit ID:** frontend | **Effort:** 10 MD

---

##### Story: Slide-by-Slide Viewer

**Jako** Viewer **chci** prohlížet parsovaná data z PPTX prezentace slide po slidu **abych** ověřil správnost extrakce.

**AC:**

- [ ] Stránka `/files/{file_id}` zobrazuje detail souboru
- [ ] Levý panel: thumbnail navigace (miniatura slidů)
- [ ] Hlavní panel: vybraný slide – PNG preview + extrahované texty + tabulky
- [ ] Tabulky zobrazeny jako HTML tabulky (read-only)
- [ ] Processing log: timeline kroků zpracování (ze engine-data:sink-log)
- [ ] Read-only: žádné editace v P1

**Tasks:**

- [ ] Route `/files/:fileId` s `FileViewer` komponentou
- [ ] Slide thumbnail navigace (vertikální list miniatur)
- [ ] Slide detail: PNG obrázek z `artifact_url`
- [ ] Extrahované texty: zobrazení per shape
- [ ] Extrahované tabulky: `<table>` komponenta
- [ ] Processing log timeline (GET `/logs/{file_id}`)
- [ ] React Query hooks: `useFileDetail(fileId)`, `useSlideData(fileId, slideId)`, `useProcessingLog(fileId)`

- [ ] Wireframe / mockup pro Viewer layout - Koncepce: „The Three-Pane Layout“
- [ ] viewer zobrazuje originální slide (PNG) a extrahovaná data vedle sebe
- [ ] Má uživatel vidět notes (poznámky ke slidu) - Ne.

**Effort:** 8 MD | **Závislosti:** processor-atomizers:pptx, engine-data:sink-tbl, engine-data:sink-log

---

#### Epic: Real-time Feedback (frontend)

**Unit ID:** frontend | **Effort:** 5 MD

---

##### Story: Processing Status Updates

**Jako** Editor **chci** vidět aktuální stav zpracování souboru v reálném čase **abych** věděl, kdy je soubor hotový.

**AC:**

- [ ] Po uploadu: status se aktualizuje automaticky (Polling v P1, SSE v P2)
- [ ] Status přechody viditelné v UI bez refresh: `UPLOADED → PROCESSING → DONE/FAILED`
- [ ] Notifikace (toast) při dokončení zpracování

**Tasks:**

- [ ] React Query s `refetchInterval: 3000` pro aktivní soubory (P1 – polling)
- [ ] Toast notifikace při přechodu do `DONE` nebo `FAILED`
- [ ] Indikátor zpracování: `Processing slide 3/15...`
- [ ] Prepared: abstrakce pro budoucí SSE (P2) – `useFileStatus(fileId)` hook

**Rozhodnutí (potvrzeno):**

- **P1:** Polling (React Query, 3s interval) – jednodušší, bez starostí s load-balancerem
- **P2:** SSE (Server-Sent Events) – lepší UX, zavedení v P2

**Effort:** 5 MD | **Závislosti:** Upload, File List

---

### P1 – Souhrn Stories

| # | Epic / Story | Function ID | Effort | Protokol | Závislosti |
|---|---|---|---|---|---|
| 1 | Nginx Routing & auth_request | router | 1 MD | REST (edge) | — |
| 2 | Spring Boot Base Setup (AUTH) | engine-core:auth | 2 MD | REST (edge) | — |
| 3 | Azure Entra ID Token Validation | engine-core:auth | 5 MD | REST (auth_request) | #2 |
| 4 | RBAC Engine | engine-core:auth | 8 MD | REST (edge) | #3 |
| 5 | KeyVault Integration | engine-core:auth | 2 MD | — | #2 |
| 6 | PostgreSQL & Flyway Setup | engine-core:auth | 3 MD | TCP | — |
| 7 | Redis Connection Setup | engine-core:auth | 2 MD | TCP | — |
| 8 | Docker Compose Full Topology + Dapr | infra | 5 MD | — | — |
| 9 | Spring Boot Base Setup (ING) | engine-ingestor | 1 MD | REST (edge) | — |
| 10 | Streaming Upload Endpoint | engine-ingestor | 8 MD | REST (edge) | #3, #6 |
| 11 | ClamAV Security Scan | engine-ingestor:scanner | 5 MD | Dapr gRPC | #10 |
| 12 | File Sanitization | engine-ingestor | 4 MD | — | #10 |
| 13 | Orchestrator Event Trigger | engine-ingestor | 2 MD | Dapr Pub/Sub | #10, #20 |
| 14 | gRPC Server Base Setup (ATM-PPTX) | processor-atomizers:pptx | 2 MD | Dapr gRPC | Proto defs |
| 15 | PPTX Structure Extraction | processor-atomizers:pptx | 5 MD | Dapr gRPC | #14 |
| 16 | Slide Content Extraction | processor-atomizers:pptx | 6 MD | Dapr gRPC | #15 |
| 17 | Slide Image Rendering | processor-atomizers:pptx | 6 MD | Dapr gRPC | #15 |
| 18 | MetaTable Logic | processor-atomizers:pptx | 8 MD | Dapr gRPC | #16 |
| 19 | Error Handling & Resilience (ATM) | processor-atomizers:pptx | 3 MD | gRPC status | #15 |
| 20 | Workflow Engine Setup | engine-orchestrator | 8 MD | Dapr gRPC + Pub/Sub | #6, #7 |
| 21 | Type-Safe Contracts (Proto defs) | engine-orchestrator | 5 MD | Protobuf | #20 |
| 22 | PPTX Processing Workflow (Saga) | engine-orchestrator | 12 MD | Dapr gRPC | #21, #15, #16, #17, #25, #26, #27 |
| 23 | Error Handling & Exponential Backoff | engine-orchestrator | 7 MD | Dapr gRPC | #22 |
| 24 | Idempotence (Redis-based) | engine-orchestrator | 4 MD | TCP (Redis) | #22, #7 |
| 24b | Async Worker Layer | engine-orchestrator | 5 MD | Dapr Pub/Sub | #20 |
| 24c | File Type Router | engine-orchestrator | 2 MD | — | #20 |
| 24d | Workflow State Persistence | engine-orchestrator | 5 MD | TCP (Redis/PG) | #20, #7 |
| 25 | gRPC Bulk Insert (SINK-TBL) | engine-data:sink-tbl | 8 MD | Dapr gRPC | #6, Proto defs |
| 25b | Spring Boot + gRPC Base (SINK-TBL) | engine-data:sink-tbl | 2 MD | Dapr gRPC | — |
| 26 | RLS Integration Test | engine-data:sink-tbl | 3 MD | — | #25 |
| 27 | gRPC Document Storage (SINK-DOC) | engine-data:sink-doc | 6 MD | Dapr gRPC | #6, Proto defs |
| 28 | Vector Embedding Pipeline (prepared) | engine-data:sink-doc | 2 MD | — | #27 |
| 29 | gRPC Append-Only Processing Log | engine-data:sink-log | 4 MD | Dapr gRPC | #6, Proto defs |
| 30 | React Project Setup | frontend | 2 MD | — | — |
| 31 | MSAL Authentication | frontend | 5 MD | REST (edge) | #30 |
| 32 | App Shell & Navigation | frontend | 3 MD | REST (edge) | #31 |
| 33 | Drag & Drop Upload Zone | frontend | 4 MD | REST (edge) | #31, #10 |
| 34 | File List View | frontend | 4 MD | REST (edge) | #31 |
| 35 | Slide-by-Slide Viewer | frontend | 8 MD | REST (edge) | #34, #16, #17, #29 |
| 36 | Processing Status Updates | frontend | 5 MD | REST polling | #33, #34 |

---

## Phase 2 – Extended Parsing

**Cíl:** Plná podpora formátů (Excel, PDF, CSV), cleanup worker, CQRS read model, základní dashboardy.

**Scope:** FS03-rest, FS10, FS06

**Effort:** ~86 MD

---

### FS03 – Remaining Atomizers

---

#### Epic: Excel Atomizer (processor-atomizers:xls)

**Unit ID:** processor-atomizers | **Effort:** 16 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | gRPC Server Base Setup | 2 MD | Stejná šablona jako processor-atomizers:pptx – gRPC server, Dapr sidecar (`app-id: ms-atm-xls`, `app-protocol: grpc`), žádné REST endpointy |
| 2 | Sheet Structure Extraction | 3 MD | `gRPC ExtractStructure(ExtractRequest)` → `ExcelStructureResponse` s listem listů (row_count, col_count) |
| 3 | Sheet Content Extraction | 5 MD | `gRPC ExtractSheetContent(SheetRequest)` → `SheetContentResponse` s headers + rows + data_types per sheet |
| 4 | Partial Success Handling | 3 MD | 9/10 listů OK + 1 FAILED → `PartialResult { status: "PARTIAL", successful: [...], failed: [...] }` |
| 5 | engine-orchestrator Excel Workflow | 3 MD | JSON workflow definition pro batch iteraci přes listy, Dapr gRPC routing na SINK-TBL |

- [ ] Má Excel Atomizer detekovat merged cells a jak je zpracovat? Ano, rozdělit a zkopírovat do sloupců
- [ ] Formátování čísel: zachovat formát z Excelu nebo normalizovat? Normalizovat
- [ ] Prázdné řádky/sloupce: skipovat nebo zachovat? skipopvat

---

#### Epic: PDF/OCR Atomizer (processor-atomizers:pdf)

**Unit ID:** processor-atomizers | **Effort:** 16 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | gRPC Server Base Setup | 2 MD | gRPC server + Tesseract OCR v Dockerfile, Dapr sidecar (`app-id: ms-atm-pdf`, `app-protocol: grpc`), žádné REST endpointy |
| 2 | PDF Type Detection | 2 MD | Detekce: textový PDF vs. skenovaný (image-based) |
| 3 | Text PDF Extraction | 4 MD | `gRPC ExtractPdf(ExtractRequest)` → PyPDF2/pdfplumber: extrakce textu, tabulek, metadat |
| 4 | OCR Pipeline | 5 MD | Tesseract OCR pro skenované stránky → text |
| 5 | engine-orchestrator PDF Workflow | 3 MD | JSON workflow definition: detect type → route → extract → store (Dapr gRPC) |

- [ ] OCR jazyky: angličtina
- [ ] Tesseract: přetrénovaný model pro finanční dokumenty? Ne, stačí klasciké tabulky.
- [ ] PDF tabulky: Camelot/Tabula integrace? Ano, zkusíme free

---

#### Epic: CSV Atomizer (processor-atomizers:csv)

**Unit ID:** processor-atomizers | **Effort:** 5 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | gRPC Server + CSV Parser | 3 MD | gRPC server + auto-detection: oddělovač (`,;|\t`), kódování (UTF-8, CP1250), header detection. Dapr sidecar (`app-id: ms-atm-csv`, `app-protocol: grpc`), žádné REST endpointy |
| 2 | engine-orchestrator CSV Workflow | 2 MD | JSON workflow definition: parse → store (Dapr gRPC) |

---

#### Epic: Cleanup Worker (processor-atomizers:cleanup)

**Unit ID:** processor-atomizers | **Effort:** 5 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Cleanup CronJob | 3 MD | Smazání dočasných artefaktů (PNG, CSV) z Blob starších než 24h |
| 2 | Cleanup Konfigurace | 2 MD | Konfigurovatelná retention policy per artifact type |

- [ ] Retention policy: 24h pro všechny temporary, pokud je proces za sebou sám nevyčistí
- [ ] Má CronJob logovat smazané soubory do engine-data:sink-log? Ne

---

### FS06 – Analytics & Query (CQRS Read)

---

#### Epic: Query API (engine-data:query) – CQRS Read Side

**Unit ID:** engine-data | **Effort:** 14 MD

> **engine-data:query je edge služba** – vystavuje REST endpointy pro frontend přes API Gateway. Čte data z PostgreSQL (zapsaná přes Sinky). Pro čtení logů volá engine-data:sink-log přes Dapr gRPC.

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Spring Boot Base Setup | 1 MD | Base setup, Redis client, Dapr sidecar (`app-protocol: http` – REST pro GW, gRPC client pro interní volání) |
| 2 | File Detail Endpoint | 3 MD | REST `GET /api/files/{id}` – kompletní detail souboru s parsovanými daty (přes API Gateway) |
| 3 | Table Data Query | 4 MD | REST `GET /api/tables?org_id=&file_id=&slide_id=` – dotazy nad JSONB (přes API Gateway) |
| 4 | Processing Log Query | 2 MD | REST `GET /api/logs/{file_id}` – čtení processing logů (Dapr gRPC volání na engine-data:sink-log `GetLogs`) |
| 5 | Redis Caching Layer | 2 MD | Cache s TTL (5 min) pro nejčastější dotazy |
| 6 | Materialized Views | 2 MD | Flyway migrace: precomputed views pro dashboard aggregace |

---

#### Epic: Dashboard Aggregation (engine-data:dashboard) – Edge Service

**Unit ID:** engine-data | **Effort:** 35 MD

> **engine-data:dashboard je edge služba** – vystavuje REST endpointy pro frontend přes API Gateway.

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Spring Boot Base Setup | 1 MD | Base setup, Dapr sidecar (`app-protocol: http` – REST pro API Gateway) |
| 2 | Aggregation Engine | 8 MD | SQL nad JSONB: GROUP BY, ORDER BY, filtr datum/org z UI parametrů |
| 3 | Dashboard Configuration API | 5 MD | REST CRUD pro definice dashboardů (JSON config: zdroj dat, typ grafu, filtry) |
| 4 | Chart Data Endpoints | 8 MD | REST endpointy vracející data pro grafy: bar, line, pie, table (přes API Gateway) |
| 5 | FE: Dashboard Viewer | 8 MD | React komponenty: chart library integrace, interaktivní filtry, drill-down |
| 6 | FE: Dashboard Builder (Admin) | 5 MD | Admin UI pro konfiguraci dashboardů |

**Rozhodnutí (potvrzeno):**

- **Chart library:** Recharts (lehká, React-nativní) pro standardní grafy + Nivo pro komplexnější vizualizace (heatmaps)

- [ ] SQL editor pro pokročilé uživatele – v P2
- [ ] Jaké výchozí dashboardy vytvořit? OPEX přehled per org, trend, srovnání

---

### FS10 – Excel Parsing Logic

*Zahrnut v Epic processor-atomizers:xls výše. Partial success logika a datová kompatibilita s PPTX data (JSONB format) pokryta v stories #3 a #4.*

---

## Phase 3a – Intelligence & Admin

**Cíl:** Holdingová hierarchie, AI integrace, Schema Mapping s learning.

**Scope:** FS07, FS08, FS12, FS15

**Effort:** ~90 MD

---

### FS07 – Admin Backend & UI

#### Epic: Admin Backend (engine-core:admin)

**Unit ID:** engine-core | **Effort:** 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Role Management API | 5 MD | CRUD pro přiřazení rolí uživatelům per organizace |
| 2 | Organization Hierarchy API | 4 MD | CRUD pro holding → dceřiné společnosti, hierarchická navigace |
| 3 | Failed Jobs UI Backend | 3 MD | `GET /admin/failed-jobs`, `POST /admin/failed-jobs/{id}/reprocess` |
| 4 | API Key Management | 4 MD | Generování, hashování (bcrypt), revokace service account klíčů |
| 5 | FE: Admin Section | 4 MD | React stránky pro role, organizace, failed jobs, API keys |

- [ ] Má Admin UI být součást hlavní SPA nebo separátní aplikace? Separátní app
- [ ] Invitation flow: jak se přidají noví uživatelé? (Azure AD sync nebo manuální pozvánka?) - skrze AD sync
- [ ] API key rate limiting: per klíč nebo per organizace? per klíč

---

### FS08 – Batch & Org Management

#### Epic: Batch & Org Service (engine-core:batch)

**Unit ID:** engine-core | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Batch Management API | 5 MD | CRUD pro batch (reporting period tag), přiřazení souborů k batchi |
| 2 | Organization Metadata | 4 MD | Holdingová metadata per soubor (`holding_id`, `company_id`, `uploaded_by`) |
| 3 | RLS Enforcement Layer | 3 MD | Sdílený middleware pro automatické nastavení `app.current_org_id` |
| 4 | FE: Batch Dashboard | 3 MD | Přehled batchů, stav souborů v batchi |


- [ ] Vztah Batch vs. Reporting Period (FS20): sloučení konceptů nebo dva různé objekty? různé objekty. batch nemusí odpovídat Reporting
- [ ] Granularita org metadata: stačí `org_id` nebo potřeba `division_id`, `cost_center_id`? granularita `org_id`, `division_id`, `cost_center_id`

---

### FS12 – AI Integration (MCP)

#### Epic: AI Gateway (processor-atomizers:ai)

**Unit ID:** processor | **Effort:** 13 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | LiteLLM Integration | 4 MD | gRPC `AnalyzeSemantic(SemanticRequest)` přes Dapr, LiteLLM wrapper pro multi-model. **Žádné REST endpointy** – voláno z engine-orchestrator přes Dapr gRPC |
| 2 | Token Quota & Cost Control | 3 MD | Tracking spotřeby tokenů per user/org, gRPC `RESOURCE_EXHAUSTED` při překročení kvóty |
| 3 | Vector Embedding Generator | 3 MD | Async generování embeddings pro documents v engine-data:sink-doc (Dapr gRPC volání) |
| 4 | Prompt Templates | 3 MD | Konfigurovatelné prompty pro klasifikaci, sumarizaci, extrakci entit |

**Rozhodnutí (částečně potvrzeno):**

- **Embedding model:** OpenAI `text-embedding-3-small` (1536 dims) via Azure Foundry AI Services

- [ ] LLM provider pro sémantickou analýzu: Azure OpenAI, OpenAI API, nebo open-source (Ollama)? všechny 3 dostupní, v config určíme
- [ ] Měsíční token quota per organizace: jaká výchozí hodnota? 0. Bude nastavitelné v konfiguraci.

#### Epic: MCP Server (processor-generators:mcp)

**Unit ID:** processor | **Effort:** 12 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | MCP Server Setup | 4 MD | FastAPI MCP server, OAuth On-Behalf-Of flow |
| 2 | Data Query Tools | 4 MD | MCP tools pro dotazování nad table_data a documents |
| 3 | RLS Enforcement in AI | 2 MD | Každý AI dotaz scoped na `org_id` uživatele |
| 4 | AI Audit Logging | 2 MD | Logování každého promptu a odpovědi do engine-core:audit |

- [ ] MCP protocol verze a klientské SDK? Microsft Azure MCP SDK
- [ ] Jaké MCP tools definovat? query_tables, search_documents, summarize_report,import_report, proced_flow

---

### FS15 – Schema Mapping Registry

#### Epic: Template & Schema Registry (engine-data:template)

**Unit ID:** engine-data | **Effort:** 30 MD

> **engine-data:template je interní služba** – komunikuje výhradně přes Dapr gRPC (voláno z engine-orchestrator). Mapping Editor UI přistupuje přes dedikované REST endpointy na engine-core:admin (proxy), ne přímo na engine-data:template.

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Mapping Rule Engine | 8 MD | Pravidla: `IF column contains "Cena/Cost/Náklady" → map to amount_czk` |
| 2 | Mapping Template CRUD | 5 MD | gRPC API pro vytvoření, editaci, verzování mapovacích šablon (voláno z engine-core:admin) |
| 3 | Auto-suggestion (Learning) | 7 MD | Systém navrhuje mapování na základě historie (fuzzy match na column names) |
| 4 | engine-orchestrator Integration Point | 3 MD | Dapr gRPC `ApplyMapping()` voláno z engine-orchestrator PŘED zápisem do Sink |
| 5 | Excel-to-Form Mapping | 4 MD | gRPC `MapExcelToForm()` – mapování Excel sloupců na pole formuláře (FS19 prepared) |
| 6 | FE: Mapping Editor | 3 MD | UI pro definici a testování mapovacích pravidel (přes engine-core:admin REST proxy) |


- [ ] Jaké normalizační pravidla jsou potřeba? měna, jednotky, datumy, naming
- [ ] Learning: jaký algoritmus? TF-IDF, embeddings similarity, rule-based
- [ ] Priorita pravidel: explicit rule > learned suggestion > user confirmation? Ano

---

## Phase 3b – Reporting Lifecycle

**Cíl:** Stavový automat pro OPEX reporty, submission workflow, deadline management.

**Scope:** FS17, FS20

**Effort:** ~75 MD

---

### FS17 – Report Lifecycle

#### Epic: Report Lifecycle Service (engine-reporting:lifecycle)

**Unit ID:** engine-reporting | **Effort:** 25 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Report Entity & State Machine | 6 MD | `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED → DRAFT` |
| 2 | State Transition REST API | 4 MD | REST `POST /api/reports/{id}/submit`, `/approve`, `/reject` – **edge služba**, přístupné přes API Gateway pro frontend |
| 3 | Rejection with Comment | 3 MD | Povinný komentář při zamítnutí, viditelný Editorovi |
| 4 | Submission Checklist | 4 MD | Validace kompletnosti před odesláním (povinná pole, nahrané listy, validační pravidla) |
| 5 | Bulk Actions | 3 MD | HoldingAdmin: schválení/zamítnutí více reportů najednou |
| 6 | Data Lock after Approval | 3 MD | Schválená data read-only, úprava = nový DRAFT (nová verze, FS14) |
| 7 | Dapr Event Publishing | 2 MD | Dapr Pub/Sub event `report.status_changed` → engine-orchestrator a engine-reporting:notification |

**DB:**

```sql
-- V010__create_reports.sql
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id),
    period_id UUID NOT NULL,  -- FK na periods tabulku (FS20)
    report_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_by UUID,
    submitted_at TIMESTAMP,
    reviewed_by UUID,
    reviewed_at TIMESTAMP,
    rejection_comment TEXT,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(org_id, period_id, report_type, version)
);

CREATE TABLE report_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by UUID NOT NULL,
    comment TEXT,
    changed_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE reports ENABLE ROW LEVEL SECURITY;
CREATE POLICY reports_org_isolation ON reports
    USING (org_id = current_setting('app.current_org_id')::UUID);
```

- [ ] Report types: jaké typy reportů existují?  ano, zstím takpo.OPEX, CAPEX, Revenue, Custom? Nechat možnost definovat kategorii reportů
- [ ] Submission checklist: jaká konkrétní validační pravidla? Nejsou, každý report je validován až výsledkem flow
- [ ] Má rejection automaticky resetovat checklist? ne, je potřeba jej ukázat uživateli. reset manuálně eboi přo nahrání nové verze souboru
- [ ] Workflow customizace: různé report_type = různý engine-orchestrator JSON workflow definition? ANO

---

##### engine-orchestrator Lifecycle Workflows (engine-orchestrator rozšíření) – 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Submission Workflow | 5 MD | Dapr Pub/Sub event `SUBMITTED` → validace dat → Dapr gRPC notifikace HoldingAdmin |
| 2 | Approval Workflow | 4 MD | Dapr Pub/Sub event `APPROVED` → zahrnutí do centrálního reportingu → notifikace Editor |
| 3 | Rejection Workflow | 3 MD | Dapr Pub/Sub event `REJECTED` → notifikace Editor s komentářem |
| 4 | Deadline Reminder Workflow | 3 MD | Cron: X dní před deadline → Dapr Pub/Sub notifikace všem s DRAFT |

---

##### FE Lifecycle UI (frontend rozšíření) – 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Report Status Dashboard | 5 MD | Matice `[Společnost × Perioda]` se stavem každého reportu (barvy) |
| 2 | Submission Flow UI | 5 MD | Checklist → odeslání → potvrzení, stepper komponenta |
| 3 | Review Flow UI | 4 MD | HoldingAdmin: detail reportu → approve / reject s komentářem |
| 4 | Status Timeline | 3 MD | Historie stavových přechodů per report (timeline komponenta) |
| 5 | Bulk Action UI | 3 MD | Checkbox select + hromadné schválení/zamítnutí |


- [ ] Wireframe pro Report Status Dashboard (matice společnosti × perioda)? ano
- [ ] Vizuální design submission flow – stepper nebo wizard? Wizzard

---

### FS20 – Reporting Period & Deadline Management

#### Epic: Reporting Period Manager (engine-reporting:period)

**Unit ID:** engine-reporting | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Period CRUD | 3 MD | Vytvoření periody: `{ name, type, start_date, submission_deadline, review_deadline }` |
| 2 | Period State Machine | 3 MD | `OPEN → COLLECTING → REVIEWING → CLOSED` |
| 3 | Automatic Form Closure | 3 MD | Cron: po submission_deadline → formuláře CLOSED, opozdilé = override |
| 4 | Deadline Notifications | 3 MD | 7/3/1 den před deadline → notifikace (event pro engine-reporting:notification) |
| 5 | Period Cloning | 2 MD | Klonování periody z předchozí (přenesení formulářů, šablon) |
| 6 | FE: Period Dashboard | 5 MD | Matice `[Společnost × Stav]`, % dokončenosti, export |

**DB:**

```sql
-- V011__create_periods.sql
CREATE TABLE periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    holding_org_id UUID NOT NULL REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    period_type VARCHAR(20) NOT NULL,  -- MONTHLY, QUARTERLY, ANNUAL
    period_code VARCHAR(20) NOT NULL,  -- e.g. "Q2/2025"
    start_date DATE NOT NULL,
    submission_deadline DATE NOT NULL,
    review_deadline DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT NOW(),
    cloned_from UUID REFERENCES periods(id)
);
```

- [ ] Může mít organizace více aktivních period současně? Ano
- [ ] Automatické uzavření: hard close (nelze vůbec) nebo soft close (lze s override)? Soft Close
- [ ] Eskalace: jen notifikace nebo i automatická akce (email vedení)? jen notifikace. zatím.

---

## Phase 3c – Form Builder

**Cíl:** Centrální sběr OPEX dat přes formuláře – nahrazení Excel šablon posílaných emailem.

**Scope:** FS19

**Effort:** ~81 MD

---

### FS19 – Dynamic Form Builder

#### Epic: Form Builder Service (engine-reporting:form)

**Unit ID:** engine-reporting | **Effort:** 40 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Form Definition Schema | 6 MD | JSON schema pro definici formuláře: sekce, pole, typy, validace |
| 2 | Form CRUD API | 5 MD | Vytvoření, editace, publikace, uzavření formuláře |
| 3 | Form Versioning | 4 MD | Změna publikovaného formuláře → nová verze, historická data zachována |
| 4 | Field Types & Validation | 6 MD | text, number, percentage, date, dropdown, table, file_attachment + validační pravidla |
| 5 | Form Assignment | 3 MD | Přiřazení formuláře k `period_id`, `report_type` a konkrétním organizacím |
| 6 | Form Data Collection API | 6 MD | `POST /forms/{id}/responses` – uložení vyplněných dat, auto-save |
| 7 | Field-level Comments | 3 MD | Komentáře k jednotlivým hodnotám (`"Toto číslo zahrnuje jednorázový odpis"`) |
| 8 | Submission Integration | 3 MD | Po odeslání → report entity (engine-reporting:lifecycle) přechází do `SUBMITTED` |
| 9 | Conditional Field Logic | 4 MD | `if field_A > 0 then field_B is required` |

**DB:**

```sql
-- V012__create_forms.sql
CREATE TABLE forms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope VARCHAR(20) NOT NULL DEFAULT 'CENTRAL', -- CENTRAL | LOCAL (FS21)
    owner_org_id UUID REFERENCES organizations(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, CLOSED
    version INT NOT NULL DEFAULT 1,
    period_id UUID REFERENCES periods(id),
    report_type VARCHAR(50),
    definition JSONB NOT NULL,  -- complete form schema
    created_at TIMESTAMP DEFAULT NOW(),
    published_at TIMESTAMP
);

CREATE TABLE form_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    form_id UUID NOT NULL REFERENCES forms(id),
    org_id UUID NOT NULL REFERENCES organizations(id),
    period_id UUID REFERENCES periods(id),
    form_version INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, SUBMITTED
    data JSONB NOT NULL,  -- { field_id: value, ... }
    comments JSONB,  -- { field_id: "comment text", ... }
    submitted_by UUID,
    submitted_at TIMESTAMP,
    auto_saved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE form_responses ENABLE ROW LEVEL SECURITY;
CREATE POLICY form_responses_org_isolation ON form_responses
    USING (org_id = current_setting('app.current_org_id')::UUID);
```

- [ ] Form definition JSON schema: jaký formát? JSON Schema
- [ ] Auto-save interval: 30 s nebo konfigurovatelný? 30 sec
- [ ] Má být `table` field type plnohodnotný spreadsheet, nebo jednoduchá tabulka s pevnými sloupci? Jednoduchá tebulka
- [ ] File attachment: max počet a velikost příloh per formulář? 0-1 s daty, 0-5 s doplňky

---

#### Epic: Excel Export/Import (engine-reporting:form rozšíření)

**Unit ID:** engine-reporting | **Effort:** 8 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Excel Template Export | 3 MD | `GET /forms/{id}/export/excel-template` → strukturovaný Excel s metadata listem |
| 2 | Excel Template Import | 3 MD | `POST /forms/{id}/import/excel` → párování dle `__form_meta`, validace verze |
| 3 | Arbitrary Excel Import | 2 MD | Upload vlastního Excelu + mapování sloupců na pole formuláře (engine-data:template) |

---

#### Epic: FE Form UI (frontend rozšíření)

**Unit ID:** frontend | **Effort:** 25 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Form Builder UI (Admin) | 8 MD | Drag & drop editor pro tvorbu formulářů, náhled |
| 2 | Form Filling UI (Editor) | 8 MD | Dynamické renderování formuláře z JSON schema, auto-save, validace |
| 3 | Submission Checklist UI | 3 MD | Vizuální checklist před odesláním |
| 4 | Excel Import Review UI | 3 MD | Po importu: zobrazení dat v UI k vizuální kontrole |
| 5 | Field Comments UI | 3 MD | Inline komentáře u každého pole |

- [ ] Form builder: vlastní implementace nebo knihovna (FormIO, SurveyJS)? Radeji knihovna, šetříme práci - co jde použít, použijeme
- [ ] Drag & drop knihovna: dnd-kit nebo react-beautiful-dnd? react-beautiful-dnd
- [ ] Wireframe pro form filling UI? plochá struktura tabulky.

---

## Phase 4a – Enterprise Features

**Cíl:** Compliance, versioning, notifications, audit – production readiness.

**Scope:** FS11, FS13, FS14, FS16

**Effort:** ~71 MD

---

### FS13 – Notification Center

#### Epic: Notification Service (engine-reporting:notification)

**Unit ID:** engine-core | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | In-app Notifications (WebSocket/SSE) | 5 MD | Push notifikace do FE při events (processing done, report approved, ...) |
| 2 | Email Notifications | 4 MD | SMTP integrace pro kritické events (deadline, rejection) |
| 3 | Notification Preferences | 3 MD | Opt-in/opt-out per event type per uživatel |
| 4 | FE: Notification Bell | 3 MD | Bell icon v top baru, dropdown se seznamem notifikací, read/unread |


- [ ] Email provider: SendGrid, Azure Communication Services, nebo SMTP server? SMTP server
- [ ] Email šablony: kdo dodá HTML šablony? připravit v admin sekci možnost nahrát a spravovat šablony
- [ ] Notification types: kompletní seznam eventů k notifikaci? ano, ale necháme možnost úpravy v aplikaci

---

### FS14 – Data Versioning & Diff

#### Epic: Versioning Service (engine-core:versioning)

**Unit ID:** engine-core | **Effort:** 16 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Version Chain API | 5 MD | Vytvoření nové verze dat (v1 → v2), originál zachován |
| 2 | Diff Engine | 6 MD | Porovnání dvou verzí: přidané/odebrané/změněné řádky, delta hodnoty |
| 3 | FE: Diff Viewer | 5 MD | Side-by-side zobrazení verzí s highlighting změn |


- [ ] Granularita verzování: per tabulka, per slide, nebo per soubor? per soubor
- [ ] Diff zobrazení: side-by-side nebo inline (git-style)? side-by-side

---

### FS16 – Audit & Compliance

#### Epic: Audit Service (engine-core:audit)

**Unit ID:** engine-core | **Effort:** 25 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Immutable Audit Log | 6 MD | Append-only tabulka: kdo, kdy, co (INSERT only, no UPDATE/DELETE) |
| 2 | Read Access Logging | 4 MD | Logování zobrazení citlivých reportů (`user_id`, `document_id`, `IP`, `timestamp`) |
| 3 | AI Audit Trail | 4 MD | Logování každého AI promptu a odpovědi |
| 4 | State Transition Audit | 3 MD | Logování stavových přechodů z FS17 |
| 5 | Export API | 4 MD | CSV/JSON export auditních logů pro bezpečnostní audit |
| 6 | FE: Audit Log Viewer | 4 MD | Filtrovaný pohled na audit logy pro Admin |

---

### FS11 – Dashboards (Extended)

#### Epic: Dashboard Builder (engine-data:dashboard rozšíření)

**Unit ID:** engine-data | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Dashboard Permissions | 3 MD | Public (Viewer vidí) vs. Private (jen autor) dashboardy |
| 2 | SQL Editor (Advanced) | 5 MD | Přímý SQL editor pro pokročilé uživatele |
| 3 | Dashboard Sharing | 3 MD | Sdílení dashboardu s konkrétními uživateli/org |
| 4 | FE: Enhanced Charts | 4 MD | Rozšířené typy grafů, interaktivní drill-down |

---

## Phase 4b – PPTX Report Generation

**Cíl:** Automatické generování standardizovaných PPTX reportů ze schválených dat.

**Scope:** FS18

**Effort:** ~75 MD

---

### FS18 – PPTX Template Engine

#### Epic: PPTX Template Manager (engine-reporting:pptx-template)

**Unit ID:** engine-reporting | **Effort:** 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Template Upload & Parsing | 5 MD | Upload PPTX šablony, extrakce `{{placeholder}}` tagů |
| 2 | Template Versioning | 3 MD | v1, v2 šablon, přiřazení k period_id / report_type |
| 3 | Placeholder Mapping UI | 6 MD | UI: `{{it_costs}}` → pole `amount_czk` z formuláře / sloupec z Excelu |
| 4 | Template Preview | 3 MD | Náhled šablony s ukázkovými hodnotami |
| 5 | Template Assignment | 3 MD | Přiřazení šablony ke konkrétním periodám a report types |

- [ ] Placeholder syntax: `{{variable}}` pro text, `{{TABLE:name}}` pro tabulky, `{{CHART:metric}}` pro grafy – je to dostačující? Ano
- [ ] Podporované typy grafů: bar, line, pie – další? tyto postačují
- [ ] Kdo definuje mapování placeholder → datový zdroj? HoldingAdmin? HoldingAdmin je moc vysoko, Admin

---

#### Epic: PPTX Generator (processor-generators:pptx)

**Unit ID:** processor | **Effort:** 32 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Template Rendering Engine | 10 MD | python-pptx: nahrazení textových placeholderů, vyplnění tabulek |
| 2 | Chart Generation | 8 MD | matplotlib/plotly: generování grafů a vložení do PPTX |
| 3 | Missing Data Handling | 3 MD | Chybějící data → `DATA MISSING` vizuální upozornění (červený rámeček) |
| 4 | Batch Generation | 5 MD | Generování PPTX pro všechny schválené reporty v periodě |
| 5 | engine-orchestrator Generation Workflow | 3 MD | JSON workflow definition: Dapr Pub/Sub event `APPROVED` → Dapr gRPC generate PPTX → Dapr Pub/Sub notify |
| 6 | FE: Generator UI | 3 MD | Trigger generování, stav, download výsledného PPTX |

- [ ] Grafové styly: kdo definuje barevné schéma a styling grafů? Bude součístí configUI.json
- [ ] Batch generation: paralelně nebo sekvenčně? sekvenčně
- [ ] Výsledný PPTX: veřejný download link nebo autentizovaný? Všechny linky autentizovaně.

---

## Phase 5 – DevOps & Onboarding

**Scope:** FS99

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | OpenTelemetry E2E Tracing | 5 MD | Trace přes FE → GW → engine-orchestrator → ATM → Sink |
| 2 | Prometheus Metrics | 5 MD | Metriky: chybovost, latence, engine-orchestrator workflow queue, DB pool |
| 3 | Grafana Dashboards | 5 MD | Operační dashboardy pro monitoring |
| 4 | Centralized Logging (Loki) | 5 MD | Structured JSON logy ze všech služeb |
| 5 | CI/CD Pipeline | 8 MD | Lint → Test → Build → Docker → Push to Registry |
| 6 | Tilt/Skaffold Local Dev | 5 MD | `tilt up` spustí kompletní topologii |
| 7 | Onboarding Documentation | 3 MD | Runbook pro onboarding první holdingové společnosti |
| 8 | Performance Testing | 5 MD | Load testy: upload, parsing, dashboard rendering |

- [ ] CI/CD: GitHub Actions nebo Azure DevOps? Azure DevOps
- [ ] Container registry: ACR (Azure Container Registry) nebo GitHub Container Registry? ACR
- [ ] Monitoring alerting: PagerDuty, Opsgenie, nebo email? email
- [ ] Onboarding: jaký je proces pro přidání nového holdingu? (tenant provisioning) - manualni vložení nového TenetID mezi povolené

---

## Phase 6 – Local Scope & Advanced Analytics

**Scope:** FS21, FS22

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Local Forms (scope: LOCAL) | 20 MD | CompanyAdmin vytváří formuláře pro interní reporting |
| 2 | Local PPTX Templates | 15 MD | CompanyAdmin nahrává vlastní PPTX šablony |
| 3 | CompanyAdmin Role | 10 MD | Nová sub-role Editora s rozšířenými právy |
| 4 | Data Release Mechanism | 5 MD | CompanyAdmin označí lokální data jako `RELEASED` pro holding |
| 5 | Shared Templates | 5 MD | Sdílení šablon mezi CompanyAdminy (scope: `SHARED_WITHIN_HOLDING`) |
| 6 | Basic Period Comparison | 8 MD | Srovnání metriky napříč periodami (Q1/2024 vs Q1/2025) |
| 7 | FE: Local Scope UI | TBD | UI pro lokální formuláře, šablony, release flow |


- [ ] CompanyAdmin: je to nová role vedle Admin/Editor/Viewer, nebo sub-role? Role Admin, jen jinak pojenovaný
- [ ] Release flow: push (CompanyAdmin posílá) nebo pull (HoldingAdmin si stahuje)?
- [ ] Advanced Period Comparison (FS22): detailní specifikace po zkušenostech z provozu

---

## Souhrn – Celkový přehled Phases

| Phase | Effort (MD) | AI (MD) | Savings | Key Milestones |
|---|---|---|---|---|
| **P1** MVP Core | 213 | 88 | 125 | Upload + PPTX parsing + viewer |
| **P2** Extended Parsing | 86 | 43 | 43 | All formats + dashboards |
| **P3a** Intelligence | 90 | 52 | 38 | Admin + AI + Schema Mapping |
| **P3b** Lifecycle | 75 | 40 | 35 | Report state machine + periods |
| **P3c** Form Builder | 81 | 41 | 40 | Forms + Excel import |
| **P4a** Enterprise | 71 | 34 | 37 | Notifications + audit + versioning |
| **P4b** PPTX Gen | 75 | 35 | 40 | Template-based report generation |
| **P5** DevOps | 41 | 20 | 21 | Production monitoring + CI/CD |
| **P6** Local + Analytics | 63+ | TBD | TBD | Subsidiary internal use |
| **TOTAL** | **~795** | **~353** | **~442** | |

---

### Rozhodnuto ✅

| Téma | Rozhodnutí |
|---|---|
| **Komunikace** | |
| Interní protokol | **Dapr gRPC** – veškerá service-to-service komunikace přes Dapr sidecars (gRPC) |
| Externí protokol | **REST** – pouze edge služby (engine-core:auth, engine-ingestor, engine-data:query, engine-data:dashboard, engine-core:admin, engine-reporting:lifecycle) přes API Gateway |
| Eventy | **Dapr Pub/Sub** – async události (file-uploaded, report.status_changed, notify) |
| Atomizery/Sinky | **Pouze Dapr gRPC** – žádné REST endpointy, volány výhradně z engine-orchestrator |
| Proto definice | Sdílený `packages/protos/` modul, protoc generování do Java/Python stubs |
| **Infrastruktura** | |
| API Gateway | Nginx (Host-based routing) + Azure Front Door (WAF + SSL) |
| Rate limiting | 100 req/s per IP (API), 10 req/s per IP (Auth/Upload), burst 20 |
| CORS | Whitelist: `https://*.company.cz` + `localhost:3000` (dev) |
| SSL (produkce) | Azure Front Door – holding standard |
| CI/CD | Azure DevOps |
| Container registry | ACR (Azure Container Registry) |
| Lokální dev | Docker Compose (Tilt/Skaffold až v P5) |
| PostgreSQL | Sdílená instance, různá schémata per service |
| Workflow engine | Spring State Machine (finální rozhodnutí CTO) |
| Redis | Sdílená instance s engine-core:auth |
| ClamAV | clamd TCP socket (port 3310), freshclam sidecar s cron |
| Blob Storage | Azure Blob Storage (lokálně Azurite v Dockeru) |
| Blob naming | `{org_id}/{yyyy}/{MM}/{file_id}/{original_filename}` |
| Max file size | 50 MB (PPTX), 100 MB (PDF s OCR) |
| Retention (raw) | 90 dní pro audit, pak smazat. Sanitizované trvale |
| Monitoring alerting | Email |
| Onboarding (tenant) | Manuální vložení nového TenantID |
| **RBAC & Org** | |
| RBAC permission matice | Admin (vše v Org), Editor (Upload/Edit), Viewer (Read-only), HoldingAdmin (Cross-org Read) |
| Organizační hierarchie | Fixní 3 úrovně: Holding → Společnost → Divize/Nákladové středisko |
| Org metadata granularita | `org_id`, `division_id`, `cost_center_id` |
| Invitation flow | Azure AD sync |
| CompanyAdmin (P6) | = Admin role, jen jinak pojmenovaný |
| Editor viditelnost | Vidí vše v rámci org + data z nižších org. jednotek |
| **Processing** | |
| Rendering engine | LibreOffice Headless (`--convert-to png`) |
| PNG rozlišení | 1280×720 (720p), ~200KB per slide |
| MetaTable threshold | Confidence > 0.85, jinak plain text + `low_confidence` flag |
| MetaTable volání | Na request (flag `detect_meta_tables: true`) |
| Atomizer file size | Stejný jako upload limit |
| Download timeout | 30 s |
| Sanitizace | VBA makra, ActiveX, embedded executables, external data connections |
| Artifact retention | 24h pro všechny temporary |
| **AI & Data** | |
| Embedding model | OpenAI `text-embedding-3-small` (1536 dims) via Azure Foundry AI Services |
| LLM provider | Multi-provider (Azure OpenAI, OpenAI API, Ollama) – konfigurovatelné |
| Token quota | Default 0, nastavitelné v konfiguraci per org |
| OCR jazyky | Angličtina |
| Schema Mapping learning | TF-IDF + embeddings similarity + rule-based |
| Priorita pravidel | explicit rule > learned suggestion > user confirmation |
| MCP SDK | Microsoft Azure MCP SDK |
| MCP tools | query_tables, search_documents, summarize_report, import_report, proced_flow |
| **Frontend & UX** | |
| UI component library | FluentUI (based on MS) |
| Monorepo | NX |
| Chart library | Recharts (standardní) + Nivo (heatmaps, komplexní) |
| Real-time feedback | P1: Polling (React Query, 3s), P2: SSE |
| Dark mode | P1 |
| Branding / barvy | Definováno v `configUI.json` |
| Wireframy | Figma |
| Viewer layout | Three-Pane Layout (slide PNG + data vedle sebe) |
| Slide notes | Nezobrazovat |
| Form builder knihovna | Hotová knihovna (FormIO / SurveyJS) |
| DnD knihovna | react-beautiful-dnd |
| Form definition | JSON Schema |
| Auto-save | 30 s |
| Table field type | Jednoduchá tabulka s pevnými sloupci |
| File attachments | 0-1 s daty, 0-5 s doplňky |
| Admin UI | Separátní aplikace |
| Submission flow design | Wizard |
| **Reporting** | |
| Report types | Konfigurovatelné kategorie (OPEX, CAPEX, Revenue, Custom) |
| Submission checklist | Validace výsledkem flow (ne manuální pravidla) |
| Rejection reset | Manuálně nebo při nahrání nové verze |
| Workflow per report_type | ANO – různý JSON workflow definition |
| Batch vs Period | Dva různé objekty |
| Period soft/hard close | Soft Close (s override) |
| Více aktivních period | ANO |
| Placeholder syntax | `{{variable}}`, `{{TABLE:name}}`, `{{CHART:metric}}` |
| Placeholder mapování | Admin role (ne HoldingAdmin) |
| Chart types (PPTX Gen) | bar, line, pie |
| Grafové styly | Definováno v `configUI.json` |
| Batch generation | Sekvenčně |
| Download linky | Vždy autentizované |
| Versioning granularita | Per soubor |
| Diff zobrazení | Side-by-side |
| Excel merged cells | Rozdělit a zkopírovat do sloupců |
| Excel formátování čísel | Normalizovat |
| Prázdné řádky/sloupce | Skipovat |
| Cleanup CronJob logging | Ne (neloguje do engine-data:sink-log) |
| API key rate limiting | Per klíč |
| Email provider | SMTP server |
| Email šablony | Správa v admin sekci |
| Notification types | Konfigurovatelné v aplikaci |
| Eskalace (FS20) | Jen notifikace (zatím) |
| File list endpoint | V engine-ingestor |
| Filtrování souborů | Dle typu, data, statusu |

### Otevřené – čekají na infra provisioning
- [ ] Azure Entra ID: Tenant ID, App Registration Client ID, Security Groups → role mapping
- [ ] Azure KeyVault URL a kompletní seznam secrets
- [ ] Prod PostgreSQL: Azure Database for PostgreSQL Flexible Server – provisioning
- [ ] Managed Identity setup

### Otevřené – čekají na UX/design
- [ ] Wireframy ve Figma pro klíčové obrazovky (Viewer, Dashboard, Form Builder, Report Status)
- [ ] Report Status Dashboard wireframe (matice společnosti × perioda)

### Otevřené – odložené (P6+)
- [ ] Release flow (P6): push (CompanyAdmin posílá) nebo pull (HoldingAdmin si stahuje)?
- [ ] Advanced Period Comparison (FS22): specifikace po zkušenostech z provozu
