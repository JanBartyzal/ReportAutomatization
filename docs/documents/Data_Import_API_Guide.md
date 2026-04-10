# Data Import & API — Uzivatelsky pruvodce

Dokumentace pro napojeni na API rozhrani RA platformy a automatizovany import dat. Zahrnuje nahravani Excelu, spravu batchu, dotazovani na extrahovaná data a integraci s externimi systemy.

---

## Prehled workflow

```
1. Autentizace            Header X-Org-Id + X-User-Id     (kazdy request)
2. Upload souboru         POST /api/upload                 (Excel, PPTX, CSV, PDF)
3. Automaticke zpracovani engine-orchestrator              (automaticky)
4. Dotaz na vysledek      GET /api/query/files/{id}/data   (po zpracovani)
5. Organizace do batchu   POST /api/batches                (volitelne)
6. Agregace & export      POST /api/query/aggregate        (analyza)
```

---

## 1. Autentizace a hlavicky

Kazdy API request musi obsahovat hlavicky pro identifikaci uzivatele a organizace:

| Hlavicka | Typ | Povinne | Popis |
|----------|-----|:-------:|-------|
| `X-Org-Id` | UUID | Ano | ID organizace (tenant) |
| `X-User-Id` | UUID | Ano | ID prihlaseneho uzivatele |
| `Content-Type` | string | Dle endpointu | `multipart/form-data` pro upload, `application/json` pro ostatni |

```bash
# Priklad hlavicek
curl -H "X-Org-Id: 550e8400-e29b-41d4-a716-446655440000" \
     -H "X-User-Id: 660e8400-e29b-41d4-a716-446655440001" \
     http://localhost:8082/api/upload
```

> **Row-Level Security:** System automaticky filtruje data podle `X-Org-Id`. Uzivatel vidi pouze data sve organizace.

---

## 2. Upload souboru (Excel)

### 2.1 Zakladni upload

```bash
curl -X POST http://localhost:8082/api/upload \
  -H "X-Org-Id: <org-uuid>" \
  -H "X-User-Id: <user-uuid>" \
  -F "file=@Test.xlsx" \
  -F "upload_purpose=PARSE"
```

**Response (201):**
```json
{
  "file_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "filename": "Test.xlsx",
  "size_bytes": 45230,
  "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "scan_status": "CLEAN",
  "upload_purpose": "PARSE",
  "created_at": "2026-04-10T08:30:00Z"
}
```

### 2.2 Upload purpose

| Ucel | Hodnota | Popis |
|------|---------|-------|
| Parsovani dat | `PARSE` | Standardni zpracovani — extrakce tabulek do `parsed_tables` |
| Import do formulare | `FORM_IMPORT` | Data se mapuji na existujici formularova pole |

### 2.3 Podporovane formaty

| Format | MIME Type | Poznamka |
|--------|-----------|---------|
| Excel (.xlsx) | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | Hlavni format pro datovy import |
| Excel s makry (.xlsm) | `application/vnd.ms-excel.sheet.macroEnabled.12` | VBA makra jsou automaticky odstranena |
| PPTX (.pptx) | `application/vnd.openxmlformats-officedocument.presentationml.presentation` | Extrakce slidu a pseudotabulek |
| PPTX s makry (.pptm) | `application/vnd.ms-powerpoint.presentation.macroEnabled.12` | VBA makra automaticky odstranena |
| PDF (.pdf) | `application/pdf` | Extrakce textu a tabulek (s OCR) |
| CSV (.csv) | `text/csv` | Parsovani s konfigurovatelnym delimiterem |

### 2.4 Bezpecnostni pipeline

Kazdy soubor prochazi bezpecnostnim pipeline pred zpracovanim:

```
1. MIME Validace      — overeni koncovky, content type, magic bytes, velikosti
2. ClamAV Scan        — antivirova kontrola obsahu souboru
3. VBA Macro Removal  — odstraneni maker z .xlsm/.pptm souboru
4. Blob Upload        — ulozeni do blob storage ({orgId}/{fileId}/{filename})
5. DB Persistence     — zaznam v tabulce files se scan_status = CLEAN
6. Event Publishing   — FileUploadedEvent pres Dapr pub/sub
```

> **Infikovany soubor:** Pokud ClamAV detekuje hrozbu, API vrati `HTTP 422 Unprocessable Entity` a soubor neni ulozen.

---

## 3. Zpracovani souboru (automaticky pipeline)

Po uploadu se automaticky spusti zpracovatelsky workflow:

