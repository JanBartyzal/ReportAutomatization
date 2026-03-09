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

**Celkový effort:** ~193 MD (s AI asistencí ~80 MD)

| Unit ID | Function ID | Effort (MD) | AI (MD) |
|---|---|---|---|
| MS-GW | MS-GW | 1 | 1 |
| MS-CORE | MS-AUTH | 30 | 9 |
| MS-INGESTOR | MS-ING | 25 | 8 |
| MS-INGESTOR | MS-SCAN | 5 | 4 |
| MS-PROCESSOR | MS-ATM-PPTX | 35 | 16 |
| MS-DATA | MS-SINK-TBL | 12 | 5 |
| MS-DATA | MS-SINK-DOC | 10 | 5 |
| MS-DATA | MS-SINK-LOG | 5 | 2 |
| MS-N8N | MS-N8N | 25 | 10 |
| MS-FE | MS-FE | 45 | 20 |

---

### FS01 – Infrastructure & Core

---

#### Epic: API Gateway Setup (MS-GW)

**Unit ID:** MS-GW | **Effort:** 1 MD

---

##### Story: Traefik Routing & ForwardAuth

**Jako** DevOps **chci** nakonfigurovat Traefik jako API Gateway **abych** zajistil centrální vstupní bod s autentizací pro všechny služby.

**AC:**

- [ ] Traefik routuje `/api/auth/*` → MS-AUTH (port 8081)
- [ ] Traefik routuje `/api/upload/*` → MS-ING (port 8082)
- [ ] Traefik routuje `/api/files/*` → MS-QRY (port 8100, prepared for P2)
- [ ] ForwardAuth middleware volá MS-AUTH na každém requestu (kromě `/healthz`)
- [ ] Rate limiting vrací `429` po překročení limitu
- [ ] CORS headers pro frontend (localhost:3000 v dev, produkční URL v prod)
- [ ] Health check endpoint `/healthz` vrací `200`

**Tasks:**

- [ ] Vytvořit `traefik.yml` – entrypoints (HTTP :80, HTTPS :443), providers
- [ ] Dynamic routing rules (Docker labels nebo file provider)
- [ ] ForwardAuth middleware → `http://ms-auth:8000/api/auth/verify`
- [ ] Rate limiting middleware
- [ ] CORS middleware konfigurace
- [ ] SSL – self-signed cert pro lokální dev (mkcert)
- [ ] Docker Compose service definice s health check

**TODO:**

- [ ] Rate limit hodnoty: req/s per IP, burst size?
- [ ] Produkční SSL strategie: Azure Front Door, Let's Encrypt, nebo vlastní cert?
- [ ] CORS: produkční frontend URL?

**Effort:** 1 MD

---

#### Epic: Auth Service (MS-AUTH)

**Unit ID:** MS-CORE | **Effort:** 30 MD

---

##### Story: Spring Boot Base Setup

**Jako** vývojář **chci** připravit base projekt MS-AUTH **abych** měl základní strukturu pro auth službu.

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
- [ ] ForwardAuth controller – vrací headers s user context
- [ ] JWKS cache s konfigurovaným TTL
- [ ] Unit testy: platný token, expirovaný, špatný issuer, chybějící role
- [ ] Integration test s mock JWKS endpoint (WireMock)

**TODO:**

- [ ] Azure Entra ID Tenant ID
- [ ] App Registration Client ID a scope URI
- [ ] Mapování Azure AD Security Groups → interní role (která skupina = Admin/Editor/Viewer/HoldingAdmin?)
- [ ] Je vyžadováno členství v konkrétní AAD Security Group (Conditional Access)?

**Effort:** 5 MD | **Závislosti:** Base Setup

---

##### Story: RBAC Engine

**Jako** systém **chci** vyhodnotit oprávnění uživatele na základě role a organizace **abych** zajistil správné přístupové kontroly.

**AC:**

- [ ] RBAC rozlišuje role: `Admin`, `Editor`, `Viewer`, `HoldingAdmin`
- [ ] Každé oprávnění je scoped na `org_id`
- [ ] HoldingAdmin vidí data ze všech dceřiných organizací v holdingu
- [ ] API endpoint `GET /api/auth/me` vrací aktuální user context (role, organizace)
- [ ] Oprávnění vyhodnoceno při ForwardAuth i při přímém volání z jiných služeb

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
- [ ] Seed data migrace: základní role + **TODO** testovací organizace
- [ ] Unit testy: oprávnění per role, hierarchie, cross-tenant denial

**TODO:**

- [ ] Kompletní permission matice: jaké akce per role? (upload, view, edit, delete, approve, ...)
- [ ] Organizační hierarchie – kolik úrovní? (holding → dceřiná → divize?)
- [ ] Seed data pro dev: testovací organizace a uživatelé
- [ ] Má Editor vidět data jiných Editorů ve stejné organizaci?

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

**TODO:**

- [ ] Azure KeyVault URL
- [ ] Kompletní seznam secrets a jejich naming konvence v KeyVault
- [ ] MSI setup – je provisioned? Jaká identita?

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

**TODO:**

- [ ] PostgreSQL credentials pro lokální dev
- [ ] Prod: Azure Database for PostgreSQL Flexible Server, nebo self-hosted?
- [ ] Max connection pool size per service?

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

##### Story: Docker Compose Full Topology (P1)

**Jako** vývojář **chci** spustit celou P1 topologii jedním příkazem **abych** mohl lokálně vyvíjet a testovat.

**AC:**

- [ ] `docker-compose up` spustí: Traefik, MS-AUTH, MS-ING, MS-SCAN, MS-N8N, MS-ATM-PPTX, MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG, MS-FE, PostgreSQL, Redis
- [ ] Všechny služby komunikují přes interní Docker network
- [ ] `.env` soubor s konfiguratelnými porty a credentials
- [ ] Hot-reload pro FE (Vite HMR), Java (Spring DevTools), Python (uvicorn --reload)

**Tasks:**

- [ ] `docker-compose.yml` se všemi P1 službami
- [ ] `.env.example` s dokumentovanými proměnnými
- [ ] Shared Docker network konfigurace
- [ ] Volume mappings pro hot-reload (source code mounts)
- [ ] Startup ordering (depends_on + healthcheck)

**TODO:**

- [ ] Má se použít Tilt/Skaffold pro lokální dev, nebo stačí Docker Compose?
- [ ] Mají služby sdílet jednu PostgreSQL instanci (různá schémata) nebo samostatné instance?

**Effort:** 5 MD

---

### FS02 – File Ingestor

---

#### Epic: Streaming Upload & Security (MS-ING, MS-SCAN)

**Unit ID:** MS-INGESTOR | **Effort:** 30 MD (ING: 25, SCAN: 5)

---

##### Story: Spring Boot Base Setup (MS-ING)

**Jako** vývojář **chci** připravit base projekt MS-ING **abych** měl základ pro ingestor službu.

**AC:**

- [ ] Spring Boot 3.x, Java 21
- [ ] Dockerfile, Docker Compose entry (port 8082:8000, debug 5006)
- [ ] Actuator health, structured logging, OTEL prepared

**Tasks:**

- [ ] Projekt scaffolding (stejná šablona jako MS-AUTH)
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
- [ ] `org_id` a `user_id` extrakce z ForwardAuth headers (`X-Org-Id`, `X-User-Id`)
- [ ] File size limit konfigurace (`spring.servlet.multipart.max-file-size`)
- [ ] Unit testy: MIME validace (povolený typ, zakázaný typ, spoofed extension)
- [ ] Integration test: upload → Blob → DB záznam

**TODO:**

- [ ] Max file size: 20 MB? 50 MB? 100 MB?
- [ ] Blob Storage: Azure Blob, S3, nebo MinIO pro lokální dev?
- [ ] Blob naming konvence – je `{org_id}/{yyyy}/{MM}/{file_id}/` OK?
- [ ] Mají se počítat storage quotas per organizace?

**Effort:** 8 MD | **Závislosti:** MS-AUTH (ForwardAuth), PostgreSQL (migrace), Blob Storage

---

##### Story: ClamAV Security Scan (MS-SCAN)

**Jako** systém **chci** skenovat každý nahraný soubor antivirem **abych** zabránil nahrání infikovaných souborů.

**AC:**

