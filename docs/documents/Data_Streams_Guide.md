# Datove streamy — Architektura a roadmapa

Dokumentace pro architekturu datovych streamu v RA platforme. Popisuje aktualni streamy (OPEX, univerzalni reporting), planovane streamy (Budget Reporting s napojenim na ServiceNow) a principy fyzickeho oddeleni dat v databazi.

---

## Prehled

```
                          RA Platforma
  ┌───────────────────────────────────────────────────────┐
  │                                                       │
  │   ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │
  │   │  OPEX Stream │  │  Universal   │  │   Budget   │ │
  │   │  (aktivni)   │  │  Reporting   │  │  Reporting │ │
  │   │              │  │  (aktivni)   │  │ (planovano)│ │
  │   └──────┬───────┘  └──────┬───────┘  └─────┬──────┘ │
  │          │                 │                 │        │
  │          ▼                 ▼                 ▼        │
  │   ┌────────────────────────────────────────────────┐  │
  │   │              data schema (PostgreSQL)          │  │
  │   │   parsed_tables │ documents │ form_responses   │  │
  │   └────────────────────────────────────────────────┘  │
  │                                                       │
  └───────────────────────────────────────────────────────┘
```

---

## 1. Aktualni DB schemata

Platforma pouziva **jednu PostgreSQL databazi** s **7 schematy**:

| Schema | Sluzba | Ucel |
|--------|--------|------|
| `core` | engine-core | Auth, Admin, Batch, Versioning, Audit |
| `data` | engine-data | Sinks (parsed_tables, documents), Query, Dashboard, Template |
| `reporting` | engine-reporting | Lifecycle, Period, Form, PPTX Template, Notification |
| `ingestor` | engine-ingestor | File ingestion, virus scanning |
| `orchestrator` | engine-orchestrator | Workflow orchestration |
| `integrations` | engine-integrations | ServiceNow integration |
| `rls` | (sdilene) | Row-Level Security funkce |

### 1.1 Row-Level Security (RLS)

Vsechny multi-tenantni tabulky pouzivaji RLS:
- Izolace pres `rls.get_current_org_id()` funkci
- Kontext nastaven aplikacnim kodem: `SET LOCAL app.current_org_id = '<uuid>'`
- Transakcni scope — automaticky reset po commit/rollback

### 1.2 Organizacni hierarchie

```
Holding (parent)
  ├── Company A (child, type=COMPANY)
  ├── Company B (child, type=COMPANY)
  └── Company C (child, type=COMPANY)
```

- `organizations.parent_id` podporuje hierarchii
- Batche a periody se prirazuji na urovni holdingu (`holding_id`)
- Kazda company vidi pouze sva data (RLS)

---

## 2. Stream: OPEX (Operational Expenditure)

**Status:** Aktivni, implementovano

### 2.1 Popis

OPEX stream pokryva sber dat o operacnich nakladech firmy — personalní naklady, IT infrastruktura, kancelarske provozni naklady a cestovni nahrady.

### 2.2 Zdroje dat

| Zdroj | Metoda importu | Cilova tabulka |
|-------|----------------|----------------|
| Excel soubory (.xlsx) | Upload → engine-ingestor → parsed_tables | `data.parsed_tables` |
| PPTX reporty | Upload → SlideMetadata extrakce → parsed_tables | `data.parsed_tables` |
| Formulare | Manualni zadani v UI → form_responses | `reporting.form_responses` |
| Import z Excelu do formulare | Upload s `FORM_IMPORT` → form_responses | `reporting.form_responses` |

### 2.3 Datova struktura (formular)

Vzorovy OPEX formular obsahuje tyto sekce:

| Sekce | Pole | Typ |
|-------|------|-----|
| Personnel Costs | Headcount, Salaries, Benefits | number |
| IT & Infrastructure | Hardware, Software Licenses, Cloud Services | number |
| Office & Operations | Rent, Utilities, Supplies, Insurance | number |
| Travel & Entertainment | Domestic Travel, International Travel, Client Entertainment | number |
| Summary | Total Budget (calculated), Category, Notes | number, dropdown, text |

### 2.4 Tok dat

```
Excel Upload                  Formular
     │                            │
     ▼                            ▼
parsed_tables              form_responses
     │                            │
     └──────────┬─────────────────┘
                │
                ▼
          Dashboard (Custom SQL)
                │
                ▼
     Reports → Generate PPTX
```

### 2.5 Priklad SQL dotazu pro OPEX

