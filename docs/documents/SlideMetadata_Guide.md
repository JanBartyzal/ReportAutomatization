# Slide Metadata — Uzivatelsky pruvodce

Dokumentace pro tvorbu, spravu a pouziti Slide Metadata sablon v RA platforme. Slide Metadata definuji strukturu pseudotabulek na PPTX slidech — rikaji atomizeru KDE na slidu hledat hlavicky, sloupce a radky.

---

## Prehled workflow

```
1. Pripravit JSON sablonu       (jednorazove, admin)
2. Nahrat/seedovat sablonu      /api/query/templates/slide-metadata   (jednorazove)
3. Validovat strukturu          /validate                             (volitelne)
4. Prirazeni k souboru/org      /assign                               (volitelne)
5. Upload PPTX souboru          /api/upload                           (per soubor)
6. Automaticky matching         engine-orchestrator                   (automaticky)
7. Extrakce dat                 processor-atomizers                   (automaticky)
8. Vysledek v parsed_tables     /api/query/tables                     (dotazovani)
```

---

## 1. Co je Slide Metadata

Slide Metadata sablona je JSON soubor, ktery popisuje **strukturu** PPTX slidu — ne konkretni data. Rika atomizeru:

- **Kde** na slidu jsou sloupce (EMU pozice)
- **Jak** rozpoznat radky (horizontalni cary, vertikalni pozice)
- **Ktere** tvary ignorovat (dekorace, OLE objekty)
- **Jak** parsovat cisla (oddelovat jednotky, WIP markery)

### 1.1 Kdy pouzit Slide Metadata

| Scenar | Pouzit metadata? | Duvod |
|--------|:-:|-------|
| PPTX s nativnimi tabulkami | Ne | Atomizer je detekuje automaticky |
| PPTX s pseudo-tabulkami (text boxy, cary) | **Ano** | Bez metadat atomizer nerozpozna strukturu |
| Opakujici se layout reportu | **Ano** | Jedna sablona pro vsechny obdobi |
| Jednorazovy PPTX soubor | Mozne | Pokud je dulezita presnost extrakce |

### 1.2 Architektura

```
Admin/Init                    engine-data:template           processor-atomizers
  |                                |                              |
  | POST /slide-metadata           |                              |
  |------------------------------->| Ulozi do DB                  |
  |                                | (slide_metadata_templates)   |
  |                                |                              |
  |        engine-orchestrator     |                              |
  |             |                  |                              |
  |             | gRPC MatchTemplate                               |
  |             |----------------->|                              |
  |             |  { template_id,  |                              |
  |             |    confidence }  |                              |
  |             |<-----------------|                              |
  |             |                                                 |
  |             | if confidence >= 0.85:                          |
  |             |   gRPC ExtractWithMetadata(definition_json)     |
  |             |------------------------------------------------>|
  |             |                                                 | SpatialTableExtractor
  |             |                                                 | rekonstruuje tabulku
  |             |   { headers, rows, confidence }                 |
  |             |<------------------------------------------------|
  |             |                                                 |
  |             | else: genericke ExtractStructure (MetaTable)    |
```

---

## 2. Struktura JSON sablony

### 2.1 Korenovy objekt

```json
{
  "schema_version": "slide_metadata/v1",
  "name": "Nazev sablony",
  "version": 1,
  "source_layout": "Title and Content",
  "description": "Popis ucelu sablony",
  "slides": [ ... ],
  "parsing_rules": { ... }
}
```

| Pole | Typ | Povinne | Popis |
|------|-----|:-------:|-------|
| `schema_version` | string | Ano | Vzdy `"slide_metadata/v1"` |
| `name` | string | Ano | Unikatni nazev sablony v ramci organizace |
| `version` | int | Ano | Verze sablony (inkrementuje se pri update) |
| `source_layout` | string | Ne | Nazev PPTX layoutu pro auto-matching |
| `slides` | array | Ano | Definice jednotlivych slidu |
| `parsing_rules` | object | Ne | Globalni pravidla pro parsovani cisel a textu |

### 2.2 Definice slidu

```json
{
  "slide_index": 0,
  "role": "data_slide",
  "title_detection": {
    "shape_type": "PLACEHOLDER",
    "shape_name_pattern": "Title *"
  },
  "tables": [ ... ],
  "ignore_shapes": { ... },
  "text_elements_extraction": { ... }
}
```