- [ ] ClamAV sidecar/container běží vedle MS-ING
- [ ] Každý soubor skenován PŘED uložením do Blob Storage
- [ ] Infikovaný soubor → `422 { error: "INFECTED", details: "..." }`
- [ ] EICAR test virus správně detekován
- [ ] Scan timeout: max 30 s per soubor
- [ ] ClamAV nedostupný → soubor odmítnut (fail-closed princip)
- [ ] `scan_status` v DB aktualizován: `PENDING` → `CLEAN` / `INFECTED`

**Tasks:**

- [ ] Docker Compose: `clamav/clamav` container s persisted virus DB volume
- [ ] ClamAV klient v MS-ING – clamd TCP socket (port 3310)
- [ ] `ScanService.scan(InputStream)` → `ScanResult(clean/infected, detail)`
- [ ] Orchestrace: receive file → scan → if clean → upload to Blob
- [ ] Timeout handling (30 s) + fail-closed logika
- [ ] Update `scan_status` v files tabulce
- [ ] Test: EICAR virus, clean file, ClamAV timeout, ClamAV unavailable

**TODO:**

- [ ] ICAP protokol nebo clamd socket? (ICAP pro enterprise, clamd jednodušší)
- [ ] ClamAV virus DB update strategie: freshclam sidecar s cron?

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

**TODO:**

- [ ] Má se originál uchovávat trvale nebo s expirací (např. 90 dní)?
- [ ] Další typy k odstranění? ActiveX, embedded executables, external data connections?

**Effort:** 4 MD | **Závislosti:** Upload Endpoint

---

##### Story: N8N Webhook Trigger

**Jako** Ingestor **chci** po úspěšném uploadu notifikovat N8N orchestrátor **abych** spustil processing pipeline.

**AC:**

- [ ] Po úspěšném uploadu + clean scan → webhook na N8N
- [ ] Payload: `{ file_id, type, org_id, blob_url, user_id, filename }`
- [ ] Fire-and-forget: Ingestor nečeká na odpověď N8N
- [ ] Webhook doručen do 1 s od uložení
- [ ] Selhání webhooku → log warning, soubor zůstane `UPLOADED` (N8N může pollovat)
- [ ] `processing_status` aktualizován na `PROCESSING`

**API (odchozí):**

```
POST http://ms-n8n:5678/webhook/new-file
Content-Type: application/json
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

- [ ] N8N webhook URL – env variable `N8N_WEBHOOK_URL`
- [ ] Async HTTP client (WebClient non-blocking) pro webhook call
- [ ] Fire-and-forget: `.subscribe()` bez čekání na response
- [ ] Error handling: log warning, neretryovat (N8N má vlastní retry)
- [ ] Update `processing_status` → `PROCESSING` v DB

**Effort:** 2 MD | **Závislosti:** Upload Endpoint, N8N Webhook Listener

---

### FS03 – PPTX Atomizer

---

#### Epic: PPTX Extraction Engine (MS-ATM-PPTX)

**Unit ID:** MS-PROCESSOR | **Effort:** 35 MD

---

##### Story: FastAPI Base Setup (MS-ATM-PPTX)

**Jako** vývojář **chci** připravit base projekt PPTX Atomizeru **abych** měl základ pro extraction službu.

**AC:**

- [ ] Python 3.11+, FastAPI, Pydantic v2, uvicorn
- [ ] Dockerfile s python:3.11-slim base image
- [ ] Health check endpoint `/healthz`
- [ ] Structured JSON logging
- [ ] Docker Compose entry (port 8090:8000, debug 5678)

**Tasks:**

- [ ] Projekt scaffolding: `pyproject.toml` (poetry/pip), FastAPI app
- [ ] Dockerfile (multi-stage: dependencies → runtime)
- [ ] Dependencies: `python-pptx`, `Pillow`, `httpx`, `pydantic`
- [ ] Config: environment variables (BLOB_STORAGE_URL, etc.)
- [ ] Docker Compose entry

**Effort:** 1 MD

---

##### Story: PPTX Structure Extraction

**Jako** N8N Orchestrátor **chci** získat strukturu PPTX souboru **abych** věděl, kolik slidů soubor obsahuje a jak je zpracovat.

**AC:**

- [ ] `POST /extract/pptx` přijímá `{ file_id, blob_url }`
- [ ] Atomizer si stahuje soubor z Blob Storage sám (přes `blob_url`)
- [ ] Vrací JSON seznam slidů s metadaty
- [ ] Chybný soubor vrací `422` s detailem, nikdy `500`

**API:**

```
POST /extract/pptx
{
  "file_id": "uuid",
  "blob_url": "https://storage.blob.core.windows.net/files/..."
}

→ 200 OK
{
  "file_id": "uuid",
  "slide_count": 15,
  "slides": [
    {
      "slide_id": 1,
      "title": "Q2 2025 OPEX Report",
      "layout": "Title Slide",
      "has_tables": false,
      "has_text": true,
      "has_images": true,
      "has_notes": true
    },
    ...
  ]
}

→ 422 Unprocessable Entity
{ "error": "INVALID_PPTX", "detail": "File is not a valid PPTX" }
```

**Tasks:**

- [ ] `BlobDownloader` – async stažení souboru z Blob URL (`httpx`)
- [ ] `PptxParser.get_structure(file_path)` – python-pptx, iterace přes slidy
- [ ] Extrakce metadat per slide: title (first text frame), layout name, detection tabulek/textu/obrázků
- [ ] Pydantic response model
- [ ] Error handling: `try/except` → `422` s popisem chyby
- [ ] Dočasný soubor – stáhnout do `/tmp`, po zpracování smazat
- [ ] Unit testy: validní PPTX, prázdný PPTX, poškozený soubor

**Effort:** 5 MD | **Závislosti:** Blob Storage

---

##### Story: Slide Content Extraction (Text + Tables)

**Jako** N8N Orchestrátor **chci** extrahovat texty a tabulky z konkrétního slidu **abych** je mohl uložit do DB.

**AC:**

- [ ] `POST /extract/pptx/slide` přijímá `{ file_id, blob_url, slide_id }`
- [ ] Extrahuje všechny textové rámce, tabulky a poznámky ze slidu
- [ ] Tabulky strukturovány jako `{ headers: [...], rows: [[...], ...] }`
- [ ] Nikdy nevrací inline binary data – pouze JSON

**API:**

```
POST /extract/pptx/slide
{
  "file_id": "uuid",
  "blob_url": "https://...",
  "slide_id": 3
}

→ 200 OK
{
  "file_id": "uuid",
  "slide_id": 3,
  "texts": [
    { "shape_name": "Title 1", "content": "IT Costs Overview" },
    { "shape_name": "Content Placeholder 2", "content": "Total OPEX: 1.2M CZK" }
  ],
  "tables": [
    {
      "table_id": 1,
      "headers": ["Category", "Q1", "Q2", "Delta"],
      "rows": [
        ["Hardware", "450000", "520000", "+15.6%"],
        ["Software", "320000", "310000", "-3.1%"]
      ]
    }
  ],
  "notes": "Speaker notes text here..."
}
```

**Tasks:**

- [ ] `PptxParser.extract_slide(file_path, slide_id)` – iterace přes shapes
- [ ] Text extraction: `shape.text_frame.paragraphs` → concatenated text per shape
- [ ] Table extraction: `shape.table` → headers (first row) + data rows
- [ ] Notes extraction: `slide.notes_slide.notes_text_frame`
- [ ] Handling: slide_id mimo rozsah → `422`
- [ ] Unit testy: slide s textem, slide s tabulkou, slide s notes, prázdný slide

**Effort:** 6 MD | **Závislosti:** Structure Extraction

---

##### Story: Slide Image Rendering

**Jako** N8N Orchestrátor **chci** vyrenderovat slide jako PNG obrázek **abych** mohl zobrazit náhled ve frontend vieweru.

**AC:**

- [ ] `POST /extract/pptx/slide/image` renderuje slide jako PNG 800×600
- [ ] PNG uložen do Blob Storage, vrací `{ artifact_url }`
- [ ] Nikdy nevrací binary data inline v JSON response

**API:**

```
POST /extract/pptx/slide/image
{
  "file_id": "uuid",
  "blob_url": "https://...",
  "slide_id": 3
}