```sql
-- OPEX souhrn per organizace z posledniho uploadu
WITH latest AS (
  SELECT pt.headers, pt.rows, pt.org_id
  FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename LIKE 'OPEX%'
  ORDER BY f.created_at DESC LIMIT 1
)
SELECT
  r.value->>1 AS LabelX,                          -- Nazev polozky
  CAST(r.value->>9 AS NUMERIC) AS LabelY           -- Celkove naklady
FROM latest l, jsonb_array_elements(l.rows) AS r(value)
ORDER BY LabelY DESC
```

---

## 3. Stream: Univerzalni Reporting

**Status:** Aktivni, implementovano

### 3.1 Popis

Univerzalni reporting stream pokryva cely zivotni cyklus reportu — od vytvoreni, pres schvalovaci proces, az po generovani PPTX vystupu. Je nezavisly na typu dat.

### 3.2 Komponenty

| Komponenta | Schema | Tabulka | Popis |
|------------|--------|---------|-------|
| Reporty | `reporting` | `reports` | Lifecycle stavy (DRAFT → APPROVED → RELEASED) |
| Periody | `reporting` | `periods` | Reportovaci obdobi (Q1, Q2, Annual) |
| Formulare | `reporting` | `forms`, `form_versions`, `form_fields` | Dynamicke formulare |
| Odpovedi | `reporting` | `form_responses`, `form_field_values` | Data z formularu |
| PPTX sablony | `reporting` | PPTX template management | Sablony pro generovani |
| Batche | `core` | `batches`, `batch_files` | Organizace souboru |

### 3.3 Reporting Lifecycle

```
Periods (Q1 2026, Q2 2026, ...)
  │
  ├── Prirazeni organizaci (period_org_assignments)
  │     └── Company A, Company B, ...
  │
  ├── Prirazeni formularu (form_assignments)
  │     └── OPEX form, Budget form, ...
  │
  └── Reporty (reports)
        └── DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → COMPLETED → RELEASED
```

### 3.4 Feature Flags

System podporuje postupne zavedeni funkci pres feature flags:

| Flag | Popis | Default |
|------|-------|---------|
| `ENABLE_LOCAL_SCOPE` | Dceriny firmy vytvareji vlastni formulare/sablony/reporty | `false` |
| `ENABLE_ADVANCED_COMPARISON` | Multi-period trend analyza | `false` |

---

## 4. Stream: Budget Reporting (planovano)

**Status:** Planovano — priprava infrastruktury

### 4.1 Popis

Budget Reporting stream bude pokryvat rozpoctovy proces — sber budgetovych dat z dcerinych firem, konsolidace na urovni holdingu a porovnani plan vs. skutecnost.

### 4.2 Zdroje dat

| Zdroj | Metoda | Poznamka |
|-------|--------|---------|
| ServiceNow (SNow) | Automaticky sync pres engine-integrations | Primarni zdroj |
| Excel import | Upload pres API | Alternativa/migrace |
| Manualni formulare | UI formular | Doplnkovy zdroj |

### 4.3 ServiceNow integrace

Engine-integrations jiz implementuje zakladni infrastrukturu:

```
ServiceNow Instance
  │
  │ REST API (OAuth2 / Basic Auth)
  ▼
engine-integrations
  │ servicenow_connections   — konfigurace pripojeni
  │ sync_schedules           — CRON planovani synchronizace
  │ sync_job_history         — historie behu
  │
  │ Dapr pub/sub event
  ▼
engine-orchestrator
  │ Workflow: SYNC → PARSE → MAP → STORE
  ▼
data.parsed_tables (nebo budouci budget schema)
```

#### Konfigurace pripojeni k ServiceNow

```bash
# Vytvoreni ServiceNow connection
curl -X POST http://localhost:8106/api/integrations/servicenow/connections \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "name": "Production SNow Instance",
    "instance_url": "https://company.service-now.com",
    "auth_type": "OAUTH2",
    "credentials_ref": "vault://snow-prod-credentials",
    "tables": ["u_budget_items", "u_cost_centers"],
    "enabled": true
  }'
```

#### Nastaveni synchronizacniho planu

```bash
# Nastaveni CRON planu synchronizace
curl -X POST http://localhost:8106/api/integrations/servicenow/schedules \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "connection_id": "<connection-uuid>",
    "cron_expression": "0 0 6 * * ?",
    "enabled": true
  }'
```

| Pole | Popis |
|------|-------|
| `cron_expression` | CRON vyraz (`0 0 6 * * ?` = denne v 6:00) |
| `enabled` | Zapnout/vypnout synchronizaci |

#### Sledovani synchronizace