| Pole | Typ | Popis |
|------|-----|-------|
| `slide_index` | int | Index slidu (0-based) |
| `role` | string | `"data_slide"`, `"title_slide"`, `"summary_slide"` |
| `title_detection` | object | Jak rozpoznat titulek slidu |
| `tables` | array | Definice pseudotabulek na slidu |
| `ignore_shapes` | object | Tvary k ignorovani pri extrakci |
| `text_elements_extraction` | object | Textove elementy mimo tabulku |

### 2.3 Definice tabulky

Kazda tabulka v `tables` poli ma tuto strukturu:

```json
{
  "table_id": "cost_optimization_main",
  "type": "pseudo_table",
  "description": "Hlavni tabulka — rekonstruovana z text boxu",
  "columns": [ ... ],
  "row_detection": { ... },
  "header_detection": { ... },
  "output_sheet_name": "Cost optimization"
}
```

| Pole | Typ | Povinne | Popis |
|------|-----|:-------:|-------|
| `table_id` | string | Ano | Unikatni ID tabulky v ramci slidu |
| `type` | string | Ano | Vzdy `"pseudo_table"` |
| `columns` | array | Ano | Definice sloupcu |
| `row_detection` | object | Ano | Metoda detekce radku |
| `header_detection` | object | Ne | Jak rozpoznat hlavicky |
| `output_sheet_name` | string | Ne | Nazev sheetu ve vyslednem `parsed_tables.source_sheet` |

### 2.4 Definice sloupce

```json
{
  "id": "net_saving",
  "header_text_pattern": "Net saving*",
  "data_type": "number",
  "allow_wip": true,
  "region": {
    "left_min_emu": 9019000,
    "left_max_emu": 10400000
  },
  "shape_name_pattern": "Rectangle *",
  "parse_format": "strip_unit_M\u20ac"
}
```

| Pole | Typ | Povinne | Popis |
|------|-----|:-------:|-------|
| `id` | string | Ano | Unikatni ID sloupce |
| `header_text_pattern` | string | Ne | Pattern pro matching hlavicky (fnmatch) |
| `data_type` | string | Ano | `"text"`, `"number"` |
| `region` | object | Ano | EMU pozice sloupce (`left_min_emu`, `left_max_emu`) |
| `shape_name_pattern` | string | Ne | Pattern nazvu tvaru (fnmatch) |
| `allow_wip` | bool | Ne | Povolit hodnoty WIP/N/A/TBD (default: false) |
| `parse_format` | string | Ne | Format parsovani: `"strip_unit_M\u20ac"` aj. |

> **EMU (English Metric Units):** PPTX pouziva EMU pro pozicovani tvaru. 1 palec = 914400 EMU. Pozice zjistite z PPTX editoru nebo z debugovaciho vystupu atomizeru.

### 2.5 Detekce radku

Dva dostupne zpusoby:

**Metoda 1: Horizontalni cary** (`horizontal_lines`)

```json
{
  "method": "horizontal_lines",
  "separator_shape_type": "LINE",
  "separator_name_pattern": "Straight Connector *",
  "data_region": {
    "top_emu": 1452442,
    "bottom_emu": 5903000
  },
  "multivalue_split": "\n"
}
```

**Metoda 2: Vertikalni pozice** (`vertical_position`)

```json
{
  "method": "vertical_position",
  "data_region": {
    "top_emu": 5950000,
    "bottom_emu": 6900000
  },
  "row_height_approx_emu": 250000
}
```

| Metoda | Kdy pouzit |
|--------|-----------|
| `horizontal_lines` | Radky jsou oddeleny carovymi tvary (Straight Connector) |
| `vertical_position` | Radky jsou oddeleny konstantni vyskou (grid layout) |

### 2.6 Ignorovane tvary

```json
{
  "ignore_shapes": {
    "patterns": [
      {"type": "EMBEDDED_OLE_OBJECT"},
      {"type": "PICTURE"},
      {"name_pattern": "Oval *", "note": "Dekoracni cisla sloupcu"},
      {"name_pattern": "object 11", "note": "Dekorativni freeform"}
    ]
  }
}
```

### 2.7 Pravidla parsovani

```json
{
  "parsing_rules": {
    "number_parsing": {
      "decimal_separator": "auto",
      "strip_patterns": ["M\u20ac", "\u20ac", " "],
      "wip_markers": ["WIP", "N/A", "TBD", "-"]
    },
    "text_cleanup": {
      "strip_whitespace": true,
      "collapse_newlines_in_cell": false
    }
  }
}
```