→ 200 OK
{
  "file_id": "uuid",
  "slide_id": 3,
  "artifact_url": "https://storage.blob.core.windows.net/artifacts/uuid/slide_3.png",
  "width": 800,
  "height": 600
}
```

**Tasks:**

- [ ] Slide → PNG rendering engine (LibreOffice headless nebo python-pptx + Pillow)
- [ ] Upload PNG do Blob Storage (`artifacts/{file_id}/slide_{n}.png`)
- [ ] Vrácení `artifact_url` v response
- [ ] Cleanup: smazání lokálního PNG po uploadu
- [ ] Unit test: renderování, ověření rozměrů PNG

**TODO:**

- [ ] Rendering engine: LibreOffice headless (`libreoffice --convert-to png`) nebo čistý Python (omezenější kvalita)?
- [ ] Rozlišení PNG: 800×600, nebo konfigurovatelné?
- [ ] Artifact retention policy: kolik dní uchovávat PNG? (MS-ATM-CLN maže po 24h)

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
- [ ] Confidence score: pokud < threshold → vrátit jako plain text
- [ ] Unit testy: tab-separated text, space-aligned text, mixed content

**TODO:**

- [ ] Confidence threshold pro MetaTable detection: jaká minimální jistota?
- [ ] Má se MetaTable logic volat vždy, nebo jen na request? (flag `detect_meta_tables: true`)

**Effort:** 8 MD | **Závislosti:** Slide Content Extraction

---

##### Story: Error Handling & Resilience

**Jako** Atomizer **chci** robustně zpracovat chybné soubory **abych** nikdy nevrátil `500` a vždy dal smysluplnou chybovou hlášku.

**AC:**

- [ ] Chybný/poškozený PPTX → `422` s `{ error: "INVALID_PPTX", detail: "..." }`
- [ ] Blob URL nedostupný → `422` s `{ error: "BLOB_UNAVAILABLE", detail: "..." }`
- [ ] Timeout na stažení souboru → `422` s `{ error: "DOWNLOAD_TIMEOUT" }`
- [ ] Memory limit: soubory > N MB odmítnuty
- [ ] Cleanup: dočasné soubory v `/tmp` vždy smazány (i při chybě)

**Tasks:**

- [ ] Global exception handler (FastAPI `@app.exception_handler`)
- [ ] Structured error response model (`ErrorResponse(error, detail, file_id)`)
- [ ] Download timeout konfigurace (env variable)
- [ ] File size check před parsováním
- [ ] `finally` blok pro cleanup dočasných souborů
- [ ] Unit testy: každý error scenario

**TODO:**

- [ ] Max file size pro Atomizer (odlišný od upload limitu?)
- [ ] Download timeout v sekundách?

**Effort:** 3 MD

---

### FS04 – N8N Orchestrator

---

#### Epic: Processing Pipeline (MS-N8N)

**Unit ID:** MS-N8N | **Effort:** 25 MD

---

##### Story: N8N Instance Setup

**Jako** DevOps **chci** nakonfigurovat N8N instanci **abych** měl workflow engine pro orchestraci processing pipeline.

**AC:**

- [ ] N8N běží v Docker kontejneru (port 5678)
- [ ] Workflow data persistována v PostgreSQL (ne SQLite)
- [ ] Webhook listener aktivní na `http://ms-n8n:5678/webhook/*`
- [ ] N8N credentials uloženy bezpečně (encrypted v DB)
- [ ] N8N UI přístupné pouze z interní sítě (ne přes API Gateway)

**Tasks:**

- [ ] Docker Compose: N8N container s PostgreSQL backend
- [ ] Environment variables: `DB_TYPE=postgresdb`, `DB_POSTGRESDB_*`
- [ ] Webhook listener konfigurace (`WEBHOOK_URL`)
- [ ] N8N credentials pro HTTP nodes (Atomizer URLs, Sink URLs)
- [ ] Disable public access k N8N UI (bind na internal network)

**TODO:**

- [ ] N8N authentication: basic auth nebo OAuth pro admin UI?
- [ ] Workflow version control: export JSON do Git nebo N8N native?

**Effort:** 3 MD

---

##### Story: PPTX Processing Workflow

**Jako** N8N **chci** zpracovat nově nahraný PPTX soubor end-to-end **abych** extrahoval data a uložil je do DB.

**Workflow:**

```
Webhook (new_file)
  → Validate file_type == "pptx"
  → HTTP: POST ms-atm-pptx/extract/pptx (get structure)
  → Split In Batches: iterate over slides (max 5 parallel)
    → HTTP: POST ms-atm-pptx/extract/pptx/slide (get content)
    → HTTP: POST ms-atm-pptx/extract/pptx/slide/image (get PNG)
    → IF has_tables:
        → HTTP: POST ms-sink-tbl/tables/{org_id}/{batch_id} (store table data)
    → IF has_text:
        → HTTP: POST ms-sink-doc/documents/{org_id} (store text + embeddings)
    → HTTP: POST ms-sink-log/logs/{file_id} (log step result)
  → On Complete:
    → Update file processing_status → DONE
    → [Future: POST ms-notif → notify user]
```

**AC:**

- [ ] Upload nového PPTX automaticky spustí workflow bez manuálního zásahu
- [ ] Každý slide zpracován nezávisle (partial success)
- [ ] Tabulky → MS-SINK-TBL, texty → MS-SINK-DOC, logy → MS-SINK-LOG
- [ ] Processing status aktualizován v DB: `PROCESSING` → `DONE` / `PARTIAL` / `FAILED`
- [ ] Max 5 paralelních volání Atomizeru

**Tasks:**

- [ ] Vytvořit N8N workflow JSON: `pptx-processing-pipeline.json`
- [ ] Webhook trigger node: `POST /webhook/new-file`
- [ ] File type router (Switch node): PPTX → this workflow, others → TBD
- [ ] HTTP Request node: call MS-ATM-PPTX `/extract/pptx`
- [ ] Split In Batches node: iterace přes `slides[]` (batch size: 5)
- [ ] HTTP Request nodes: `/extract/pptx/slide`, `/extract/pptx/slide/image`
- [ ] Condition nodes: `has_tables` → Sink TBL, `has_text` → Sink DOC
- [ ] HTTP Request nodes: call MS-SINK-TBL, MS-SINK-DOC, MS-SINK-LOG
- [ ] Status update node: HTTP PATCH na MS-ING nebo přímý DB update
- [ ] Export workflow jako JSON do Git

**Effort:** 8 MD | **Závislosti:** MS-ATM-PPTX, MS-SINK-*, N8N Setup

---

##### Story: Error Handling, Retry & DLQ

**Jako** N8N **chci** správně zpracovat selhání Atomizerů **abych** neztratil data a umožnil manuální reprocessing.

**AC:**

- [ ] Automatický retry: 3× s exponential backoff (1s, 5s, 25s)
- [ ] Circuit breaker: po 5 selháních Atomizeru pozastavit workflow
- [ ] Fatální selhání → záznam v `failed_jobs` tabulce
- [ ] Admin může zobrazit failed_jobs a spustit reprocessing

**DB:**

```sql
-- V003__create_failed_jobs_table.sql
CREATE TABLE failed_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_id UUID NOT NULL REFERENCES files(id),
    workflow_name VARCHAR(255) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    error_message TEXT,
    error_stacktrace TEXT,
    retry_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'FAILED',  -- FAILED, REPROCESSING, RESOLVED
    created_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP
);
```

**Tasks:**

- [ ] N8N Error Trigger node → catch workflow errors
- [ ] Retry konfigurace na HTTP Request nodes (3×, exponential backoff)
- [ ] Error workflow: uložení chyby do `failed_jobs` přes MS-SINK-LOG
- [ ] Circuit breaker logika: IF Node s počítadlem selhání
- [ ] Flyway migrace V003 – failed_jobs tabulka
- [ ] Endpoint pro reprocessing: re-trigger webhook s `file_id`

**Effort:** 5 MD | **Závislosti:** PPTX Processing Workflow

---

##### Story: Idempotence

**Jako** systém **chci** garantovat idempotenci zpracování **abych** neměl duplicitní záznamy v DB při opětovném spuštění workflow.

**AC:**

- [ ] Opětovné spuštění workflow pro stejné `file_id` nevytvoří duplicitní záznamy
- [ ] Detekce: before insert → check `file_id` + `slide_id` existence
- [ ] Existující záznamy přepsány (upsert), ne duplikovány

**Tasks:**

- [ ] Upsert logika v MS-SINK-TBL a MS-SINK-DOC (ON CONFLICT DO UPDATE)
- [ ] N8N workflow: check node – je file_id již zpracováno?
- [ ] Processing status check: pokud `DONE` → skip (nebo force flag)

