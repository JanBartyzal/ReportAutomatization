# Mermaid Diagramy – Stav systému po každé fázi
**Verze:** 1.0  
**Navazuje na:** Implementation Plan v2.1  
**Datum:** Únor 2026

> Každý diagram zobrazuje **kumulativní stav systému** po dokončení dané fáze – tedy co je živé a funkční, ne jen co bylo přidáno v dané fázi. Nové komponenty jsou vizuálně odlišeny.

---

## Phase 1 – MVP Core (M1–2)
*Upload PPTX → parsování → uložení → základní viewer*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway\n(Traefik)"]
        AUTH["engine-core:auth\nAuth Service\n(Entra ID + RBAC)"]
    end

    subgraph "Ingestion"
        ING["engine-ingestor\nFile Ingestor\n(streaming upload)"]
        SCAN["engine-ingestor:scanner\nSecurity Scanner\n(ClamAV)"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator\n(basic pipeline)"]
    end

    subgraph "Processing"
        PPTX["processor-atomizers:pptx\nPPTX Atomizer\n(text + tables + images)"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl\nTable API"]
        SINKLOG["engine-data:sink-log\nLog API"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL")]
        BLOB[("Blob Storage")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(upload + viewer)"]
    end

    subgraph "External"
        AAD["Azure Entra ID"]
        KV["Azure KeyVault"]
    end

    FE -->|HTTPS| GW
    GW --> AUTH
    GW --> ING
    AUTH --> AAD
    AUTH --> KV
    ING --> SCAN
    ING --> BLOB
    ING -->|webhook| N8N
    N8N --> PPTX
    PPTX --> BLOB
    N8N --> SINKTBL
    N8N --> SINKLOG
    SINKTBL --> PG
    SINKLOG --> PG

    style GW fill:#1F3864,color:#fff
    style AUTH fill:#1F3864,color:#fff
    style ING fill:#1F3864,color:#fff
    style SCAN fill:#1F3864,color:#fff
    style N8N fill:#1F3864,color:#fff
    style PPTX fill:#1F3864,color:#fff
    style SINKTBL fill:#1F3864,color:#fff
    style SINKLOG fill:#1F3864,color:#fff
    style FE fill:#1F3864,color:#fff
```

**Po Phase 1 je živé:** 9 services · PPTX upload & extrakce · základní viewer · auth

---

## Phase 2 – Extended Parsing (M3–4)
*Plná podpora formátů + BI dashboardy · nové komponenty zvýrazněny*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway"]
        AUTH["engine-core:auth\nAuth Service"]
    end

    subgraph "Ingestion"
        ING["engine-ingestor\nFile Ingestor"]
        SCAN["engine-ingestor:scanner\nSecurity Scanner"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator"]
    end

    subgraph "Processing"
        PPTX["processor-atomizers:pptx\nPPTX Atomizer"]
        XLS["processor-atomizers:xls\nExcel Atomizer ★"]
        PDF["processor-atomizers:pdf\nPDF/OCR Atomizer ★"]
        CSV["processor-atomizers:csv\nCSV Atomizer ★"]
        CLN["processor-atomizers:cleanup\nCleanup Worker ★"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl\nTable API"]
        SINKLOG["engine-data:sink-log\nLog API"]
    end

    subgraph "Read Layer"
        QRY["engine-data:query\nQuery API ★"]
        DASH["engine-data:dashboard\nDashboard Aggregation ★"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL\n+ RLS ★")]
        BLOB[("Blob Storage")]
        REDIS[("Redis Cache ★")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(upload + viewer\n+ dashboards ★)"]
    end

    FE --> GW
    GW --> AUTH
    GW --> ING
    GW --> QRY
    ING --> SCAN
    ING --> BLOB
    ING --> N8N
    N8N --> PPTX
    N8N --> XLS
    N8N --> PDF
    N8N --> CSV
    PPTX --> BLOB
    XLS --> BLOB
    PDF --> BLOB
    CLN --> BLOB
    N8N --> SINKTBL
    N8N --> SINKLOG
    SINKTBL --> PG
    SINKLOG --> PG
    QRY --> PG
    QRY --> REDIS
    DASH --> PG

    style XLS fill:#2E75B6,color:#fff
    style PDF fill:#2E75B6,color:#fff
    style CSV fill:#2E75B6,color:#fff
    style CLN fill:#2E75B6,color:#fff
    style QRY fill:#2E75B6,color:#fff
    style DASH fill:#2E75B6,color:#fff
```

**Po Phase 2 je živé:** 15 services · Excel/PDF/CSV parsování · RLS · BI dashboardy · Redis cache

---

## Phase 3 – Intelligence & Admin (M5–6)
*AI integrace + holdingová hierarchie + Schema Mapping*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway"]
        AUTH["engine-core:auth\nAuth Service"]
    end

    subgraph "Ingestion"
        ING["engine-ingestor\nFile Ingestor"]
        SCAN["engine-ingestor:scanner\nSecurity Scanner"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator"]
    end

    subgraph "Processing"
        PPTX["processor-atomizers:pptx"]
        XLS["processor-atomizers:xls"]
        PDF["processor-atomizers:pdf"]
        CSV["processor-atomizers:csv"]
        CLN["processor-atomizers:cleanup"]
        AI["processor-atomizers:ai\nAI Gateway ★\n(LiteLLM)"]
        MCP["processor-generators:mcp\nMCP Server ★\n(AI Agents + OBO)"]
    end

    subgraph "Schema & Admin"
        TMPL["engine-data:template\nSchema Mapping Registry ★\n(column normalization + learning)"]
        ADMIN["engine-core:admin\nAdmin Backend ★\n(roles, holding hierarchy, API keys)"]
        BATCH["engine-core:batch\nBatch & Org Service ★\n(org metadata + RLS)"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl\nTable API"]
        SINKLOG["engine-data:sink-log\nLog API"]
        SINKDOC["engine-data:sink-doc\nDocument API ★\n(+ pgVector embeddings)"]
    end

    subgraph "Read Layer"
        QRY["engine-data:query\nQuery API"]
        DASH["engine-data:dashboard\nDashboard Aggregation"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL + RLS")]
        BLOB[("Blob Storage")]
        REDIS[("Redis")]
        VEC[("pgVector ★")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(+ Admin UI ★\n+ AI Query ★)"]
    end

    FE --> GW
    GW --> AUTH
    GW --> ING
    GW --> QRY
    GW --> ADMIN
    GW --> MCP
    ING --> SCAN
    ING --> BLOB
    ING --> N8N
    N8N --> PPTX
    N8N --> XLS
    N8N --> PDF
    N8N --> CSV
    N8N --> AI
    N8N --> TMPL
    N8N --> SINKTBL
    N8N --> SINKDOC
    N8N --> SINKLOG
    TMPL --> PG
    ADMIN --> PG
    BATCH --> PG
    SINKDOC --> VEC
    AI --> SINKDOC
    MCP --> QRY
    MCP --> AUTH
    QRY --> PG
    QRY --> REDIS
    DASH --> PG

    style AI fill:#2E75B6,color:#fff
    style MCP fill:#2E75B6,color:#fff
    style TMPL fill:#2E75B6,color:#fff
    style ADMIN fill:#2E75B6,color:#fff
    style BATCH fill:#2E75B6,color:#fff
    style SINKDOC fill:#2E75B6,color:#fff
    style VEC fill:#2E75B6,color:#fff
```

**Po Phase 3 je živé:** 22 services · AI sémantická analýza · MCP agent · Schema Mapping · holdingová hierarchie

---

## Phase 3b – Reporting Lifecycle (M6–7)
*Stavový automat reportu + periody + deadliny*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway"]
        AUTH["engine-core:auth\nAuth Service"]
    end

    subgraph "Reporting Lifecycle ★"
        LIFE["engine-reporting:lifecycle\nReport Lifecycle ★\n(Draft→Submitted\n→Approved/Rejected)"]
        PERIOD["engine-reporting:period\nReporting Period Mgr ★\n(deadlines + escalation\n+ completion tracking)"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator\n(+ lifecycle workflows ★)"]
    end

    subgraph "Processing"
        PPTX["processor-atomizers:pptx"]
        XLS["processor-atomizers:xls"]
        PDF["processor-atomizers:pdf"]
        CSV["processor-atomizers:csv"]
        AI["processor-atomizers:ai\nAI Gateway"]
    end

    subgraph "Schema & Admin"
        TMPL["engine-data:template\nSchema Mapping"]
        ADMIN["engine-core:admin\nAdmin Backend\n(+ Reviewer role ★)"]
        BATCH["engine-core:batch\nBatch & Org"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl"]
        SINKLOG["engine-data:sink-log"]
        SINKDOC["engine-data:sink-doc"]
    end

    subgraph "Read Layer"
        QRY["engine-data:query\nQuery API"]
        DASH["engine-data:dashboard\nDashboard\n(+ period matrix ★)"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL\n(+ reports table ★\n+ periods table ★)")]
        BLOB[("Blob Storage")]
        REDIS[("Redis")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(+ lifecycle UI ★\n+ period dashboard ★\n+ submission flow ★)"]
    end

    subgraph "External"
        PUBSUB["Dapr PubSub\nreport.status_changed ★"]
    end

    FE --> GW
    GW --> AUTH
    GW --> LIFE
    GW --> PERIOD
    LIFE --> PG
    LIFE --> PUBSUB
    PERIOD --> PG
    PERIOD --> PUBSUB
    PUBSUB --> N8N
    N8N --> PPTX
    N8N --> XLS
    N8N --> AI
    N8N --> TMPL
    N8N --> SINKTBL
    N8N --> SINKLOG
    QRY --> PG
    QRY --> REDIS
    DASH --> PG

    style LIFE fill:#2E75B6,color:#fff
    style PERIOD fill:#2E75B6,color:#fff
    style PUBSUB fill:#2E75B6,color:#fff
```

**Po Phase 3b je živé:** 24 services · stavový automat reportů · periody s deadliny · matice stavu Společnost × Perioda

---

## Phase 3c – Form Builder (M7–8)
*Centrální sběr dat přes formuláře + Excel export/import*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway"]
        AUTH["engine-core:auth\nAuth Service"]
    end

    subgraph "Data Collection ★"
        FORM["engine-reporting:form\nForm Builder ★\n(drag & drop editor\nvalidation + auto-save\nversioning + PUBLISHED/CLOSED)"]
        FORMXLS["engine-reporting:form\nExcel Export/Import ★\n(template export\n+ re-import with form_version_id)"]
    end

    subgraph "Reporting Lifecycle"
        LIFE["engine-reporting:lifecycle\nReport Lifecycle\n(Draft→Submitted\n→Approved/Rejected)"]
        PERIOD["engine-reporting:period\nReporting Period Mgr"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator"]
    end

    subgraph "Schema & Admin"
        TMPL["engine-data:template\nSchema Mapping\n(+ excel-to-form mapping ★)"]
        ADMIN["engine-core:admin\nAdmin Backend\n(+ form management ★)"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl\nTable API\n(+ form_responses ★)"]
        SINKLOG["engine-data:sink-log"]
        SINKDOC["engine-data:sink-doc"]
    end

    subgraph "Read Layer"
        QRY["engine-data:query\nQuery API\n(source_type: FORM/FILE ★)"]
        DASH["engine-data:dashboard\nDashboard"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL\n(+ form_definitions ★\n+ form_responses ★\n+ form_versions ★)")]
        BLOB[("Blob Storage\n(+ Excel attachments ★)")]
        REDIS[("Redis")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(+ Form Builder editor ★\n+ form fill UI ★\n+ Excel export/import UI ★\n+ field-level comments ★)"]
    end

    FE --> GW
    GW --> AUTH
    GW --> FORM
    GW --> LIFE
    FORM --> PG
    FORM --> LIFE
    FORMXLS --> BLOB
    FORMXLS --> TMPL
    FORMXLS --> PG
    SINKTBL --> PG
    QRY --> PG
    QRY --> REDIS
    DASH --> PG

    style FORM fill:#2E75B6,color:#fff
    style FORMXLS fill:#2E75B6,color:#fff
```

**Po Phase 3c je živé:** 25 services · Form Builder · Excel export/import šablony · form_responses v DB · Schema Mapping pro import

---

## Phase 4 – Enterprise Features (M8–9)
*Audit · verzování · notifikace · full-text search*

```mermaid
graph TB
    subgraph "Edge"
        GW["router\nAPI Gateway"]
        AUTH["engine-core:auth\nAuth Service"]
    end

    subgraph "Enterprise Layer ★"
        NOTIF["engine-reporting:notification\nNotification Center ★\n(WebSocket/SSE\n+ SendGrid e-mail\n+ lifecycle triggers)"]
        VER["engine-core:versioning\nVersioning Service ★\n(v1→v2 + diff tool\n+ lock on APPROVED)"]
        AUDIT["engine-core:audit\nAudit & Compliance ★\n(immutable logs\n+ AI audit\n+ export)"]
        SRCH["engine-data:search\nSearch Service ★\n(FTS + vector search)"]
    end

    subgraph "Reporting Lifecycle"
        LIFE["engine-reporting:lifecycle\nReport Lifecycle"]
        PERIOD["engine-reporting:period\nReporting Period Mgr"]
        FORM["engine-reporting:form\nForm Builder"]
    end

    subgraph "Orchestration"
        N8N["engine-orchestrator\nN8N Orchestrator\n(+ notif triggers ★)"]
    end

    subgraph "Processing"
        PPTX["processor-atomizers:pptx"]
        XLS["processor-atomizers:xls"]
        AI["processor-atomizers:ai"]
        MCP["processor-generators:mcp\nMCP Server"]
    end

    subgraph "Schema & Admin"
        TMPL["engine-data:template\nSchema Mapping"]
        ADMIN["engine-core:admin\nAdmin Backend"]
        BATCH["engine-core:batch\nBatch & Org"]
    end

    subgraph "Persistence"
        SINKTBL["engine-data:sink-tbl"]
        SINKLOG["engine-data:sink-log"]
        SINKDOC["engine-data:sink-doc"]
    end

    subgraph "Read Layer"
        QRY["engine-data:query\nQuery API"]
        DASH["engine-data:dashboard\nDashboard"]
    end

    subgraph "Data Stores"
        PG[("PostgreSQL\n(+ audit_log ★\n+ versions ★)")]
        BLOB[("Blob Storage")]
        REDIS[("Redis")]
        VEC[("pgVector")]
        ES[("ElasticSearch ★")]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(+ diff viewer ★\n+ audit trail UI ★\n+ search ★)"]
    end

    FE --> GW
    GW --> AUTH
    GW --> QRY
    GW --> SRCH
    N8N --> NOTIF
    LIFE --> NOTIF
    PERIOD --> NOTIF
    AUDIT --> PG
    VER --> PG
    SRCH --> ES
    SRCH --> VEC
    NOTIF --> REDIS
    QRY --> PG
    QRY --> REDIS
    DASH --> PG

    style NOTIF fill:#2E75B6,color:#fff
    style VER fill:#2E75B6,color:#fff
    style AUDIT fill:#2E75B6,color:#fff
    style SRCH fill:#2E75B6,color:#fff
    style ES fill:#2E75B6,color:#fff
```

**Po Phase 4 je živé:** 29 services · immutable audit log · versioning + diff · e-mail notifikace pro lifecycle events · full-text + vector search

---

## Phase 4b – PPTX Report Generation (M9–10)
*Uzavření cyklu: schválená data → standardizovaný PPTX report*

```mermaid
graph LR
    subgraph "Report Generation ★"
        TMPLPPTX["engine-reporting:pptx-template\nPPTX Template Manager ★\n(upload šablony\nplaceholder extrakce\nplaceholder → field mapping)"]
        GEN["processor-generators:pptx\nPPTX Generator ★\n(python-pptx + matplotlib\nasync + batch generování\nDATA MISSING fallback)"]
    end

    subgraph "Trigger Flow"
        LIFE["engine-reporting:lifecycle\nReport Lifecycle\n(APPROVED event)"]
        PUBSUB["Dapr PubSub\nreport.status_changed"]
        N8N["engine-orchestrator\nN8N Orchestrator\n(+ generation workflow ★)"]
    end

    subgraph "Data Sources"
        PG[("PostgreSQL\n(form_responses\n+ parsed tables)")]
        BLOB[("Blob Storage\n(+ generated PPTX ★)")]
    end

    subgraph "Notification"
        NOTIF["engine-reporting:notification\nNotification Center\n(PPTX ready alert ★)"]
    end

    subgraph "Frontend"
        FE["frontend\nReact SPA\n(+ Template Manager UI ★\n+ placeholder mapping UI ★\n+ download & batch generate ★)"]
    end

    LIFE -->|APPROVED| PUBSUB
    PUBSUB --> N8N
    N8N --> GEN
    GEN --> PG
    GEN --> BLOB
    GEN --> NOTIF
    N8N --> TMPLPPTX
    TMPLPPTX --> PG
    FE --> TMPLPPTX
    FE --> GEN

    style TMPLPPTX fill:#2E75B6,color:#fff
    style GEN fill:#2E75B6,color:#fff
    style PUBSUB fill:#2E75B6,color:#fff
```

**Po Phase 4b je živé:** 31 services · kompletní OPEX lifecycle uzavřen · automatické generování PPTX po schválení dat

---

## Phase 5 – DevOps Maturity + First Holding Onboarding (M10–11)
*Produkční infrastruktura + observability + první zákazník live*

```mermaid
graph TB
    subgraph "Internet"
        USER["Uživatel / Browser"]
        CDN["CDN / Static Assets"]
    end

    subgraph "Azure Edge"
        WAF["Azure Front Door\n+ WAF ★\n(DDoS, bot protection)"]
    end

    subgraph "Azure Container Apps Environment"
        subgraph "Namespace: edge"
            GW["router\nTraefik"]
            AUTH["engine-core:auth"]
        end

        subgraph "Namespace: ingestion"
            ING["engine-ingestor"]
            SCAN["engine-ingestor:scanner"]
        end

        subgraph "Namespace: lifecycle"
            LIFE["engine-reporting:lifecycle"]
            PERIOD["engine-reporting:period"]
            FORM["engine-reporting:form"]
        end

        subgraph "Namespace: orchestration"
            N8N["engine-orchestrator"]
        end

        subgraph "Namespace: atomizers"
            PPTX["processor-atomizers:pptx"]
            XLS["processor-atomizers:xls"]
            PDF["processor-atomizers:pdf"]
            AI["processor-atomizers:ai"]
            GEN["processor-generators:pptx"]
        end

        subgraph "Namespace: sinks"
            SINKTBL["engine-data:sink-tbl"]
            SINKDOC["engine-data:sink-doc"]
            SINKLOG["engine-data:sink-log"]
        end

        subgraph "Namespace: read"
            QRY["engine-data:query"]
            DASH["engine-data:dashboard"]
            SRCH["engine-data:search"]
        end

        subgraph "Namespace: support"
            ADMIN["engine-core:admin"]
            NOTIF["engine-reporting:notification"]
            TMPL["engine-data:template"]
            AUDIT["engine-core:audit"]
            VER["engine-core:versioning"]
            MCP["processor-generators:mcp"]
            BATCH["engine-core:batch"]
            TMPLPPTX["engine-reporting:pptx-template"]
        end

        subgraph "Namespace: observability ★"
            OTEL["OpenTelemetry\nCollector ★"]
            PROM["Prometheus ★"]
            GRAF["Grafana ★"]
            LOKI["Loki ★"]
        end
    end

    subgraph "Azure Data Layer"
        PG[("PostgreSQL 16\n+ RLS + pgVector")]
        REDIS[("Redis Cache")]
        BLOB[("Blob Storage")]
        ES[("ElasticSearch")]
    end

    subgraph "External Services"
        AAD["Azure Entra ID"]
        KV["Azure KeyVault"]
        SMTP["SendGrid\n(e-mail)"]
        LLM["Azure OpenAI\n/ LiteLLM"]
    end

    USER --> WAF
    CDN --> WAF
    WAF --> GW
    GW --> AUTH
    AUTH --> AAD
    AUTH --> KV
    OTEL -.->|traces| PROM
    OTEL -.->|logs| LOKI
    PROM -.-> GRAF
    LOKI -.-> GRAF
    NOTIF --> SMTP
    AI --> LLM

    style WAF fill:#2E75B6,color:#fff
    style OTEL fill:#2E75B6,color:#fff
    style PROM fill:#2E75B6,color:#fff
    style GRAF fill:#2E75B6,color:#fff
    style LOKI fill:#2E75B6,color:#fff
```

**Po Phase 5 je živé:** 31 services + observability stack · WAF · autoscaling · první holding onboardován

---

## Phase 6 – Local Scope & Advanced Analytics (M12+)
*Platforma jako standalone nástroj pro dceřiné společnosti*

```mermaid
graph TB
    subgraph "Central Scope (Holding)"
        direction TB
        ADMIN_H["engine-core:admin\nHoldingAdmin\n(přehled lokálních\nšablon/formulářů)"]
        PERIOD_C["engine-reporting:period\nCentral Periods\n(+ Advanced Comparison ★)"]
        FORM_C["engine-reporting:form\nCentral Forms\n(scope: CENTRAL)"]
        TMPL_C["engine-reporting:pptx-template\nCentral Templates\n(scope: CENTRAL)"]
    end

    subgraph "Local Scope – Dceřiná společnost ★"
        direction TB
        CADMIN["CompanyAdmin ★\n(nová role)"]
        FORM_L["engine-reporting:form\nLocal Forms ★\n(scope: LOCAL)"]
        TMPL_L["engine-reporting:pptx-template\nLocal Templates ★\n(scope: LOCAL)"]
        LIFE_L["engine-reporting:lifecycle\nLocal Lifecycle ★\n(bez holdingového\napproval)"]
        GEN_L["processor-generators:pptx\nLocal Report ★\n(interní report)"]
    end

    subgraph "Release Flow ★"
        REL["RELEASED data ★\n(CompanyAdmin uvolní\nlokální data pro Holding)"]
    end

    subgraph "Advanced Analytics ★"
        COMP["Advanced Period\nComparison ★\n(multi-org benchmarking\ncost center drill-down)"]
        DASH["engine-data:dashboard\n(extended ★)"]
    end

    subgraph "Data Store"
        PG[("PostgreSQL\n(scope: LOCAL/CENTRAL/\nSHARED_WITHIN_HOLDING ★)")]
    end

    CADMIN --> FORM_L
    CADMIN --> TMPL_L
    FORM_L --> LIFE_L
    LIFE_L --> GEN_L
    FORM_L --> REL
    REL --> ADMIN_H
    ADMIN_H --> PERIOD_C
    PERIOD_C --> COMP
    COMP --> DASH
    FORM_L --> PG
    FORM_C --> PG
    TMPL_L --> PG
    TMPL_C --> PG

    style CADMIN fill:#2E75B6,color:#fff
    style FORM_L fill:#2E75B6,color:#fff
    style TMPL_L fill:#2E75B6,color:#fff
    style LIFE_L fill:#2E75B6,color:#fff
    style GEN_L fill:#2E75B6,color:#fff
    style REL fill:#2E75B6,color:#fff
    style COMP fill:#2E75B6,color:#fff
```

**Po Phase 6 je živé:** 31 services + lokální scope · CompanyAdmin role · local forms & templates · "release" flow · advanced period comparison

---

## Kompletní architektura – Full System (po Phase 6)

```mermaid
graph TB
    subgraph "Internet & Edge"
        USER["Uživatelé\n(HoldingAdmin / Editor\n/ Viewer / CompanyAdmin)"]
        WAF["Azure Front Door + WAF"]
    end

    subgraph "Azure Container Apps"
        subgraph "Edge Layer"
            GW["router\nAPI Gateway"]
            AUTH["engine-core:auth\nAuth Service"]
        end

        subgraph "Ingestion Layer"
            ING["engine-ingestor\nFile Ingestor"]
            SCAN["engine-ingestor:scanner\nSecurity Scanner"]
        end

        subgraph "Lifecycle Layer"
            LIFE["engine-reporting:lifecycle\nReport Lifecycle"]
            PERIOD["engine-reporting:period\nReporting Period"]
            FORM["engine-reporting:form\nForm Builder"]
        end

        subgraph "Orchestration Layer"
            N8N["engine-orchestrator\nN8N Orchestrator"]
        end

        subgraph "Processing Layer"
            PPTX["processor-atomizers:pptx"]
            XLS["processor-atomizers:xls"]
            PDF["processor-atomizers:pdf"]
            CSV["processor-atomizers:csv"]
            CLN["processor-atomizers:cleanup"]
            AI["processor-atomizers:ai\nAI Gateway"]
            GEN["processor-generators:pptx\nPPTX Generator"]
        end

        subgraph "Sink Layer"
            SINKTBL["engine-data:sink-tbl"]
            SINKDOC["engine-data:sink-doc"]
            SINKLOG["engine-data:sink-log"]
        end

        subgraph "Read Layer"
            QRY["engine-data:query\nQuery API"]
            DASH["engine-data:dashboard\nDashboard Agg"]
            SRCH["engine-data:search\nSearch"]
        end

        subgraph "Support Layer"
            ADMIN["engine-core:admin"]
            NOTIF["engine-reporting:notification"]
            TMPL["engine-data:template\nSchema Mapping"]
            AUDIT["engine-core:audit"]
            VER["engine-core:versioning"]
            MCP["processor-generators:mcp"]
            BATCH["engine-core:batch"]
            TMPLPPTX["engine-reporting:pptx-template"]
        end

        subgraph "Observability"
            OTEL["OpenTelemetry"]
            PROM["Prometheus"]
            GRAF["Grafana"]
            LOKI["Loki"]
        end
    end

    subgraph "Data Layer"
        PG[("PostgreSQL 16\n+ RLS + pgVector")]
        REDIS[("Redis")]
        BLOB[("Blob Storage")]
        ES[("ElasticSearch")]
    end

    subgraph "External"
        AAD["Azure Entra ID"]
        KV["Azure KeyVault"]
        SMTP["SendGrid"]
        LLM["Azure OpenAI"]
    end

    USER --> WAF --> GW
    GW --> AUTH --> AAD
    GW --> ING --> SCAN
    GW --> LIFE
    GW --> FORM
    GW --> PERIOD
    GW --> QRY
    GW --> ADMIN
    GW --> MCP
    ING --> BLOB
    ING --> N8N
    LIFE --> N8N
    N8N --> PPTX & XLS & PDF & CSV & AI & GEN
    N8N --> SINKTBL & SINKDOC & SINKLOG
    N8N --> TMPL & NOTIF
    GEN --> BLOB
    SINKTBL & SINKDOC & SINKLOG --> PG
    QRY --> PG & REDIS
    DASH --> PG
    SRCH --> ES & PG
    AUDIT --> PG
    VER --> PG
    NOTIF --> SMTP
    AI --> LLM
    AUTH --> KV
    OTEL -.-> PROM -.-> GRAF
    LOKI -.-> GRAF
```

**Kompletní systém:** 31 microservices · 5 databázových technologií · full OPEX lifecycle · M12+

---

*Mermaid Diagramy v1.0 | PPTX Analyzer & Automation Platform | Únor 2026*
