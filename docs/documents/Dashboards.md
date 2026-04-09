# Dashboard — Custom SQL Widgety

Dokumentace pro tvorbu Custom SQL widgetů v RA Dashboard systemu.

## Architektura

```
Frontend (DashboardViewerPage)
  │  POST /api/dashboards/sql/execute  { sql: "..." }
  ▼
DashboardController (engine-data :8100)
  │
  ▼
AggregationService.executeRawSql()
  │  - Validace: pouze SELECT dotazy
  │  - Injekce org_id filtru (RLS)
  │  - Limit: 1000 řádků
  ▼
PostgreSQL
  │  data.parsed_tables      (schema: data)
  │  data.sink_corrections   (schema: data)
  │  ingestor.files          (schema: ingestor)
  │  core.organizations      (schema: core)
```

## Datový model

### DB schemata

| Schema | Service | Tabulky |
|--------|---------|---------|
| `data` | engine-data | `parsed_tables`, `sink_corrections`, `dashboards` |
| `ingestor` | engine-ingestor | `files` |
| `core` | engine-core | `organizations`, `users`, `batches`, `batch_files` |

> **Dulezite:** V Custom SQL widgetech je nutne pouzivat plne kvalifikovane nazvy tabulek
> (napr. `data.parsed_tables`, `ingestor.files`), protoze `search_path` engine-data
> obsahuje pouze schema `data`.

### `data.parsed_tables` — sink tabulka pro extrahovaná data

| Sloupec | Typ | Popis |
|---------|-----|-------|
| `id` | UUID | PK |
| `file_id` | VARCHAR(255) | Odkaz na `files.id` (jako text) |
| `org_id` | VARCHAR(255) | Organizace |
| `source_sheet` | VARCHAR(255) | Název sheetu (Excel) nebo pseudo-tabulky (PPTX) |
| `headers` | JSONB | `["Col1", "Col2", ...]` |
| `rows` | JSONB | `[["val1", "val2"], ["val3", "val4"], ...]` |
| `metadata` | JSONB | Doplňkové info (source_type apod.) |
| `created_at` | TIMESTAMPTZ | Datum extrakce |

### `ingestor.files` — nahrane soubory

| Sloupec | Typ | Popis |
|---------|-----|-------|
| `id` | UUID | PK |
| `org_id` | UUID | Organizace |
| `filename` | VARCHAR(512) | Nazev souboru (např. `Test.xlsx`) |
| `created_at` | TIMESTAMPTZ | Datum uploadu |

**JOIN:** `ingestor.files.id::text = data.parsed_tables.file_id`

---

## Pravidla pro chart widgety

Aby chart (BAR, LINE, PIE) fungoval, musí SQL vracet slouce s aliasy:

| Alias | Ucel |
|-------|------|
| `LabelX` | Osa X / kategorie / popisek |
| `LabelY` | Osa Y / hodnota (numericky) |

Pro TABLE widget aliasy nehraji roli — zobrazí se všechny sloupce.

---

## Vzory SQL dotazu

### 1. Nejnovejsi data ze souboru podle nazvu

Klicovy pattern: `JOIN ingestor.files` + `WHERE f.filename = '...'` + `ORDER BY f.created_at DESC LIMIT 1`

```sql
-- Hlavicky posledniho uploadu souboru
SELECT h.val AS LabelX, h.ord AS LabelY
FROM data.parsed_tables pt
JOIN ingestor.files f ON f.id::text = pt.file_id
CROSS JOIN jsonb_array_elements_text(pt.headers) WITH ORDINALITY AS h(val, ord)
WHERE f.filename = 'Test.xlsx'
  AND f.created_at = (
    SELECT MAX(f2.created_at) FROM ingestor.files f2
    WHERE f2.filename = f.filename AND f2.org_id = f.org_id
  )
```

### 2. Cela tabulka z posledniho uploadu

```sql
WITH latest AS (
  SELECT pt.headers, pt.rows
  FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'Test.xlsx'
  ORDER BY f.created_at DESC
  LIMIT 1
)
SELECT
  r.value->>0 AS "Id",
  r.value->>1 AS "Project Name",
  r.value->>2 AS "Max project cost",
  r.value->>3 AS "Budget24",
  r.value->>4 AS "Budget 25",
  r.value->>5 AS "Budget 26",
  r.value->>6 AS "Cost24",
  r.value->>7 AS "Cost25",
  r.value->>8 AS "Cost26",
  r.value->>9 AS "TotalCost",
  r.value->>10 AS "ToalBudget",
  r.value->>11 AS "DiffCost",
  r.value->>12 AS "DiffBudget"
FROM latest l, jsonb_array_elements(l.rows) AS r(value)
```

> **Poznamka:** Indexy `r.value->>0`, `r.value->>1` atd. odpovídají pořadí sloupců v `headers`.
> Pro dynamický přístup (když neznáš pozici) použij vzor #5.

### 3. Agregace — SUM po sloupcich

```sql
-- Total Cost per projekt (bar chart)
WITH latest AS (
  SELECT pt.rows FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'Test.xlsx'
  ORDER BY f.created_at DESC LIMIT 1
)
SELECT
  r.value->>1 AS LabelX,                          -- Project Name
  CAST(r.value->>9 AS NUMERIC) AS LabelY           -- TotalCost
FROM latest l, jsonb_array_elements(l.rows) AS r(value)
ORDER BY LabelY DESC
```

