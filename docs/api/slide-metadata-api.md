# Slide Metadata API — engine-data:template

## Kontext

Slide metadata šablony definují **strukturu pseudotabulek** na PPTX slidech.
PPTX Atomizer (processor-atomizers:pptx) je používá při extrakci dat —
říkají mu KDE na slidu hledat hlavičky, sloupce a řádky (EMU pozice, shape
name patterny, separator typy). Neobsahují konkrétní data.

Šablony vytváří admin v UI (Form Builder pro metadata) nebo se seedují při
init (infra/init/setup.py → step_seed_slide_metadata).

## Služba

**engine-data:template** (Java 21 + Spring Boot)
- Deployment unit: `engine-data`
- Modul: `template`
- Port: 8100

## REST API Endpointy

Prefix: `/api/v1/templates/slide-metadata`

### CRUD

| Method | Path | Popis | Auth |
|--------|------|-------|------|
| `POST`   | `/`                    | Vytvoří novou metadata šablonu | Admin |
| `GET`    | `/`                    | Seznam všech šablon (stránkovaný) | Admin, Editor |
| `GET`    | `/{id}`                | Detail šablony | Admin, Editor |
| `PUT`    | `/{id}`                | Aktualizuje šablonu (vytvoří novou verzi) | Admin |
| `DELETE` | `/{id}`                | Smaže šablonu (soft delete) | Admin |
| `GET`    | `/by-name/{name}`      | Vyhledání šablony podle jména | Admin, Editor |
| `PUT`    | `/by-name/{name}`      | Aktualizace podle jména (pro seed) | Admin |

### Verzování

| Method | Path | Popis | Auth |
|--------|------|-------|------|
| `GET`    | `/{id}/versions`       | Seznam verzí šablony | Admin, Editor |
| `GET`    | `/{id}/versions/{ver}` | Konkrétní verze | Admin, Editor |
| `POST`  | `/{id}/revert/{ver}`   | Návrat k dřívější verzi | Admin |

### Validace & Preview

| Method | Path | Popis | Auth |
|--------|------|-------|------|
| `POST`  | `/validate`            | Validace JSON struktury bez uložení | Admin |
| `POST`  | `/{id}/preview`        | Dry-run: aplikuje šablonu na PPTX a vrátí extrahovaná data bez uložení | Admin |

### Přiřazení

| Method | Path | Popis | Auth |
|--------|------|-------|------|
| `POST`  | `/{id}/assign`         | Přiřadí šablonu ke konkrétnímu souboru/typu/organizaci | Admin |
| `GET`   | `/{id}/assignments`    | Seznam přiřazení | Admin |
| `DELETE` | `/{id}/assignments/{aid}` | Zruší přiřazení | Admin |
| `GET`   | `/match`               | Auto-match: systém navrhne šablonu pro daný PPTX (na základě layoutu, titulu) | Orchestrátor (gRPC) |

---

## Datový model

### SlideMetadataTemplate (PostgreSQL)

```sql
CREATE TABLE slide_metadata_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    schema_version  VARCHAR(50) NOT NULL DEFAULT 'slide_metadata/v1',
    version         INT NOT NULL DEFAULT 1,
    is_active       BOOLEAN NOT NULL DEFAULT true,
    definition      JSONB NOT NULL,       -- celý slide_metadata.json
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE (org_id, name, version)
);

-- RLS policy
ALTER TABLE slide_metadata_templates ENABLE ROW LEVEL SECURITY;
CREATE POLICY slide_metadata_org_isolation ON slide_metadata_templates
    USING (org_id = current_setting('app.current_org_id')::UUID);

-- Index pro vyhledávání
CREATE INDEX idx_slide_meta_name ON slide_metadata_templates (org_id, name, is_active);
CREATE INDEX idx_slide_meta_schema ON slide_metadata_templates USING GIN (definition jsonb_path_ops);
```

### SlideMetadataAssignment