```bash
# Historie synchronizaci
curl http://localhost:8106/api/integrations/servicenow/schedules/<schedule-id>/history \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
[
  {
    "id": "job-uuid-1",
    "started_at": "2026-04-10T06:00:00Z",
    "completed_at": "2026-04-10T06:02:30Z",
    "records_fetched": 1250,
    "records_stored": 1250,
    "status": "COMPLETED"
  }
]
```

#### Distribuce reportu zpet do ServiceNow

```bash
# Nastaveni distribucniho pravidla
curl -X POST http://localhost:8106/api/integrations/servicenow/distribution-rules \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "schedule_id": "<schedule-uuid>",
    "report_template_id": "<template-uuid>",
    "recipients": "budget-team@company.com",
    "format": "XLSX",
    "enabled": true
  }'
```

### 4.4 Planovane DB schema: budget

V budoucnosti se pocita s fyzickym oddelenim budgetovych dat do vlastniho schematu:

```
Aktualni stav (vsechno v data schema):
┌─────────────────────────────────┐
│         data schema             │
│  parsed_tables (OPEX + Budget)  │
│  documents                      │
│  form_responses                 │
└─────────────────────────────────┘

Planovany stav (oddelene schemata):
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  data schema │  │ budget schema│  │  opex schema │
│  parsed_     │  │  budget_     │  │  opex_       │
│  tables      │  │  items       │  │  items       │
│  (universal) │  │  cost_       │  │  cost_       │
│              │  │  centers     │  │  categories  │
│              │  │  actuals     │  │  actuals     │
│              │  │  vs_plan     │  │  vs_plan     │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 4.5 Promoted Tables — mechanismus pro schema separaci

System jiz podporuje mechanismus **table promotion** — prevod JSONB dat z `parsed_tables` do dedikovaných tabulek:

```sql
-- Registry promovanych tabulek
SELECT * FROM data.promoted_tables_registry;

-- Priklad zaznamu:
-- id:               promo-uuid-1
-- table_name:       budget.budget_items
-- ddl_applied:      CREATE TABLE budget.budget_items (...)
-- dual_write_until: 2026-06-01 00:00:00+00  (datum cutoveru)
-- status:           ACTIVE
```

**Zivotni cyklus promoted table:**

```
CREATING → ACTIVE → MIGRATING → DISABLED
```

| Faze | Popis |
|------|-------|
| `CREATING` | DDL se aplikuje, tabulka se vytvari |
| `ACTIVE` | Dual-write — data se zapisuji do parsed_tables I do promoted tabulky |
| `MIGRATING` | Historicka data se migruji z parsed_tables |
| `DISABLED` | Stara data v parsed_tables smazana, vsechno jen v promoted tabulce |

### 4.6 Excel import pro Budget

Budget data lze importovat i pres Excel — stejny postup jako pro OPEX:

```bash
# Upload budget Excelu
curl -X POST http://localhost:8082/api/upload \
  -H "X-Org-Id: <org-uuid>" \
  -H "X-User-Id: <user-uuid>" \
  -F "file=@Budget_2026.xlsx" \
  -F "upload_purpose=PARSE"
```

Data se ulozi do `parsed_tables` s `source_sheet` odpovídajícím nazvu Excel sheetu. Nasledne se mohou promovat do dedikovaného `budget` schematu.

---

## 5. Principy fyzickeho oddeleni streamu

### 5.1 Proc oddelovat?

| Duvod | Vysvetleni |
|-------|-----------|
| Vykonnost | Dedikované tabulky s typovanymi sloupci jsou rychlejsi nez JSONB |
| Integrita | CHECK constraints, FK references, NOT NULL — nelze na JSONB |
| Indexy | B-tree/GIN indexy na konkretnich sloupcich |
| Compliance | Auditni pozadavky — jasne oddeleni datovych domen |
| Scaling | Kazdy stream muze mit vlastni retention policy |

### 5.2 Postup pridani noveho streamu

1. **Definovat schema** — Flyway migrace s `CREATE SCHEMA` a tabulkami
2. **Vytvorit service user** — pristupy v `09_p8_consolidated_users.sql`
3. **Mapping template** — pravidla pro normalizaci sloupcu z Excelu/SNow
4. **Promoted table registry** — registrace pro dual-write
5. **Dashboard widgety** — SQL dotazy nad novym schematem
6. **Formulare** — volitelne, pro manualni zadani dat

### 5.3 Priklad: Flyway migrace pro novy stream

```sql
-- V1_0_1__budget_create_tables.sql

CREATE SCHEMA IF NOT EXISTS budget;