```
Upload (engine-ingestor)
  │ FileUploadedEvent (Dapr pub/sub)
  ▼
Orchestrator (engine-orchestrator)
  │
  ├─ Step 1: SCAN    → Atomizer detekuje typ souboru
  ├─ Step 2: PARSE   → Atomizer extrahuje data (headers, rows)
  ├─ Step 3: MAP     → engine-data mapuje sloupce (mapping templates)
  └─ Step 4: STORE   → engine-data ulozi do parsed_tables + documents
```

### 3.1 Sledovani stavu zpracovani

```bash
# Status souboru
curl http://localhost:8082/api/files/<file-id> \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
{
  "id": "a1b2c3d4-...",
  "filename": "Test.xlsx",
  "status": "COMPLETED",
  "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "size_bytes": 45230,
  "created_at": "2026-04-10T08:30:00Z"
}
```

| Status | Popis |
|--------|-------|
| `UPLOADED` | Soubor nahran, ceka na zpracovani |
| `PROCESSING` | Workflow bezi |
| `COMPLETED` | Uspesne zpracovano |
| `FAILED` | Chyba pri zpracovani |
| `PARTIAL` | Castecne zpracovano (nektere sheety selhaly) |

### 3.2 Processing log

```bash
curl http://localhost:8100/api/query/processing-logs/<file-id> \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
[
  {
    "step_name": "SCAN",
    "status": "COMPLETED",
    "duration_ms": 120,
    "created_at": "2026-04-10T08:30:01Z"
  },
  {
    "step_name": "PARSE",
    "status": "COMPLETED",
    "duration_ms": 850,
    "created_at": "2026-04-10T08:30:02Z"
  },
  {
    "step_name": "MAP",
    "status": "COMPLETED",
    "duration_ms": 200,
    "created_at": "2026-04-10T08:30:03Z"
  },
  {
    "step_name": "STORE",
    "status": "COMPLETED",
    "duration_ms": 150,
    "created_at": "2026-04-10T08:30:03Z"
  }
]
```

### 3.3 Znovu-zpracovani souboru

Pokud zpracovani selhalo nebo chcete aktualizovat data po zmene mapping sablony:

```bash
curl -X POST http://localhost:8082/api/files/<file-id>/reprocess \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
{
  "status": "REPROCESS_TRIGGERED",
  "file_id": "a1b2c3d4-..."
}
```

---

## 4. Dotazovani na extrahována data

### 4.1 Data souboru (tabulky + dokumenty)

```bash
curl http://localhost:8100/api/query/files/<file-id>/data \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
{
  "fileId": "a1b2c3d4-...",
  "filename": "Test.xlsx",
  "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  "tables": [
    {
      "id": "tbl-uuid-1",
      "source_sheet": "Sheet1",
      "headers": ["Id", "Project Name", "Budget24", "Budget25", "Cost24", "Cost25"],
      "rows": [
        ["1", "Project Alpha", "100000", "120000", "95000", "110000"],
        ["2", "Project Beta", "80000", "90000", "78000", "85000"]
      ],
      "metadata": {"table_index": 0}
    }
  ],
  "documents": []
}
```

### 4.2 Seznam parsed tabulek (s filtrovanim)

```bash
curl "http://localhost:8100/api/query/tables?file_id=<file-id>&source_sheet=Sheet1&page=0&size=20" \
  -H "X-Org-Id: <org-uuid>"
```

### 4.3 Konkretni Excel sheet

```bash
curl http://localhost:8100/api/query/files/<file-id>/sheets/0 \
  -H "X-Org-Id: <org-uuid>"
```

### 4.4 PPTX slidy

```bash
curl http://localhost:8100/api/query/files/<file-id>/slides \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
{
  "fileId": "...",
  "filename": "DemoPage.pptx",
  "slides": [
    {
      "slideIndex": 0,
      "title": "Cost optimization",
      "texts": ["..."],
      "tables": [
        {
          "headers": ["Cost Category", "Net saving 2024"],
          "rows": [["IT", "-21.4"], ["HR", "-3.2"]]
        }
      ],
      "imageUrl": null,
      "notes": ""
    }
  ]
}
```

### 4.5 Agregace dat

Dynamicka agregace nad `parsed_tables`:

```bash
curl -X POST http://localhost:8100/api/query/aggregate \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "group_by": "source_sheet",
    "metric": "row_count",
    "aggregation": "SUM"
  }'
```