---

## 3. Vytvoreni sablony krok za krokem

### 3.1 Analyza PPTX souboru

1. Otevrete PPTX v PowerPointu
2. Prejdete na **View → Selection Pane** — zobrazite nazvy vsech tvaru
3. Kliknete na jednotlive tvary a zaznamenejte:
   - **Nazev tvaru** (napr. `Rectangle 5`, `object 18`)
   - **Pozici** (v PowerPointu Format → Position; prevedte na EMU * 914400)
   - **Typ tvaru** (text box, linka, obdrazek, OLE objekt)

4. Identifikujte:
   - Ktere tvary tvori **sloupce** tabulky
   - Ktere tvary jsou **oddelovace radku** (horizontalni cary)
   - Ktere tvary jsou **dekorace** k ignorovani

### 3.2 Ziskani EMU pozic

Pro presne EMU pozice pouzijte atomizer debug endpoint:

```bash
# Upload PPTX a ziskejte debug informace o tvarech
curl -X POST http://localhost:8088/api/v1/extract/pptx \
  -F "file=@DemoPage.pptx" \
  -F "debug=true" \
  | jq '.slides[0].shapes[] | {name, left, top, width, height}'
```

Vystup obsahuje EMU pozice vsech tvaru na kazdem slidu.

### 3.3 Sestaveni JSON sablony

1. Vytvorte novy JSON soubor (napr. `my_report_metadata.json`)
2. Vyplnte korenovy objekt se `schema_version`, `name`, `version`
3. Pro kazdy slide pridejte definici do `slides[]`
4. Pro kazdou pseudotabulku na slidu:
   - Definujte `columns[]` s EMU regiony
   - Zvolte `row_detection` metodu
   - Nastavte `header_detection` pokud jsou hlavicky
5. Pridejte `ignore_shapes` pro dekorativni tvary
6. Nastavte `parsing_rules` pro spravne parsovani cisel

### 3.4 Validace sablony

Pred nahranim oveerte strukturu pomoci validate endpointu:

```bash
curl -X POST http://localhost:8100/api/query/templates/slide-metadata/validate \
  -H "Content-Type: application/json" \
  -d @my_report_metadata.json
```

**Uspesna validace (200):**
```json
{
  "valid": true,
  "warnings": [],
  "slide_count": 1,
  "table_count": 2,
  "column_count": 7
}
```

**Neuspesna validace (422):**
```json
{
  "valid": false,
  "errors": [
    "slides[0].tables[0].columns[2].region.left_min_emu must be < left_max_emu"
  ]
}
```

### 3.5 Nahrani sablony

```bash
curl -X POST http://localhost:8100/api/query/templates/slide-metadata \
  -H "Content-Type: application/json" \
  -H "X-Org-Id: <org-uuid>" \
  -d @my_report_metadata.json
```

**Response (201):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Cost Optimization Report",
  "schema_version": "slide_metadata/v1",
  "version": 1,
  "created_at": "2026-03-23T10:00:00Z"
}
```

### 3.6 Preview (dry-run)

Otestujte sablonu na konkretnim PPTX souboru bez ulozeni vysledku:

```bash
curl -X POST http://localhost:8100/api/v1/templates/slide-metadata/<template-id>/preview \
  -H "Content-Type: application/json" \
  -d '{"file_id": "<pptx-file-uuid>"}'
```

**Response (200):**
```json
{
  "template_id": "550e8400-...",
  "file_id": "pptx-file-uuid",
  "extraction_result": {
    "slides": [
      {
        "slide_index": 0,
        "title": "Cost optimization",
        "tables": [
          {
            "table_id": "cost_optimization_main",
            "headers": ["Cost Category", "Main saving initiatives", "Net saving 2024"],
            "row_count": 14,
            "confidence": 0.92
          }
        ]
      }
    ]
  },
  "warnings": ["Row 13 column 'net_saving' contains non-numeric value 'WIP'"]
}
```

---

## 4. Sprava sablon

### 4.1 Seznam sablon

```bash
curl http://localhost:8100/api/query/templates/slide-metadata \
  -H "X-Org-Id: <org-uuid>"