**Effort:** 3 MD | **Závislosti:** PPTX Processing Workflow

---

##### Story: File Type Router (Prepared for P2)

**Jako** N8N **chci** routovat soubory dle typu na správný workflow **abych** v P2 mohl přidat Excel, PDF a CSV parsování.

**AC:**

- [ ] Router node: `type == "pptx"` → PPTX workflow
- [ ] Ostatní typy: `type == "xlsx|pdf|csv"` → placeholder node s logem "Not implemented yet"
- [ ] Neznámý typ → error + záznam do failed_jobs

**Tasks:**

- [ ] Switch node v N8N workflow
- [ ] Placeholder workflows pro xlsx, pdf, csv (prázdné, jen log)

**Effort:** 1 MD

---

### FS05 – Sinks (Persistence)

---

#### Epic: Table API (MS-SINK-TBL)

**Unit ID:** MS-DATA | **Effort:** 12 MD

---

##### Story: Spring Boot Base Setup (MS-SINK-TBL)

*Identická šablona jako MS-AUTH base setup. Port 8100:8080, debug 5005.*

**Effort:** 1 MD

---

##### Story: Bulk Insert Endpoint

**Jako** N8N Orchestrátor **chci** uložit strukturovaná tabulková data do PostgreSQL **abych** je měl k dispozici pro dashboardy a query.

**AC:**

- [ ] `POST /tables/{org_id}/{batch_id}` přijímá JSON tabulková data
- [ ] Data uložena jako JSONB v PostgreSQL
- [ ] RLS policy: uživatel vidí pouze záznamy svého `org_id`
- [ ] Upsert: duplicitní `file_id + slide_id + table_id` přepíše existující záznam
- [ ] Bulk insert: až 100 řádků v jednom requestu

**API:**

```
POST /tables/{org_id}/{batch_id}
{
  "file_id": "uuid",
  "slide_id": 3,
  "table_id": 1,
  "source_type": "FILE",
  "headers": ["Category", "Q1", "Q2", "Delta"],
  "rows": [
    ["Hardware", "450000", "520000", "+15.6%"],
    ["Software", "320000", "310000", "-3.1%"]
  ],
  "metadata": {
    "filename": "report_q2.pptx",
    "slide_title": "IT Costs Overview"
  }
}

→ 201 Created
{ "record_id": "uuid", "rows_inserted": 2 }

→ 409 Conflict (if duplicate and no upsert flag)
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

- [ ] Flyway migrace V004 – table_data tabulka s RLS
- [ ] REST controller `POST /tables/{org_id}/{batch_id}`
- [ ] `TableDataRepository` (Spring Data JPA)
- [ ] Upsert logika (ON CONFLICT DO UPDATE)
- [ ] RLS enforcement: `SET app.current_org_id = ?` před každým dotazem
- [ ] Request validation (Pydantic-style: headers a rows povinné)
- [ ] Unit testy: insert, upsert, RLS isolation
- [ ] Integration test: dva org_id nesmí vidět data druhého

**Effort:** 7 MD | **Závislosti:** PostgreSQL, Flyway

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

#### Epic: Document API (MS-SINK-DOC)

**Unit ID:** MS-DATA | **Effort:** 10 MD

---

##### Story: Document Storage Endpoint

**Jako** N8N Orchestrátor **chci** uložit nestrukturované texty z prezentací **abych** je měl k dispozici pro full-text a sémantické vyhledávání.

**AC:**

- [ ] `POST /documents/{org_id}` přijímá JSON s textem a metadaty
- [ ] Data uložena do PostgreSQL (JSONB + full-text index)
- [ ] RLS policy per org_id
- [ ] Response: `{ document_id }`

**API:**

```
POST /documents/{org_id}
{
  "file_id": "uuid",
  "slide_id": 3,
  "content": "Total OPEX for Q2 2025 reached 1.2M CZK...",
  "content_type": "slide_text",
  "metadata": {
    "filename": "report_q2.pptx",
    "slide_title": "IT Costs Overview"
  }
}

→ 201 Created
{ "document_id": "uuid" }
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
    embedding vector(1536),  -- pgVector, filled async by MS-ATM-AI
    created_at TIMESTAMP DEFAULT NOW()
);

ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY documents_org_isolation ON documents
    USING (org_id = current_setting('app.current_org_id')::UUID);

CREATE INDEX idx_documents_org ON documents(org_id);
CREATE INDEX idx_documents_content_fts ON documents USING gin(to_tsvector('simple', content));
```

**Tasks:**

- [ ] Flyway migrace V005 – documents tabulka s RLS a FTS index
- [ ] REST controller `POST /documents/{org_id}`
- [ ] `DocumentRepository` (Spring Data JPA)
- [ ] RLS enforcement
- [ ] `embedding` sloupec zatím NULL – bude plněn v P2 (MS-ATM-AI)
- [ ] Unit testy

**Effort:** 5 MD | **Závislosti:** PostgreSQL, Flyway

---

##### Story: Vector Embedding Pipeline (Prepared)

**Jako** systém **chci** připravit infrastrukturu pro vector embeddings **abych** je mohl v P2 plnit přes MS-ATM-AI.

**AC:**

- [ ] pgVector extension nainstalována a funkční
- [ ] `embedding` sloupec typu `vector(1536)` existuje v documents tabulce
- [ ] Index HNSW nebo IVFFlat připraven (ale prázdný)

**Tasks:**

- [ ] Ověřit pgVector extension v PostgreSQL
- [ ] `CREATE INDEX idx_documents_embedding ON documents USING hnsw (embedding vector_cosine_ops);`
- [ ] Placeholder service: `EmbeddingService.generateEmbedding()` → throws "Not implemented in P1"

**TODO:**

- [ ] Embedding model: OpenAI `text-embedding-ada-002` (1536 dim) nebo jiný?
- [ ] Embedding dimension: 1536 (OpenAI) nebo 768 (open-source)?

**Effort:** 2 MD

---

#### Epic: Log API (MS-SINK-LOG)

**Unit ID:** MS-DATA | **Effort:** 5 MD

---

##### Story: Append-Only Processing Log

**Jako** N8N Orchestrátor **chci** logovat každý krok zpracování souboru **abych** měl audit trail pro debugging a monitoring.

**AC:**

- [ ] `POST /logs/{file_id}` přidává záznam do processing logu
- [ ] Append-only: žádné UPDATE ani DELETE
- [ ] Záznamy: `step_name`, `status`, `duration_ms`, `error_detail`
- [ ] `GET /logs/{file_id}` vrací chronologický seznam kroků

**API:**

```
POST /logs/{file_id}
{
  "step_name": "extract_slide_3",
  "status": "SUCCESS",
  "duration_ms": 1250,
  "error_detail": null,
  "metadata": { "slide_id": 3, "tables_found": 2 }
}

→ 201 Created
{ "log_id": "uuid" }

GET /logs/{file_id}

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

- [ ] Flyway migrace V006 – processing_logs tabulka
- [ ] REST controller: POST (append), GET (read by file_id)
- [ ] App user permissions: INSERT + SELECT only
- [ ] Unit testy

**Effort:** 3 MD | **Závislosti:** PostgreSQL

---

### FS09 – Frontend SPA (Basic)

---

#### Epic: Auth & Shell (MS-FE)

**Unit ID:** MS-FE | **Effort:** 10 MD

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

**TODO:**

- [ ] UI component library: Shadcn/UI + Radix, nebo jiná?
- [ ] Monorepo přístup (turborepo) nebo standalone?

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

**TODO:**

- [ ] Azure App Registration Client ID
- [ ] Redirect URI pro lokální dev a produkci
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

**TODO:**

- [ ] UI design / wireframe pro layout?
- [ ] Barevné schéma a branding?
- [ ] Dark mode v P1 nebo později?

**Effort:** 3 MD

---

#### Epic: Upload Manager (MS-FE)

**Unit ID:** MS-FE | **Effort:** 12 MD

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

**Effort:** 4 MD | **Závislosti:** MSAL Auth, MS-ING Upload Endpoint

---

##### Story: File List View

**Jako** Editor **chci** vidět seznam nahraných souborů **abych** měl přehled o stavu zpracování.

**AC:**

- [ ] Tabulka: filename, size, upload date, processing status, actions
- [ ] Status badge: `UPLOADED` (šedá), `PROCESSING` (žlutá), `DONE` (zelená), `FAILED` (červená), `PARTIAL` (oranžová)
- [ ] Řazení dle data uploadu (nejnovější nahoře)
- [ ] Klik na soubor → přechod na Viewer