### 4. Trend po letech (UNION pro vice sloupcu)

```sql
-- Cost 2024/2025/2026 jako line chart
WITH latest AS (
  SELECT pt.rows FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'Test.xlsx'
  ORDER BY f.created_at DESC LIMIT 1
),
years AS (
  SELECT '2024' AS yr, 6 AS idx       -- Cost24 je na pozici 6
  UNION ALL SELECT '2025', 7           -- Cost25 na pozici 7
  UNION ALL SELECT '2026', 8           -- Cost26 na pozici 8
)
SELECT
  y.yr AS LabelX,
  SUM(CAST(r.value->>y.idx AS NUMERIC)) AS LabelY
FROM latest l,
  jsonb_array_elements(l.rows) AS r(value),
  years y
GROUP BY y.yr
ORDER BY y.yr
```

### 5. Dynamicky pristup ke sloupcum (podle nazvu)

Když neznas pozici sloupce, vyhledej ji z `headers`:

```sql
WITH latest AS (
  SELECT pt.headers, pt.rows
  FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'OPEX_Summary.xlsx'
  ORDER BY f.created_at DESC LIMIT 1
),
col_idx AS (
  SELECT
    (SELECT ord-1 FROM jsonb_array_elements_text(l.headers)
     WITH ORDINALITY AS h(val,ord) WHERE h.val = 'Organisation' LIMIT 1) AS org_idx,
    (SELECT ord-1 FROM jsonb_array_elements_text(l.headers)
     WITH ORDINALITY AS h(val,ord) WHERE h.val = 'Amount' LIMIT 1) AS amt_idx,
    l.rows
  FROM latest l
)
SELECT
  r.value->>CAST(c.org_idx AS int) AS LabelX,
  SUM(CAST(r.value->>CAST(c.amt_idx AS int) AS NUMERIC)) AS LabelY
FROM col_idx c, jsonb_array_elements(c.rows) AS r(value)
GROUP BY 1
ORDER BY LabelY DESC
```

### 6. PPTX pseudo-tabulky

PPTX atomizer extrahuje pseudo-tabulky ze slidu. Kazda pseudo-tabulka je jeden záznam v `parsed_tables` se `source_sheet` jako nazev tabulky.

```sql
-- Net savings z DemoPage.pptx (bar chart, WIP vyfiltrovano)
WITH latest AS (
  SELECT pt.rows FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'DemoPage.pptx'
  ORDER BY jsonb_array_length(pt.rows) DESC, f.created_at DESC
  LIMIT 1
)
SELECT
  r.value->>0 AS LabelX,                                  -- Cost Category
  CAST(NULLIF(r.value->>2, 'WIP') AS NUMERIC) AS LabelY   -- Net saving 2024
FROM latest l, jsonb_array_elements(l.rows) AS r(value)
WHERE r.value->>2 IS NOT NULL
  AND r.value->>2 != 'WIP'
  AND r.value->>2 != ''
ORDER BY LabelY
```

### 7. Metadata dotazy (bez JSONB unnest)

```sql
-- Prehled sinku per soubor
SELECT
  f.filename,
  pt.source_sheet,
  jsonb_array_length(pt.headers) AS columns,
  jsonb_array_length(pt.rows) AS rows,
  pt.created_at
FROM data.parsed_tables pt
JOIN ingestor.files f ON f.id::text = pt.file_id
ORDER BY f.created_at DESC
LIMIT 50
```

```sql
-- Prazdne tabulky (0 radku)
SELECT pt.id, f.filename, pt.source_sheet, pt.created_at
FROM data.parsed_tables pt
JOIN ingestor.files f ON f.id::text = pt.file_id
WHERE jsonb_array_length(pt.rows) = 0
ORDER BY pt.created_at DESC
```

---

## Shrnuti patternu

| Co chces | SQL pattern |
|----------|-------------|
| Vzdy stejny soubor | `JOIN ingestor.files f ON f.id::text = pt.file_id WHERE f.filename = '...'` |
| Posledni verze | `ORDER BY f.created_at DESC LIMIT 1` |
| Pristup k JSONB radkum | `jsonb_array_elements(pt.rows) AS r(value)` |
| Sloupec podle pozice | `r.value->>N` (N = 0-based index) |
| Sloupec podle nazvu | Subquery na `jsonb_array_elements_text(headers) WITH ORDINALITY` |
| Pro BAR/LINE/PIE chart | Aliasy `AS LabelX` a `AS LabelY` |
| Filtrace WIP/prazdnych | `NULLIF(r.value->>N, 'WIP')`, `WHERE r.value->>N != ''` |
| Pocet radku/sloupcu | `jsonb_array_length(pt.rows)`, `jsonb_array_length(pt.headers)` |

d## Seed dashboardy

Ukazkove dashboardy se seeduji pres init skript:

```bash
cd infra/init
.\run.ps1 --only-step dashboards
```

Konfigurace: `infra/init/init.json` sekce `DashboardSeed`.

Aktualne seedovane dashboardy:
- **OPEX Overview (Sample)** — genericke dotazy nad `parsed_tables`
- **Data Quality Monitor (Sample)** — prazdne tabulky, distribuce sloupcu, korekce
- **Test.xlsx — Project Budget Overview** — budget/cost widgety z UAT souboru
- **DemoPage.pptx — Cost Optimization** — PPTX pseudo-tabulky, net savings