```

### 4.2 Detail sablony

```bash
curl http://localhost:8100/api/v1/templates/slide-metadata/<template-id>
```

### 4.3 Aktualizace sablony (nova verze)

Kazda aktualizace vytvori novou verzi — stare verze zustavaji v historii:

```bash
curl -X PUT http://localhost:8100/api/v1/templates/slide-metadata/<template-id> \
  -H "Content-Type: application/json" \
  -d @my_report_metadata_v2.json
```

### 4.4 Historie verzi

```bash
curl http://localhost:8100/api/v1/templates/slide-metadata/<template-id>/versions
```

### 4.5 Prirazeni sablony

Prirazeni umoznuje automaticky matching — system navrhe sablonu pro PPTX soubor na zaklade layoutu, titulku nebo organizace:

```bash
curl -X POST http://localhost:8100/api/v1/templates/slide-metadata/<template-id>/assign \
  -H "Content-Type: application/json" \
  -d '{
    "match_criteria": {
      "slide_layout": "Title and Content",
      "title_pattern": "*Cost*"
    },
    "priority": 10
  }'
```

### 4.6 Auto-matching

System automaticky hleda sablonu pro PPTX:

```bash
curl "http://localhost:8100/api/v1/templates/slide-metadata/match?file_id=<file-uuid>"
```

**Response:**
```json
{
  "matches": [
    {
      "template_id": "550e8400-...",
      "template_name": "Cost Optimization Report",
      "score": 0.95,
      "match_reason": "layout='Title and Content' + title contains 'Cost'"
    }
  ]
}
```

---

## 5. Seedovani pres init skript

Pro automaticke nahrani sablon pri deploymentu:

### 5.1 Konfigurace v init.json

```json
{
  "SlideMetadata": {
    "Directory": "../../tests/UAT/data",
    "Enabled": true
  }
}
```

| Pole | Popis |
|------|-------|
| `Directory` | Cesta k adresari s JSON sablonovymi soubory |
| `Enabled` | `true` = seedovani se provede pri init |

### 5.2 Spusteni seedu

```bash
cd infra/init
.\run.ps1 --only-step slide-metadata
```

Skript:
1. Nacte vsechny `*.json` soubory z nakonfigurovaneho adresare
2. Filtruje soubory se `schema_version: "slide_metadata/v1"`
3. Pro kazdy soubor zavola `POST /api/query/templates/slide-metadata`
4. Zapise log uspesne/neuspesne nahrane sablony

### 5.3 Priprava seed dat

Umistete JSON soubory do adresare uvedeneho v `Directory`. Kazdy soubor musi obsahovat platnou slide metadata sablonu:

```
tests/UAT/data/
  slide_metadata.json              <-- Cost Optimization Report
  budget_report_metadata.json      <-- Budget Report (priklad)
  quarterly_opex_metadata.json     <-- OPEX Quarterly (priklad)
```

---

## 6. Automaticky workflow

Po nahrani PPTX souboru system automaticky provadi:

```
1. Upload PPTX               POST /api/upload
2. engine-orchestrator        prijme FileUploadedEvent (Dapr pub/sub)
3. MatchTemplate              engine-data:template (gRPC)
     → vrati template_id + confidence
4. Rozhodovaci logika:
     confidence >= 0.85 → metadata-driven extrakce (SpatialTableExtractor)
     confidence < 0.85  → genericka extrakce (MetaTable algoritmus)