| Agregace | Popis |
|----------|-------|
| `SUM` | Soucet hodnot |
| `AVG` | Prumer |
| `DETAIL` | Detailni vypis bez agregace |

### 4.6 Export do Excelu

```bash
curl -o export.xlsx \
  "http://localhost:8100/api/query/tables/export/excel?file_id=<file-id>&source_sheet=Sheet1" \
  -H "X-Org-Id: <org-uuid>"
```

### 4.7 Custom SQL dotazy (Dashboard)

Pro pokrocile dotazy pouzijte Custom SQL widget:

```bash
curl -X POST http://localhost:8100/api/dashboards/sql/execute \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "sql": "SELECT r.value->>1 AS \"Project\", CAST(r.value->>9 AS NUMERIC) AS \"Cost\" FROM data.parsed_tables pt JOIN ingestor.files f ON f.id::text = pt.file_id CROSS JOIN jsonb_array_elements(pt.rows) AS r(value) WHERE f.filename = '\''Test.xlsx'\'' ORDER BY f.created_at DESC LIMIT 100"
  }'
```

Vice SQL vzoru viz [Dashboards.md](Dashboards.md).

---

## 5. Sprava batchu

Batche organizuji soubory do logickych skupin — typicky pro jedno reportovaci obdobi.

### 5.1 Vytvoreni batche

```bash
curl -X POST http://localhost:8081/api/batches \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "name": "OPEX Q1 2026",
    "period": "Q1 2026",
    "period_id": "<period-uuid>",
    "description": "OPEX data pro prvni kvartal 2026",
    "holding_id": "<holding-uuid>",
    "created_by": "<user-uuid>"
  }'
```

**Response (201):**
```json
{
  "id": "batch-uuid-1",
  "name": "OPEX Q1 2026",
  "period": "Q1 2026",
  "status": "OPEN",
  "created_at": "2026-04-10T09:00:00Z"
}
```

### 5.2 Zivotni cyklus batche

```
OPEN → COLLECTING → CLOSED
```

| Stav | Popis |
|------|-------|
| `OPEN` | Batch vytvoren, lze pridavat soubory |
| `COLLECTING` | Aktivni sber dat |
| `CLOSED` | Batch uzavren, soubory nelze pridavat |

### 5.3 Pridani souboru do batche

```bash
curl -X POST http://localhost:8081/api/batches/<batch-id>/files \
  -H "Content-Type: application/json" \
  -d '{"file_id": "<file-uuid>"}'
```

### 5.4 Seznam souboru v batchi

```bash
curl http://localhost:8081/api/batches/<batch-id>/files
```

### 5.5 Status batche

```bash
curl http://localhost:8081/api/batches/<batch-id>/status
```

**Response:**
```json
{
  "batch_id": "batch-uuid-1",
  "status": "COLLECTING",
  "file_count": 5,
  "period": "Q1 2026"
}
```

### 5.6 Uzavreni batche

```bash
curl -X PUT http://localhost:8081/api/batches/<batch-id> \
  -H "Content-Type: application/json" \
  -d '{"status": "CLOSED"}'
```

---

## 6. Automatizovany import — kompletni priklad

### 6.1 Scenar: Mesicni import OPEX dat

```bash
#!/bin/bash
ORG_ID="550e8400-e29b-41d4-a716-446655440000"
USER_ID="660e8400-e29b-41d4-a716-446655440001"
API_BASE="http://localhost"

# 1. Vytvorit batch pro obdobi
BATCH=$(curl -s -X POST $API_BASE:8081/api/batches \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: $ORG_ID" \
  -d '{
    "name": "OPEX April 2026",
    "period": "2026-04",
    "holding_id": "'$ORG_ID'",
    "created_by": "'$USER_ID'"
  }')
BATCH_ID=$(echo $BATCH | jq -r '.id')
echo "Batch created: $BATCH_ID"

# 2. Nahrat Excel soubory
for FILE in ./data/opex_*.xlsx; do
  UPLOAD=$(curl -s -X POST $API_BASE:8082/api/upload \
    -H "X-Org-Id: $ORG_ID" \
    -H "X-User-Id: $USER_ID" \
    -F "file=@$FILE" \
    -F "upload_purpose=PARSE")
  FILE_ID=$(echo $UPLOAD | jq -r '.file_id')
  echo "Uploaded: $(basename $FILE) → $FILE_ID"

  # 3. Pridat do batche
  curl -s -X POST $API_BASE:8081/api/batches/$BATCH_ID/files \
    -H "Content-Type: application/json" \
    -d '{"file_id": "'$FILE_ID'"}'
done

# 4. Pockat na zpracovani (polling)
echo "Waiting for processing..."
sleep 10

# 5. Overit vysledky
for FILE_ID in $(curl -s $API_BASE:8081/api/batches/$BATCH_ID/files | jq -r '.[].file_id'); do
  STATUS=$(curl -s $API_BASE:8082/api/files/$FILE_ID -H "X-Org-Id: $ORG_ID" | jq -r '.status')
  echo "File $FILE_ID: $STATUS"
done

# 6. Uzavrit batch
curl -s -X PUT $API_BASE:8081/api/batches/$BATCH_ID \
  -H "Content-Type: application/json" \
  -d '{"status": "CLOSED"}'
echo "Batch closed."
```