**API (čtení z MS-ING nebo MS-QRY):**

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

- [ ] Backend: `GET /api/files` endpoint v MS-ING (read from files table)
- [ ] FE: `FileList` komponenta s TanStack Table
- [ ] Status badge komponenta
- [ ] Pagination
- [ ] React Query hook: `useFiles(orgId, page)`
- [ ] Link na Viewer stránku

**TODO:**

- [ ] Má být file list endpoint v MS-ING nebo v MS-QRY (CQRS read side)?
- [ ] Filtrování: dle typu souboru, dle data, dle statusu?

**Effort:** 4 MD | **Závislosti:** Upload, Backend endpoint

---

#### Epic: Basic Viewer (MS-FE)

**Unit ID:** MS-FE | **Effort:** 10 MD

---

##### Story: Slide-by-Slide Viewer

**Jako** Viewer **chci** prohlížet parsovaná data z PPTX prezentace slide po slidu **abych** ověřil správnost extrakce.

**AC:**

- [ ] Stránka `/files/{file_id}` zobrazuje detail souboru
- [ ] Levý panel: thumbnail navigace (miniatura slidů)
- [ ] Hlavní panel: vybraný slide – PNG preview + extrahované texty + tabulky
- [ ] Tabulky zobrazeny jako HTML tabulky (read-only)
- [ ] Processing log: timeline kroků zpracování (ze MS-SINK-LOG)
- [ ] Read-only: žádné editace v P1

**Tasks:**

- [ ] Route `/files/:fileId` s `FileViewer` komponentou
- [ ] Slide thumbnail navigace (vertikální list miniatur)
- [ ] Slide detail: PNG obrázek z `artifact_url`
- [ ] Extrahované texty: zobrazení per shape
- [ ] Extrahované tabulky: `<table>` komponenta
- [ ] Processing log timeline (GET `/logs/{file_id}`)
- [ ] React Query hooks: `useFileDetail(fileId)`, `useSlideData(fileId, slideId)`, `useProcessingLog(fileId)`

**TODO:**

- [ ] Wireframe / mockup pro Viewer layout?
- [ ] Má viewer zobrazovat originální slide (PNG) a extrahovaná data vedle sebe, nebo pod sebou?
- [ ] Má uživatel vidět notes (poznámky ke slidu)?

**Effort:** 8 MD | **Závislosti:** MS-ATM-PPTX, MS-SINK-TBL, MS-SINK-LOG

---

#### Epic: Real-time Feedback (MS-FE)

**Unit ID:** MS-FE | **Effort:** 5 MD

---

##### Story: Processing Status Updates

**Jako** Editor **chci** vidět aktuální stav zpracování souboru v reálném čase **abych** věděl, kdy je soubor hotový.

**AC:**

- [ ] Po uploadu: status se aktualizuje automaticky (polling nebo WebSocket/SSE)
- [ ] Status přechody viditelné v UI bez refresh: `UPLOADED → PROCESSING → DONE/FAILED`
- [ ] Notifikace (toast) při dokončení zpracování

**Tasks:**

- [ ] Varianta A (polling): React Query s `refetchInterval: 3000` pro aktivní soubory
- [ ] Varianta B (SSE): EventSource endpoint v backendu + FE listener
- [ ] Toast notifikace při přechodu do `DONE` nebo `FAILED`
- [ ] Indikátor zpracování: `Processing slide 3/15...`

**TODO:**

- [ ] Polling nebo SSE/WebSocket v P1? (polling jednodušší, SSE lepší UX)
- [ ] Pokud SSE: který backend service bude SSE endpoint hostovat?

**Effort:** 5 MD | **Závislosti:** Upload, File List

---

### P1 – Souhrn Stories

| # | Epic / Story | Function ID | Effort | Závislosti |
|---|---|---|---|---|
| 1 | Traefik Routing & ForwardAuth | MS-GW | 1 MD | — |
| 2 | Spring Boot Base Setup (AUTH) | MS-AUTH | 2 MD | — |
| 3 | Azure Entra ID Token Validation | MS-AUTH | 5 MD | #2 |
| 4 | RBAC Engine | MS-AUTH | 8 MD | #3 |
| 5 | KeyVault Integration | MS-AUTH | 2 MD | #2 |
| 6 | PostgreSQL & Flyway Setup | MS-AUTH | 3 MD | — |
| 7 | Redis Connection Setup | MS-AUTH | 2 MD | — |
| 8 | Docker Compose Full Topology | infra | 5 MD | — |
| 9 | Spring Boot Base Setup (ING) | MS-ING | 1 MD | — |
| 10 | Streaming Upload Endpoint | MS-ING | 8 MD | #3, #6 |
| 11 | ClamAV Security Scan | MS-SCAN | 5 MD | #10 |
| 12 | File Sanitization | MS-ING | 4 MD | #10 |
| 13 | N8N Webhook Trigger | MS-ING | 2 MD | #10, #17 |
| 14 | FastAPI Base Setup (ATM-PPTX) | MS-ATM-PPTX | 1 MD | — |
| 15 | PPTX Structure Extraction | MS-ATM-PPTX | 5 MD | #14 |
| 16 | Slide Content Extraction | MS-ATM-PPTX | 6 MD | #15 |
| 17 | Slide Image Rendering | MS-ATM-PPTX | 6 MD | #15 |
| 18 | MetaTable Logic | MS-ATM-PPTX | 8 MD | #16 |
| 19 | Error Handling & Resilience (ATM) | MS-ATM-PPTX | 3 MD | #15 |
| 20 | N8N Instance Setup | MS-N8N | 3 MD | #6 |
| 21 | PPTX Processing Workflow | MS-N8N | 8 MD | #15, #16, #17, #25, #26, #27 |
| 22 | Error Handling, Retry & DLQ | MS-N8N | 5 MD | #21 |
| 23 | Idempotence | MS-N8N | 3 MD | #21 |
| 24 | File Type Router | MS-N8N | 1 MD | #20 |
| 25 | Bulk Insert Endpoint (SINK-TBL) | MS-SINK-TBL | 7 MD | #6 |
| 26 | RLS Integration Test | MS-SINK-TBL | 3 MD | #25 |
| 27 | Document Storage Endpoint | MS-SINK-DOC | 5 MD | #6 |
| 28 | Vector Embedding Pipeline (prepared) | MS-SINK-DOC | 2 MD | #27 |
| 29 | Append-Only Processing Log | MS-SINK-LOG | 3 MD | #6 |
| 30 | React Project Setup | MS-FE | 2 MD | — |
| 31 | MSAL Authentication | MS-FE | 5 MD | #30 |
| 32 | App Shell & Navigation | MS-FE | 3 MD | #31 |
| 33 | Drag & Drop Upload Zone | MS-FE | 4 MD | #31, #10 |
| 34 | File List View | MS-FE | 4 MD | #31 |
| 35 | Slide-by-Slide Viewer | MS-FE | 8 MD | #34, #16, #17, #29 |
| 36 | Processing Status Updates | MS-FE | 5 MD | #33, #34 |

---

## Phase 2 – Extended Parsing

**Cíl:** Plná podpora formátů (Excel, PDF, CSV), cleanup worker, CQRS read model, základní dashboardy.

**Scope:** FS03-rest, FS10, FS06

**Effort:** ~86 MD

---

### FS03 – Remaining Atomizers

---

#### Epic: Excel Atomizer (MS-ATM-XLS)

**Unit ID:** MS-PROCESSOR | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | FastAPI Base Setup | 1 MD | Stejná šablona jako MS-ATM-PPTX |
| 2 | Sheet Structure Extraction | 3 MD | `POST /extract/excel` → list listů s metadaty (row_count, col_count) |
| 3 | Sheet Content Extraction | 5 MD | `POST /extract/excel/sheet` → headers + rows + data_types per sheet |
| 4 | Partial Success Handling | 3 MD | 9/10 listů OK + 1 FAILED → `{ status: "PARTIAL", successful: [...], failed: [...] }` |
| 5 | N8N Excel Workflow | 3 MD | Workflow pro batch iteraci přes listy, routing na SINK-TBL |

**TODO:**

- [ ] Má Excel Atomizer detekovat merged cells a jak je zpracovat?
- [ ] Formátování čísel: zachovat formát z Excelu nebo normalizovat?
- [ ] Prázdné řádky/sloupce: skipovat nebo zachovat?