5. Extrakce dat               processor-atomizers:pptx
6. Ulozeni vysledku           engine-data:sink-tbl (gRPC BulkInsert)
7. Data dostupna              /api/query/tables
```

### 6.1 SpatialTableExtractor vs MetaTable

| Vlastnost | SpatialTableExtractor (metadata) | MetaTable (genericky) |
|-----------|:--------------------------------:|:---------------------:|
| Presnost | Vysoka (0.9+) | Stredni (0.3-0.7) |
| Konfigurace | Vyzaduje JSON sablonu | Automaticky |
| Pseudotabulky | Ano | Castecne |
| Nativni tabulky | Ano | Ano |
| Multivalue bunky | Ano (split newline) | Ne |
| WIP/N/A handling | Ano (konfigurovatelne) | Ne |

---

## 7. Priklad kompletni sablony

```json
{
  "schema_version": "slide_metadata/v1",
  "name": "Cost Optimization Report",
  "version": 1,
  "source_layout": "Title and Content",

  "slides": [
    {
      "slide_index": 0,
      "role": "data_slide",
      "title_detection": {
        "shape_type": "PLACEHOLDER",
        "shape_name_pattern": "Title *"
      },
      "tables": [
        {
          "table_id": "cost_optimization_main",
          "type": "pseudo_table",
          "columns": [
            {
              "id": "cost_category",
              "header_text_pattern": "Cost Category",
              "data_type": "text",
              "region": {"left_min_emu": 771788, "left_max_emu": 2750000}
            },
            {
              "id": "description",
              "header_text_pattern": "Main saving initiatives*",
              "data_type": "text",
              "region": {"left_min_emu": 2900000, "left_max_emu": 8500000}
            },
            {
              "id": "net_saving",
              "header_text_pattern": "Net saving*",
              "data_type": "number",
              "allow_wip": true,
              "region": {"left_min_emu": 9019000, "left_max_emu": 10400000}
            }
          ],
          "row_detection": {
            "method": "horizontal_lines",
            "separator_shape_type": "LINE",
            "separator_name_pattern": "Straight Connector *",
            "data_region": {"top_emu": 1452442, "bottom_emu": 5903000},
            "multivalue_split": "\n"
          },
          "header_detection": {
            "shape_names": ["ee4pHeader4", "object 22"],
            "region_top_emu": 1000000,
            "region_bottom_emu": 1452442
          },
          "output_sheet_name": "Cost optimization"
        }
      ],
      "ignore_shapes": {
        "patterns": [
          {"type": "EMBEDDED_OLE_OBJECT"},
          {"type": "PICTURE"},
          {"name_pattern": "Oval *"}
        ]
      }
    }
  ],

  "parsing_rules": {
    "number_parsing": {
      "decimal_separator": "auto",
      "strip_patterns": ["M\u20ac", "\u20ac", " "],
      "wip_markers": ["WIP", "N/A", "TBD", "-"]
    },
    "text_cleanup": {
      "strip_whitespace": true,
      "collapse_newlines_in_cell": false
    }
  }
}
```

---

## 8. Troubleshooting

| Problem | Pricina | Reseni |
|---------|---------|--------|
| Zadne tabulky extrahovany | Spatne EMU pozice | Pouzijte debug endpoint pro ziskani presnych pozic |
| Prazdne bunky | `shape_name_pattern` neodpovida | Zkontrolujte nazvy tvaru v Selection Pane |
| Cisla se neparsoval | Chybi `strip_patterns` | Pridejte jednotky do `parsing_rules.number_parsing.strip_patterns` |
| WIP hodnoty zpusobuji chyby | `allow_wip` neni nastaveno | Pridejte `"allow_wip": true` ke sloupci |
| Sablona se nenapoji na PPTX | Neni prirazeni | Vytvorte assignment s `match_criteria` |
| Nizka confidence (< 0.85) | Layout/titulek neodpovida | Upravte `source_layout` a `title_detection` |

---

## 9. API endpointy — shrnuti

| Endpoint | Metoda | Sluzba | Popis |
|----------|--------|--------|-------|
| `/api/query/templates/slide-metadata` | GET | engine-data | Seznam sablon |
| `/api/query/templates/slide-metadata` | POST | engine-data | Vytvoreni sablony |
| `/api/query/templates/slide-metadata/validate` | POST | engine-data | Validace JSON |
| `/api/query/templates/slide-metadata/match` | GET | engine-data | Auto-matching |
| `/api/v1/templates/slide-metadata/{id}` | GET | engine-data | Detail sablony |
| `/api/v1/templates/slide-metadata/{id}` | PUT | engine-data | Aktualizace (nova verze) |
| `/api/v1/templates/slide-metadata/{id}` | DELETE | engine-data | Smazani (soft delete) |
| `/api/v1/templates/slide-metadata/{id}/versions` | GET | engine-data | Historie verzi |
| `/api/v1/templates/slide-metadata/{id}/preview` | POST | engine-data | Dry-run extrakce |
| `/api/v1/templates/slide-metadata/{id}/assign` | POST | engine-data | Prirazeni sablony |

---

## 10. Souvisejici dokumentace

- [Reports & Templates Guide](Reports_Templates_Guide.md) — sprava reportu a PPTX sablon
- [Dashboards](Dashboards.md) — Custom SQL widgety nad extrahovanymi daty
- [Data Import API Guide](Data_Import_API_Guide.md) — nahravani souboru pres API
- [Data Streams Guide](Data_Streams_Guide.md) — architektura datovych streamu