CREATE TABLE budget.budget_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL,
    period_id       UUID NOT NULL,
    cost_center     VARCHAR(255) NOT NULL,
    category        VARCHAR(255) NOT NULL,
    description     TEXT,
    planned_amount  NUMERIC(15,2) NOT NULL,
    actual_amount   NUMERIC(15,2),
    currency        VARCHAR(3) DEFAULT 'EUR',
    source          VARCHAR(50) NOT NULL DEFAULT 'MANUAL',  -- MANUAL, SNOW, EXCEL
    source_ref      VARCHAR(255),  -- reference na zdrojovy zaznam
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- RLS
ALTER TABLE budget.budget_items ENABLE ROW LEVEL SECURITY;
CREATE POLICY budget_org_isolation ON budget.budget_items
    USING (org_id = current_setting('app.current_org_id')::UUID);

-- Indexy
CREATE INDEX idx_budget_org_period ON budget.budget_items (org_id, period_id);
CREATE INDEX idx_budget_cost_center ON budget.budget_items (cost_center);

CREATE TABLE budget.budget_vs_plan (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_item_id  UUID NOT NULL REFERENCES budget.budget_items(id),
    period_id       UUID NOT NULL,
    planned         NUMERIC(15,2) NOT NULL,
    actual          NUMERIC(15,2),
    variance        NUMERIC(15,2) GENERATED ALWAYS AS (actual - planned) STORED,
    variance_pct    NUMERIC(5,2) GENERATED ALWAYS AS (
                      CASE WHEN planned != 0 THEN ((actual - planned) / planned * 100)
                      ELSE NULL END
                    ) STORED,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

### 5.4 Priklad: Service user pro novy stream

```sql
-- V doplnku k 09_p8_consolidated_users.sql

CREATE USER budget_user WITH PASSWORD 'password';
GRANT USAGE ON SCHEMA budget TO budget_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA budget TO budget_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA budget TO budget_user;

-- Read-only pristup pro query service
GRANT USAGE ON SCHEMA budget TO ms_qry;
GRANT SELECT ON ALL TABLES IN SCHEMA budget TO ms_qry;
```

---

## 6. Roadmapa datovych streamu

| Stream | Schema | Zdroj dat | Status | Poznamka |
|--------|--------|-----------|--------|---------|
| OPEX | `data` (parsed_tables) | Excel, PPTX, Formulare | Aktivni | Plne implementovano |
| Univerzalni Reporting | `reporting` | Formulare, parsed_tables | Aktivni | Lifecycle + generovani PPTX |
| Budget Reporting | `budget` (planovano) | ServiceNow, Excel | Planovano | SNow infrastruktura existuje |
| CAPEX (kapitalove vydaje) | `capex` (budouci) | ServiceNow, Excel | Koncept | Po Budget streamu |
| HR Reporting | `hr` (budouci) | ServiceNow, Excel | Koncept | Personalní data |
| Custom Streams | dedicke schema | Libovolny | Koncept | Genericke rozhrani pro zakazniky |

### 6.1 Priorita implementace

```
Q2 2026:  Budget Reporting (SNow sync + Excel import)
Q3 2026:  OPEX schema separace (promote z parsed_tables)
Q4 2026:  CAPEX stream
2027+:    HR Reporting, Custom Streams
```

---

## 7. Integracni body

### 7.1 Jak streamy spolupracuji

```
ServiceNow ──► engine-integrations ──► parsed_tables ──► promoted table
                                                              │
Excel Upload ──► engine-ingestor ──► parsed_tables ────────────┤
                                                              │
Manual Form ──► engine-reporting ──► form_responses            │
                                                              │
                                               ┌──────────────┘
                                               ▼
                                    Dashboard (Custom SQL)
                                               │
                                               ▼
                                    Reports → PPTX/Excel/PDF
```

### 7.2 API pro cross-stream dotazy

Custom SQL widget umoznuje dotazovat data z ruznych streamu dohromady:

```sql
-- Budget vs OPEX porovnani (po zavedeni budget schematu)
SELECT
  b.cost_center AS LabelX,
  b.planned_amount - COALESCE(o.actual_amount, 0) AS LabelY
FROM budget.budget_items b
LEFT JOIN opex.opex_items o ON o.cost_center = b.cost_center
  AND o.period_id = b.period_id
WHERE b.period_id = '<period-uuid>'
ORDER BY LabelY DESC
```

---

## 8. Souvisejici dokumentace

- [Data Import API Guide](Data_Import_API_Guide.md) — nahravani souboru pres API
- [SlideMetadata Guide](SlideMetadata_Guide.md) — metadata sablony pro PPTX extrakci
- [Reports & Templates Guide](Reports_Templates_Guide.md) — sprava reportu a PPTX sablon
- [Dashboards](Dashboards.md) — Custom SQL widgety nad daty