---

#### Epic: PDF/OCR Atomizer (MS-ATM-PDF)

**Unit ID:** MS-PROCESSOR | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | FastAPI Base Setup | 1 MD | Base setup + Tesseract OCR v Dockerfile |
| 2 | PDF Type Detection | 2 MD | Detekce: textový PDF vs. skenovaný (image-based) |
| 3 | Text PDF Extraction | 4 MD | PyPDF2/pdfplumber: extrakce textu, tabulek, metadat |
| 4 | OCR Pipeline | 5 MD | Tesseract OCR pro skenované stránky → text |
| 5 | N8N PDF Workflow | 3 MD | Workflow: detect type → route → extract → store |

**TODO:**

- [ ] OCR jazyky: čeština + angličtina, nebo více?
- [ ] Tesseract: přetrénovaný model pro finanční dokumenty?
- [ ] PDF tabulky: Camelot/Tabula integrace?

---

#### Epic: CSV Atomizer (MS-ATM-CSV)

**Unit ID:** MS-PROCESSOR | **Effort:** 4 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | CSV Parser | 2 MD | Auto-detection: oddělovač (`,;|\t`), kódování (UTF-8, CP1250), header detection |
| 2 | N8N CSV Workflow | 2 MD | Workflow: parse → store |

---

#### Epic: Cleanup Worker (MS-ATM-CLN)

**Unit ID:** MS-PROCESSOR | **Effort:** 5 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Cleanup CronJob | 3 MD | Smazání dočasných artefaktů (PNG, CSV) z Blob starších než 24h |
| 2 | Cleanup Konfigurace | 2 MD | Konfigurovatelná retention policy per artifact type |

**TODO:**

- [ ] Retention policy: 24h pro PNG slidy, jiná doba pro jiné typy?
- [ ] Má CronJob logovat smazané soubory do MS-SINK-LOG?

---

### FS06 – Analytics & Query (CQRS Read)

---

#### Epic: Query API (MS-QRY)

**Unit ID:** MS-DATA | **Effort:** 12 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Spring Boot Base Setup | 1 MD | Base setup, Redis client |
| 2 | File Detail Endpoint | 3 MD | `GET /api/files/{id}` – kompletní detail souboru s parsovanými daty |
| 3 | Table Data Query | 4 MD | `GET /api/tables?org_id=&file_id=&slide_id=` – dotazy nad JSONB |
| 4 | Redis Caching Layer | 2 MD | Cache s TTL (5 min) pro nejčastější dotazy |
| 5 | Materialized Views | 2 MD | Flyway migrace: precomputed views pro dashboard aggregace |

---

#### Epic: Dashboard Aggregation (MS-DASH)

**Unit ID:** MS-DATA | **Effort:** 35 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Spring Boot Base Setup | 1 MD | Base setup |
| 2 | Aggregation Engine | 8 MD | SQL nad JSONB: GROUP BY, ORDER BY, filtr datum/org z UI parametrů |
| 3 | Dashboard Configuration API | 5 MD | CRUD pro definice dashboardů (JSON config: zdroj dat, typ grafu, filtry) |
| 4 | Chart Data Endpoints | 8 MD | Endpointy vracející data pro grafy: bar, line, pie, table |
| 5 | FE: Dashboard Viewer | 8 MD | React komponenty: chart library integrace, interaktivní filtry, drill-down |
| 6 | FE: Dashboard Builder (Admin) | 5 MD | Admin UI pro konfiguraci dashboardů |

**TODO:**

- [ ] Chart library: Recharts, Nivo, nebo ECharts?
- [ ] SQL editor pro pokročilé uživatele – v P2 nebo později?
- [ ] Jaké výchozí dashboardy vytvořit? (OPEX přehled per org, trend, srovnání)

---

### FS10 – Excel Parsing Logic

*Zahrnut v Epic MS-ATM-XLS výše. Partial success logika a datová kompatibilita s PPTX data (JSONB format) pokryta v stories #3 a #4.*

---

## Phase 3a – Intelligence & Admin

**Cíl:** Holdingová hierarchie, AI integrace, Schema Mapping s learning.

**Scope:** FS07, FS08, FS12, FS15

**Effort:** ~90 MD

---

### FS07 – Admin Backend & UI

#### Epic: Admin Backend (MS-ADMIN)

**Unit ID:** MS-CORE | **Effort:** 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Role Management API | 5 MD | CRUD pro přiřazení rolí uživatelům per organizace |
| 2 | Organization Hierarchy API | 4 MD | CRUD pro holding → dceřiné společnosti, hierarchická navigace |
| 3 | Failed Jobs UI Backend | 3 MD | `GET /admin/failed-jobs`, `POST /admin/failed-jobs/{id}/reprocess` |
| 4 | API Key Management | 4 MD | Generování, hashování (bcrypt), revokace service account klíčů |
| 5 | FE: Admin Section | 4 MD | React stránky pro role, organizace, failed jobs, API keys |

**TODO:**

- [ ] Má Admin UI být součást hlavní SPA nebo separátní aplikace?
- [ ] Invitation flow: jak se přidají noví uživatelé? (Azure AD sync nebo manuální pozvánka?)
- [ ] API key rate limiting: per klíč nebo per organizace?

---

### FS08 – Batch & Org Management

#### Epic: Batch & Org Service (MS-BATCH)

**Unit ID:** MS-CORE | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Batch Management API | 5 MD | CRUD pro batch (reporting period tag), přiřazení souborů k batchi |
| 2 | Organization Metadata | 4 MD | Holdingová metadata per soubor (`holding_id`, `company_id`, `uploaded_by`) |
| 3 | RLS Enforcement Layer | 3 MD | Sdílený middleware pro automatické nastavení `app.current_org_id` |
| 4 | FE: Batch Dashboard | 3 MD | Přehled batchů, stav souborů v batchi |

**TODO:**

- [ ] Vztah Batch vs. Reporting Period (FS20): sloučení konceptů nebo dva různé objekty?
- [ ] Granularita org metadata: stačí `org_id` nebo potřeba `division_id`, `cost_center_id`?

---

### FS12 – AI Integration (MCP)

#### Epic: AI Gateway (MS-ATM-AI)

**Unit ID:** MS-AI | **Effort:** 13 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | LiteLLM Integration | 4 MD | FastAPI endpoint `POST /analyze/semantic`, LiteLLM wrapper pro multi-model |
| 2 | Token Quota & Cost Control | 3 MD | Tracking spotřeby tokenů per user/org, `429` při překročení kvóty |
| 3 | Vector Embedding Generator | 3 MD | Async generování embeddings pro documents v MS-SINK-DOC |
| 4 | Prompt Templates | 3 MD | Konfigurovatelné prompty pro klasifikaci, sumarizaci, extrakci entit |

**TODO:**

- [ ] LLM provider: Azure OpenAI, OpenAI API, nebo open-source (Ollama)?
- [ ] Model pro embeddings: `text-embedding-ada-002` nebo `text-embedding-3-small`?
- [ ] Měsíční token quota per organizace: jaká výchozí hodnota?

#### Epic: MCP Server (MS-MCP)

**Unit ID:** MS-AI | **Effort:** 12 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | MCP Server Setup | 4 MD | FastAPI MCP server, OAuth On-Behalf-Of flow |
| 2 | Data Query Tools | 4 MD | MCP tools pro dotazování nad table_data a documents |
| 3 | RLS Enforcement in AI | 2 MD | Každý AI dotaz scoped na `org_id` uživatele |
| 4 | AI Audit Logging | 2 MD | Logování každého promptu a odpovědi do MS-AUDIT |

**TODO:**

- [ ] MCP protocol verze a klientské SDK?
- [ ] Jaké MCP tools definovat? (query_tables, search_documents, summarize_report, ...)

---

### FS15 – Schema Mapping Registry

#### Epic: Template & Schema Registry (MS-TMPL)

**Unit ID:** MS-DATA | **Effort:** 30 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Mapping Rule Engine | 8 MD | Pravidla: `IF column contains "Cena/Cost/Náklady" → map to amount_czk` |
| 2 | Mapping Template CRUD | 5 MD | API pro vytvoření, editaci, verzování mapovacích šablon |
| 3 | Auto-suggestion (Learning) | 7 MD | Systém navrhuje mapování na základě historie (fuzzy match na column names) |
| 4 | N8N Integration Point | 3 MD | `POST /map/apply` voláno z N8N PŘED zápisem do Sink |
| 5 | Excel-to-Form Mapping | 4 MD | `POST /map/excel-to-form` – mapování Excel sloupců na pole formuláře (FS19 prepared) |
| 6 | FE: Mapping Editor | 3 MD | UI pro definici a testování mapovacích pravidel |