### 6.2 Scenar: Import do formulare

Pro import Excelu do existujiciho formulare pouzijte `upload_purpose=FORM_IMPORT`:

```bash
# Upload s urcenim FORM_IMPORT
curl -X POST http://localhost:8082/api/upload \
  -H "X-Org-Id: <org-uuid>" \
  -H "X-User-Id: <user-uuid>" \
  -F "file=@opex_data.xlsx" \
  -F "upload_purpose=FORM_IMPORT"
```

Workflow pro FORM_IMPORT:
```
1. Parse Excel             processor-atomizers → headers + rows
2. Mapping Suggestions     engine-data:template → navrhne mapovani sloupcu na formularova pole
3. User Confirmation       Dapr event form.import.mapping_suggestions → frontend zobrazi navrh
4. Potvrzeni               uzivatel potvrdí/upravi mapovani
5. Store                   data se ulozi do form_responses
```

---

## 7. Sprava souboru

### 7.1 Seznam souboru (strankovaný)

```bash
curl "http://localhost:8082/api/files?page=0&size=20" \
  -H "X-Org-Id: <org-uuid>"
```

**Response:**
```json
{
  "items": [
    {
      "id": "a1b2c3d4-...",
      "filename": "Test.xlsx",
      "size_bytes": 45230,
      "mime_type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "status": "COMPLETED",
      "created_at": "2026-04-10T08:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "total": 42
}
```

### 7.2 Detail souboru

```bash
curl http://localhost:8082/api/files/<file-id> \
  -H "X-Org-Id: <org-uuid>"
```

---

## 8. Mapping templates (normalizace sloupcu)

System automaticky normalizuje nazvy sloupcu pres mapping templates pri kroku MAP. Toto zajistuje konzistentni data bez ohledu na to, jak zdrojovy Excel pojmenovava sloupce.

### 8.1 Jak funguje mapping

```
Excel sloupec "Nazev projektu"  →  mapping rule SYNONYM  →  target "project_name"
Excel sloupec "Project Name"    →  mapping rule EXACT     →  target "project_name"
Excel sloupec "Proj. nazev"     →  mapping rule REGEX     →  target "project_name"
Excel sloupec "nazov projektu"  →  mapping rule AI        →  target "project_name"
```

### 8.2 Typy pravidel

| Typ | Popis | Priklad |
|-----|-------|---------|
| `EXACT_MATCH` | Presna shoda nazvu sloupce | `"Project Name" → "project_name"` |
| `SYNONYM` | Synonyma | `"Nazev projektu" → "project_name"` |
| `REGEX` | Regularni vyraz | `"Proj\.?\s*n[aá]zev" → "project_name"` |
| `AI_SUGGESTED` | AI navrhne mapovani | Automaticky pres LiteLLM gateway |

### 8.3 API pro mapping templates

```bash
# Seznam mapping sablon
curl http://localhost:8100/api/query/templates/schema-mapping \
  -H "X-Org-Id: <org-uuid>"

# Vytvoreni mapping sablony
curl -X POST http://localhost:8100/api/query/templates/schema-mapping \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d '{
    "name": "OPEX Standard Mapping",
    "rules": [
      {"rule_type": "EXACT_MATCH", "source_pattern": "Project Name", "target_column": "project_name", "priority": 100},
      {"rule_type": "SYNONYM", "source_pattern": "Nazev projektu", "target_column": "project_name", "priority": 90},
      {"rule_type": "EXACT_MATCH", "source_pattern": "Total Cost", "target_column": "total_cost", "priority": 100}
    ]
  }'
```

---

## 9. Datovy model v databazi

### 9.1 Struktura parsed_tables

Po zpracovani souboru se data ukladaji do `data.parsed_tables`:

| Sloupec | Typ | Popis |
|---------|-----|-------|
| `id` | UUID | PK |
| `file_id` | VARCHAR | Odkaz na zdrojovy soubor |
| `org_id` | VARCHAR | Organizace (tenant) |
| `source_sheet` | VARCHAR | Nazev Excel sheetu nebo PPTX pseudo-tabulky |
| `headers` | JSONB | `["Col1", "Col2", ...]` |
| `rows` | JSONB | `[["val1", "val2"], ["val3", "val4"]]` |
| `metadata` | JSONB | Doplnkove info (table_index, detected_type) |
| `created_at` | TIMESTAMPTZ | Datum extrakce |

### 9.2 Struktura files

| Sloupec | Typ | Popis |
|---------|-----|-------|
| `id` | UUID | PK |
| `org_id` | UUID | Organizace |
| `filename` | VARCHAR | Nazev souboru |
| `size_bytes` | BIGINT | Velikost v bajtech |
| `mime_type` | VARCHAR | MIME typ |
| `blob_url` | VARCHAR | URL v blob storage |
| `scan_status` | VARCHAR | PENDING, CLEAN, INFECTED, ERROR |
| `upload_purpose` | VARCHAR | PARSE, FORM_IMPORT |
| `created_at` | TIMESTAMPTZ | Datum uploadu |

---

## 10. API endpointy — shrnuti

### engine-ingestor (port 8082)

| Endpoint | Metoda | Popis |
|----------|--------|-------|
| `/api/upload` | POST | Upload souboru (multipart/form-data) |
| `/api/files` | GET | Seznam souboru (strankovaný) |
| `/api/files/{file_id}` | GET | Detail souboru |
| `/api/files/{file_id}/reprocess` | POST | Znovu-zpracovani souboru |

### engine-core (port 8081) — Batches

| Endpoint | Metoda | Popis |
|----------|--------|-------|
| `/api/batches` | GET | Seznam batchu |
| `/api/batches` | POST | Vytvoreni batche |
| `/api/batches/{id}` | PUT | Aktualizace batche (status) |
| `/api/batches/{id}` | DELETE | Smazani batche |
| `/api/batches/{id}/status` | GET | Status batche |
| `/api/batches/{id}/files` | GET | Soubory v batchi |
| `/api/batches/{id}/files` | POST | Pridani souboru do batche |
| `/api/batches/{id}/files/{fileId}` | DELETE | Odebrani souboru z batche |

### engine-data (port 8100) — Query

| Endpoint | Metoda | Popis |
|----------|--------|-------|
| `/api/query/files/{file_id}/data` | GET | Extrahována data souboru |
| `/api/query/files/{file_id}/slides` | GET | PPTX slidy |
| `/api/query/files/{file_id}/sheets/{n}` | GET | Konkretni Excel sheet |
| `/api/query/tables` | GET | Seznam parsed tabulek (filtrovane) |
| `/api/query/tables/export/excel` | GET | Export do Excelu |
| `/api/query/processing-logs/{file_id}` | GET | Processing log |
| `/api/query/aggregate` | POST | Dynamicka agregace |
| `/api/query/documents` | GET | Dotaz na dokumenty |
| `/api/dashboards/sql/execute` | POST | Custom SQL dotaz |

---

## 11. Troubleshooting

| Problem | Pricina | Reseni |
|---------|---------|--------|
| Upload vrati 422 | Infikovany soubor | Zkontrolujte soubor antivirem |
| Upload vrati 415 | Nepodporovany format | Zkontrolujte MIME typ a koncovku souboru |
| Status FAILED | Chyba pri zpracovani | Zkontrolujte processing log |
| Status PARTIAL | Nektere sheety selhaly | Zkontrolujte log — spatna data v nekterem sheetu |
| Prazdne parsed_tables | Mapping nenalezeno | Zkontrolujte/vytvorte mapping template |
| Pomale zpracovani | Velky soubor | Rozdelete na mensi soubory nebo zvyste timeout |
| Batch nejde uzavrit | Soubory jeste zpracovavaji | Pockejte na COMPLETED status vsech souboru |

---

## 12. Souvisejici dokumentace

- [Dashboards](Dashboards.md) — Custom SQL dotazy nad extrahovanymi daty
- [Reports & Templates Guide](Reports_Templates_Guide.md) — generovani reportu z dat
- [SlideMetadata Guide](SlideMetadata_Guide.md) — metadata sablony pro PPTX
- [Data Streams Guide](Data_Streams_Guide.md) — architektura datovych streamu