```sql
CREATE TABLE slide_metadata_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id     UUID NOT NULL REFERENCES slide_metadata_templates(id),
    org_id          UUID NOT NULL,
    match_criteria  JSONB NOT NULL,
    -- match_criteria příklady:
    -- {"slide_layout": "Title and Content", "title_pattern": "*Cost*"}
    -- {"file_type": "pptx", "org_id": "specific-org-uuid"}
    priority        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## Request/Response příklady

### POST /api/v1/templates/slide-metadata

Request:
```json
{
    "name": "Cost Optimization Report",
    "schema_version": "slide_metadata/v1",
    "definition": { ... celý slide_metadata.json ... }
}
```

Response (201):
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Cost Optimization Report",
    "schema_version": "slide_metadata/v1",
    "version": 1,
    "created_at": "2026-03-23T10:00:00Z"
}
```

### GET /api/v1/templates/slide-metadata

Response (200):
```json
{
    "items": [
        {
            "id": "550e8400-...",
            "name": "Cost Optimization Report",
            "schema_version": "slide_metadata/v1",
            "version": 2,
            "slide_count": 1,
            "table_count": 2,
            "is_active": true,
            "updated_at": "2026-03-23T12:00:00Z"
        }
    ],
    "total": 1,
    "page": 1,
    "page_size": 20
}
```

### POST /api/v1/templates/slide-metadata/validate

Request:
```json
{
    "definition": { ... }
}
```

Response (200):
```json
{
    "valid": true,
    "warnings": [
        "Column 'net_saving' has allow_wip=true — consider adding WIP to parsing_rules.wip_markers"
    ],
    "slide_count": 1,
    "table_count": 2,
    "column_count": 7
}
```

Response (422) pokud nevalidní:
```json
{
    "valid": false,
    "errors": [
        "slides[0].tables[0].columns[2].region.left_min_emu must be < left_max_emu",
        "slides[0].tables[0].row_detection.method must be one of: horizontal_lines, vertical_position, shape_grouping"
    ]
}
```

### POST /api/v1/templates/slide-metadata/{id}/preview

Request:
```json
{
    "file_id": "pptx-file-uuid"
}
```

Response (200):
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
                        "headers": ["Cost Category", "Main saving initiatives...", "Net saving 2024 (M€)", "Additional..."],
                        "row_count": 14,
                        "rows": [ ... ],
                        "confidence": 0.92
                    }
                ]
            }
        ]
    },
    "warnings": ["Row 13 column 'net_saving' contains non-numeric value 'WIP'"]
}
```

### GET /api/v1/templates/slide-metadata/match

Query params: `?file_id=xxx` nebo `?layout=Title+and+Content&title=Cost*`

Response (200):
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

## gRPC (interní, volaný z engine-orchestrator)

```protobuf
service SlideMetadataService {
    // Orchestrátor volá při zpracování PPTX — najde vhodnou šablonu
    rpc MatchTemplate(MatchTemplateRequest) returns (MatchTemplateResponse);

    // Orchestrátor volá s nalezenou šablonou — vrátí extrakční instrukce pro Atomizer
    rpc GetExtractionPlan(GetExtractionPlanRequest) returns (ExtractionPlanResponse);
}

message MatchTemplateRequest {
    string file_id = 1;
    string slide_layout = 2;
    string slide_title = 3;
    int32 shape_count = 4;
}

message MatchTemplateResponse {
    string template_id = 1;
    string template_name = 2;
    float confidence = 3;
}

message GetExtractionPlanRequest {
    string template_id = 1;
}

message ExtractionPlanResponse {
    string template_id = 1;
    bytes definition_json = 2;  // celý slide_metadata JSON
}
```

---

## Workflow integrace (engine-orchestrator)

```
1. File uploaded (PPTX)
2. engine-orchestrator → engine-data:template (gRPC MatchTemplate)
     → vrátí template_id + confidence
3. if confidence >= 0.85:
     engine-orchestrator → processor-atomizers:pptx (gRPC ExtractWithMetadata)
       → atomizer použije metadata pro přesnou rekonstrukci tabulky
   else:
     engine-orchestrator → processor-atomizers:pptx (gRPC ExtractStructure)
       → atomizer použije generický MetaTable algoritmus (FS03)
4. Výsledek → engine-data:sink-tbl (gRPC BulkInsert)
```