**TODO:**

- [ ] Jaké normalizační pravidla jsou potřeba? (měna, jednotky, datumy, naming)
- [ ] Learning: jaký algoritmus? (TF-IDF, embeddings similarity, rule-based?)
- [ ] Priorita pravidel: explicit rule > learned suggestion > user confirmation?

---

## Phase 3b – Reporting Lifecycle

**Cíl:** Stavový automat pro OPEX reporty, submission workflow, deadline management.

**Scope:** FS17, FS20

**Effort:** ~75 MD

---

### FS17 – Report Lifecycle

#### Epic: Report Lifecycle Service (MS-LIFECYCLE)

**Unit ID:** MS-REPORTING | **Effort:** 25 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Report Entity & State Machine | 6 MD | `DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED → DRAFT` |
| 2 | State Transition API | 4 MD | `POST /reports/{id}/submit`, `/approve`, `/reject` s validací oprávnění |
| 3 | Rejection with Comment | 3 MD | Povinný komentář při zamítnutí, viditelný Editorovi |
| 4 | Submission Checklist | 4 MD | Validace kompletnosti před odesláním (povinná pole, nahrané listy, validační pravidla) |
| 5 | Bulk Actions | 3 MD | HoldingAdmin: schválení/zamítnutí více reportů najednou |
| 6 | Data Lock after Approval | 3 MD | Schválená data read-only, úprava = nový DRAFT (nová verze, FS14) |
| 7 | Dapr Event Publishing | 2 MD | Event `report.status_changed` → PubSub pro N8N a MS-NOTIF |

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

**TODO:**

- [ ] Report types: jaké typy reportů existují? (OPEX, CAPEX, Revenue, Custom?)
- [ ] Submission checklist: jaká konkrétní validační pravidla?
- [ ] Má rejection automaticky resetovat checklist?
- [ ] Workflow customizace: různé report_type = různý N8N workflow?

---

##### N8N Lifecycle Workflows (MS-N8N rozšíření) – 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Submission Workflow | 5 MD | Event `SUBMITTED` → validace dat → notifikace HoldingAdmin |
| 2 | Approval Workflow | 4 MD | Event `APPROVED` → zahrnutí do centrálního reportingu → notifikace Editor |
| 3 | Rejection Workflow | 3 MD | Event `REJECTED` → notifikace Editor s komentářem |
| 4 | Deadline Reminder Workflow | 3 MD | Cron: X dní před deadline → notifikace všem s DRAFT |

---

##### FE Lifecycle UI (MS-FE rozšíření) – 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Report Status Dashboard | 5 MD | Matice `[Společnost × Perioda]` se stavem každého reportu (barvy) |
| 2 | Submission Flow UI | 5 MD | Checklist → odeslání → potvrzení, stepper komponenta |
| 3 | Review Flow UI | 4 MD | HoldingAdmin: detail reportu → approve / reject s komentářem |
| 4 | Status Timeline | 3 MD | Historie stavových přechodů per report (timeline komponenta) |
| 5 | Bulk Action UI | 3 MD | Checkbox select + hromadné schválení/zamítnutí |

**TODO:**

- [ ] Wireframe pro Report Status Dashboard (matice společnosti × perioda)?
- [ ] Vizuální design submission flow – stepper nebo wizard?

---

### FS20 – Reporting Period & Deadline Management

#### Epic: Reporting Period Manager (MS-PERIOD)

**Unit ID:** MS-REPORTING | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Period CRUD | 3 MD | Vytvoření periody: `{ name, type, start_date, submission_deadline, review_deadline }` |
| 2 | Period State Machine | 3 MD | `OPEN → COLLECTING → REVIEWING → CLOSED` |
| 3 | Automatic Form Closure | 3 MD | Cron: po submission_deadline → formuláře CLOSED, opozdilé = override |
| 4 | Deadline Notifications | 3 MD | 7/3/1 den před deadline → notifikace (event pro MS-NOTIF) |
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

**TODO:**

- [ ] Může mít organizace více aktivních period současně?
- [ ] Automatické uzavření: hard close (nelze vůbec) nebo soft close (lze s override)?
- [ ] Eskalace: jen notifikace nebo i automatická akce (email vedení)?

---

## Phase 3c – Form Builder

**Cíl:** Centrální sběr OPEX dat přes formuláře – nahrazení Excel šablon posílaných emailem.

**Scope:** FS19

**Effort:** ~81 MD

---

### FS19 – Dynamic Form Builder

#### Epic: Form Builder Service (MS-FORM)

**Unit ID:** MS-REPORTING | **Effort:** 40 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Form Definition Schema | 6 MD | JSON schema pro definici formuláře: sekce, pole, typy, validace |
| 2 | Form CRUD API | 5 MD | Vytvoření, editace, publikace, uzavření formuláře |
| 3 | Form Versioning | 4 MD | Změna publikovaného formuláře → nová verze, historická data zachována |
| 4 | Field Types & Validation | 6 MD | text, number, percentage, date, dropdown, table, file_attachment + validační pravidla |
| 5 | Form Assignment | 3 MD | Přiřazení formuláře k `period_id`, `report_type` a konkrétním organizacím |
| 6 | Form Data Collection API | 6 MD | `POST /forms/{id}/responses` – uložení vyplněných dat, auto-save |
| 7 | Field-level Comments | 3 MD | Komentáře k jednotlivým hodnotám (`"Toto číslo zahrnuje jednorázový odpis"`) |
| 8 | Submission Integration | 3 MD | Po odeslání → report entity (MS-LIFECYCLE) přechází do `SUBMITTED` |
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

**TODO:**

- [ ] Form definition JSON schema: jaký formát? (JSON Schema, custom, nebo hotová knihovna jako react-jsonschema-form?)
- [ ] Auto-save interval: 30 s nebo konfigurovatelný?
- [ ] Má být `table` field type plnohodnotný spreadsheet, nebo jednoduchá tabulka s pevnými sloupci?
- [ ] File attachment: max počet a velikost příloh per formulář?

---

#### Epic: Excel Export/Import (MS-FORM rozšíření)

**Unit ID:** MS-REPORTING | **Effort:** 8 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Excel Template Export | 3 MD | `GET /forms/{id}/export/excel-template` → strukturovaný Excel s metadata listem |
| 2 | Excel Template Import | 3 MD | `POST /forms/{id}/import/excel` → párování dle `__form_meta`, validace verze |
| 3 | Arbitrary Excel Import | 2 MD | Upload vlastního Excelu + mapování sloupců na pole formuláře (MS-TMPL) |

---

#### Epic: FE Form UI (MS-FE rozšíření)

**Unit ID:** MS-FE | **Effort:** 25 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Form Builder UI (Admin) | 8 MD | Drag & drop editor pro tvorbu formulářů, náhled |
| 2 | Form Filling UI (Editor) | 8 MD | Dynamické renderování formuláře z JSON schema, auto-save, validace |
| 3 | Submission Checklist UI | 3 MD | Vizuální checklist před odesláním |
| 4 | Excel Import Review UI | 3 MD | Po importu: zobrazení dat v UI k vizuální kontrole |
| 5 | Field Comments UI | 3 MD | Inline komentáře u každého pole |

**TODO:**

- [ ] Form builder: vlastní implementace nebo knihovna (FormIO, SurveyJS)?
- [ ] Drag & drop knihovna: dnd-kit nebo react-beautiful-dnd?
- [ ] Wireframe pro form filling UI?

---

## Phase 4a – Enterprise Features

**Cíl:** Compliance, versioning, notifications, audit – production readiness.

**Scope:** FS11, FS13, FS14, FS16

**Effort:** ~71 MD

---

### FS13 – Notification Center

#### Epic: Notification Service (MS-NOTIF)

**Unit ID:** MS-CORE | **Effort:** 15 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | In-app Notifications (WebSocket/SSE) | 5 MD | Push notifikace do FE při events (processing done, report approved, ...) |
| 2 | Email Notifications | 4 MD | SendGrid/SMTP integrace pro kritické events (deadline, rejection) |
| 3 | Notification Preferences | 3 MD | Opt-in/opt-out per event type per uživatel |
| 4 | FE: Notification Bell | 3 MD | Bell icon v top baru, dropdown se seznamem notifikací, read/unread |

**TODO:**

- [ ] Email provider: SendGrid, Azure Communication Services, nebo SMTP server?
- [ ] Email šablony: kdo dodá HTML šablony?
- [ ] Notification types: kompletní seznam eventů k notifikaci?

---

### FS14 – Data Versioning & Diff

#### Epic: Versioning Service (MS-VER)

**Unit ID:** MS-CORE | **Effort:** 16 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Version Chain API | 5 MD | Vytvoření nové verze dat (v1 → v2), originál zachován |
| 2 | Diff Engine | 6 MD | Porovnání dvou verzí: přidané/odebrané/změněné řádky, delta hodnoty |
| 3 | FE: Diff Viewer | 5 MD | Side-by-side zobrazení verzí s highlighting změn |

**TODO:**

- [ ] Granularita verzování: per tabulka, per slide, nebo per soubor?
- [ ] Diff zobrazení: side-by-side nebo inline (git-style)?

---

### FS16 – Audit & Compliance

#### Epic: Audit Service (MS-AUDIT)

**Unit ID:** MS-CORE | **Effort:** 25 MD

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

#### Epic: Dashboard Builder (MS-DASH rozšíření)

**Unit ID:** MS-DATA | **Effort:** 15 MD

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

#### Epic: PPTX Template Manager (MS-TMPL-PPTX)

**Unit ID:** MS-REPORTING | **Effort:** 20 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Template Upload & Parsing | 5 MD | Upload PPTX šablony, extrakce `{{placeholder}}` tagů |
| 2 | Template Versioning | 3 MD | v1, v2 šablon, přiřazení k period_id / report_type |
| 3 | Placeholder Mapping UI | 6 MD | UI: `{{it_costs}}` → pole `amount_czk` z formuláře / sloupec z Excelu |
| 4 | Template Preview | 3 MD | Náhled šablony s ukázkovými hodnotami |
| 5 | Template Assignment | 3 MD | Přiřazení šablony ke konkrétním periodám a report types |

**TODO:**

- [ ] Placeholder syntax: `{{variable}}` pro text, `{{TABLE:name}}` pro tabulky, `{{CHART:metric}}` pro grafy – je to dostačující?
- [ ] Podporované typy grafů: bar, line, pie – další?
- [ ] Kdo definuje mapování placeholder → datový zdroj? HoldingAdmin?

---

#### Epic: PPTX Generator (MS-GEN-PPTX)

**Unit ID:** MS-AI | **Effort:** 32 MD

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | Template Rendering Engine | 10 MD | python-pptx: nahrazení textových placeholderů, vyplnění tabulek |
| 2 | Chart Generation | 8 MD | matplotlib/plotly: generování grafů a vložení do PPTX |
| 3 | Missing Data Handling | 3 MD | Chybějící data → `DATA MISSING` vizuální upozornění (červený rámeček) |
| 4 | Batch Generation | 5 MD | Generování PPTX pro všechny schválené reporty v periodě |
| 5 | N8N Generation Workflow | 3 MD | Workflow: event `APPROVED` → generate PPTX → notify |
| 6 | FE: Generator UI | 3 MD | Trigger generování, stav, download výsledného PPTX |

**TODO:**

- [ ] Grafové styly: kdo definuje barevné schéma a styling grafů?
- [ ] Batch generation: paralelně nebo sekvenčně?
- [ ] Výsledný PPTX: veřejný download link nebo autentizovaný?

---

## Phase 5 – DevOps & Onboarding

**Scope:** FS99

| # | Story | Effort | Popis |
|---|---|---|---|
| 1 | OpenTelemetry E2E Tracing | 5 MD | Trace přes FE → GW → N8N → ATM → Sink |
| 2 | Prometheus Metrics | 5 MD | Metriky: chybovost, latence, N8N fronta, DB pool |
| 3 | Grafana Dashboards | 5 MD | Operační dashboardy pro monitoring |
| 4 | Centralized Logging (Loki) | 5 MD | Structured JSON logy ze všech služeb |
| 5 | CI/CD Pipeline | 8 MD | Lint → Test → Build → Docker → Push to Registry |
| 6 | Tilt/Skaffold Local Dev | 5 MD | `tilt up` spustí kompletní topologii |
| 7 | Onboarding Documentation | 3 MD | Runbook pro onboarding první holdingové společnosti |
| 8 | Performance Testing | 5 MD | Load testy: upload, parsing, dashboard rendering |

**TODO:**

- [ ] CI/CD: GitHub Actions nebo Azure DevOps?
- [ ] Container registry: ACR (Azure Container Registry) nebo GitHub Container Registry?
- [ ] Monitoring alerting: PagerDuty, Opsgenie, nebo email?
- [ ] Onboarding: jaký je proces pro přidání nového holdingu? (tenant provisioning)

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

**TODO:**

- [ ] CompanyAdmin: je to nová role vedle Admin/Editor/Viewer, nebo sub-role?
- [ ] Release flow: push (CompanyAdmin posílá) nebo pull (HoldingAdmin si stahuje)?
- [ ] Advanced Period Comparison (FS22): detailní specifikace po zkušenostech z provozu

---

## Souhrn – Celkový přehled Phases

| Phase | Effort (MD) | AI (MD) | Savings | Key Milestones |
|---|---|---|---|---|
| **P1** MVP Core | 193 | 80 | 113 | Upload + PPTX parsing + viewer |
| **P2** Extended Parsing | 86 | 43 | 43 | All formats + dashboards |
| **P3a** Intelligence | 90 | 52 | 38 | Admin + AI + Schema Mapping |
| **P3b** Lifecycle | 75 | 40 | 35 | Report state machine + periods |
| **P3c** Form Builder | 81 | 41 | 40 | Forms + Excel import |
| **P4a** Enterprise | 71 | 34 | 37 | Notifications + audit + versioning |
| **P4b** PPTX Gen | 75 | 35 | 40 | Template-based report generation |
| **P5** DevOps | 41 | 20 | 21 | Production monitoring + CI/CD |
| **P6** Local + Analytics | 63+ | TBD | TBD | Subsidiary internal use |
| **TOTAL** | **~775** | **~345** | **~430** | |

---

## TODO Summary – Rozhodnutí vyžadující vstup

### Infrastruktura & DevOps
- [ ] Azure Entra ID: Tenant ID, App Registration Client ID, Security Groups → role mapping
- [ ] Azure KeyVault URL a seznam secrets
- [ ] Blob Storage: Azure Blob, S3, nebo MinIO (lokální dev)?
- [ ] SSL strategie (produkce): Azure Front Door, Let's Encrypt, vlastní?
- [ ] CI/CD: GitHub Actions nebo Azure DevOps?
- [ ] Container registry: ACR nebo GHCR?
- [ ] Lokální dev: Docker Compose, Tilt, nebo Skaffold?
- [ ] PostgreSQL: sdílená instance (různá schémata per service) nebo oddělené instance?

### Business pravidla
- [ ] RBAC permission matice: kompletní seznam akcí per role
- [ ] Organizační hierarchie: kolik úrovní? (holding → dceřiná → divize?)
- [ ] Max file size pro upload (20/50/100 MB?)
- [ ] Report types: jaké typy reportů existují?
- [ ] Submission checklist: konkrétní validační pravidla
- [ ] Notification events: kompletní seznam eventů k notifikaci
- [ ] Token quota per organizace: výchozí hodnota?

### UX & Design
- [ ] Wireframy / mockupy pro klíčové obrazovky
- [ ] Barevné schéma a branding
- [ ] UI component library: Shadcn/UI + Radix?
- [ ] Form builder: vlastní implementace nebo knihovna (FormIO, SurveyJS)?
- [ ] Chart library: Recharts, Nivo, ECharts?
- [ ] Dark mode: v P1 nebo později?

### AI & Data
- [ ] LLM provider: Azure OpenAI, OpenAI API, open-source?
- [ ] Embedding model a dimenze (1536 vs 768)
- [ ] OCR jazyky (čeština + angličtina + další?)
- [ ] Schema Mapping learning algoritmus

### Retention & Security
- [ ] Originální soubory (s makry): trvale nebo s expirací?
- [ ] Artifact retention: 24h PNG, jiné typy?
- [ ] ClamAV: ICAP nebo clamd socket?
- [ ] Virus DB update strategie
