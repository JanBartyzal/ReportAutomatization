# Reports & Templates — Uzivatelsky pruvodce

Dokumentace pro tvorbu reportu, spravu sablon a generovani vystupu v RA platforme.

---

## Prehled workflow

```
1. Nahrat sablonu        /templates        (jednorazove)
2. Namapovat placeholdery /templates/:id    (jednorazove)
3. Vytvorit report       /reports           (per perioda)
4. Schvalit report       /reports/:id       (lifecycle)
5. Generovat PPTX        /reports/:id       (po schvaleni)
6. Stahnout vysledek     /reports/:id       (download)
```

---

## 1. Sprava sablon (PPTX Templates)

### 1.1 Nahrani sablony

1. Prejdi na **Templates** (`/templates`)
2. Klikni **Upload Template**
3. Vyber PPTX soubor a zadej nazev
4. System automaticky extrahuje placeholdery

### 1.2 Placeholdery v sablone

PPTX sablona obsahuje specialni znacky, ktere se pri generovani nahradi daty:

| Syntax | Typ | Priklad |
|--------|-----|---------|
| `{{nazev}}` | Text | `{{company_name}}`, `{{period}}`, `{{total_opex}}` |
| `{{TABLE:nazev}}` | Tabulka | `{{TABLE:opex_by_category}}` — vlozi tabulku s daty |
| `{{CHART:nazev}}` | Graf | `{{CHART:cost_trend}}` — vlozi graf |

**Priklad PPTX slidu:**
```
Firma: {{company_name}}
Obdobi: {{period}}
Celkovy OPEX: {{total_opex}} EUR

{{TABLE:opex_breakdown}}

{{CHART:opex_trend_2024_2026}}
```

### 1.3 Mapovani placeholderu na data

1. Prejdi na **Templates** → klikni na sablonu
2. Otevri tab **Placeholder Mapping**
3. Pro kazdy placeholder vyber zdroj dat:

| Zdroj | Popis | Priklad |
|-------|-------|---------|
| `form_field` | Pole z formulare | `company_name` → pole "Nazev firmy" |
| `table` | Data z parsed_tables (sink) | `opex_breakdown` → tabulka z batche |
| `aggregated` | Agregovana hodnota | `total_opex` → SUM sloupce Amount |
| `time_series` | Casova rada | `cost_trend` → data pro line chart |

4. Klikni **Save Mapping**

### 1.4 Verzovani sablon

- Kazdy upload nove verze PPTX vytvori novou verzi sablony
- Stare verze zustavaji v historii
- Mapovani placeholderu se prenasi na novou verzi

---

## 2. Tvorba reportu

### 2.1 Vytvoreni reportu

1. Prejdi na **Reports** (`/reports`)
2. Klikni **New Report**
3. Vyber:
   - **Organization** — pro kterou firmu
   - **Reporting Period** — obdobi (Q1 2026, Full Year 2025, ...)
   - **Report Type** — typ reportu (OPEX, CAPEX, Financial, General)
4. Klikni **Create Report**

Report se vytvori ve stavu **DRAFT**.

### 2.2 Zivotni cyklus reportu

```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → COMPLETED → RELEASED
                                  ↘ REJECTED → DRAFT (resubmit)
```

| Stav | Kdo | Akce |
|------|-----|------|
| DRAFT | Editor | Editace dat, priprava k odeslani |
| SUBMITTED | Editor | Klikne "Submit Report" |
| UNDER_REVIEW | Admin | Kontroluje data |
| APPROVED | Admin | Klikne "Approve" — report je schvalen |
| REJECTED | Admin | Klikne "Reject" + komentar — vrati se do DRAFT |
| COMPLETED | Editor | Klikne "Complete" |
| RELEASED | Admin | Klikne "Release" — finalni stav |

### 2.3 Hromadne operace

Na strance **Reports** lze vybrat vice reportu a provest:
- **Bulk Approve** — hromadne schvaleni
- **Bulk Reject** — hromadne zamitnuti s komentarem

---

## 3. Generovani PPTX

### 3.1 Generovani z detailu reportu

1. Otevri schvaleny report (stav **APPROVED**)
2. Klikni **Generate PPTX**
3. System asynchronne generuje PPTX:
   - Nacte sablonu s mapovanymi placeholdery
   - Nacte data z batche/sinku pro danou organizaci a periodu
   - Nahradi placeholdery skutecnymi daty
4. Po dokonceni se zobrazi tlacitko **Download PPTX**

### 3.2 Batch generovani

1. Prejdi na **Batch Generation** (`/batch-generation`)
2. Vyber periodu
3. System zobrazi vsechny schvalene reporty pro danou periodu
4. Oznac reporty a klikni **Generate Selected**
5. Sleduj prubeh generovani
6. Po dokonceni stahni jednotlive PPTX soubory

### 3.3 Jak funguje generovani (technicke detaily)

```
Frontend                     engine-reporting              processor-generators
   │                              │                              │
   │ POST /reports/{id}/generate  │                              │
   │─────────────────────────────>│                              │
   │   { reportId, templateId }   │                              │
   │                              │ Dapr gRPC: generate(data)    │
   │                              │─────────────────────────────>│
   │                              │                              │ Nacte PPTX sablonu
   │                              │                              │ Nahradi placeholdery
   │                              │                              │ Ulozi vysledek
   │   { jobId, status: QUEUED }  │                              │
   │<─────────────────────────────│                              │
   │                              │                              │
   │ GET /reports/{id}/generation-status?jobId=...               │
   │─────────────────────────────>│                              │
   │   { status: COMPLETED }      │                              │
   │<─────────────────────────────│                              │
   │                              │                              │
   │ GET /reports/{id}/download   │                              │
   │─────────────────────────────>│ Blob download                │
   │   <PPTX binary>              │                              │
   │<─────────────────────────────│                              │
```

### 3.4 Seznam vygenerovanych reportu

Prejdi na **Generated Reports** (`/generated-reports`) pro prehled vsech vygenerovanych souboru s moznosti:
- **Download** — stahnout PPTX
- **Regenerate** — pregenerovat s aktualni daty

---

## 4. Univerzalni sablony — koncept

Sablona je **univerzalni** — obsahuje placeholdery, ktere se naplni daty podle kontextu:

| Co se meni | Odkud |
|-----------|-------|
| Data (cisla, tabulky) | `parsed_tables` (sink) filtrovane podle org_id + period_id |
| Nazev firmy | `organizations` tabulka |
| Obdobi | `periods` tabulka |
| Grafy | Agregovana data z dashboardu |

**Priklad:** Jedna sablona "OPEX Quarterly Report" se pouzije pro vsechny firmy v holdingu. Kazda firma dostane svuj PPTX s vlastnimi cisly.

### 4.1 Jak propojit sablonu s batch daty

1. Nahraj Excel soubor do batche → data se ulozi do `data.parsed_tables`
2. V sablone mapuj placeholder `{{TABLE:opex_data}}` na tabulku z batche
3. Pri generovani system najde data podle:
   - `org_id` — filtr na organizaci reportu
   - `period_id` nebo `created_at` — filtr na obdobi
   - `source_sheet` — nazev sheetu v Excelu

### 4.2 Custom SQL widget jako alternativa

Misto mapovani placeholderu lze pouzit **Custom SQL** widget v dashboardu a ten exportovat:

```sql
-- Data z posledniho uploadu Test.xlsx pro danou organizaci
WITH latest AS (
  SELECT pt.rows FROM data.parsed_tables pt
  JOIN ingestor.files f ON f.id::text = pt.file_id
  WHERE f.filename = 'Test.xlsx'
  ORDER BY f.created_at DESC LIMIT 1
)
SELECT r.value->>1 AS "Project", CAST(r.value->>9 AS NUMERIC) AS "Cost"
FROM latest l, jsonb_array_elements(l.rows) AS r(value)
```

Vice vzoru viz [Dashboards.md](Dashboards.md).

---

## 5. Planovane funkce

| Funkce | Stav | Poznamka |
|--------|------|---------|
| Create Report z /reports | Implementovano | Dialog s vyberem org/period/type |
| Generate PPTX z detailu reportu | Implementovano | Async s pollovanim statusu |
| Batch Generate PPTX | Implementovano | /batch-generation stranka |
| Download PPTX | Implementovano | Z detailu reportu i generated-reports |
| Export Excel | Planovano | Renderer existuje, chybi endpoint + UI |
| Export PDF | Planovano | LibreOffice headless konverze |
| Template versioning | Implementovano | Kazdy upload = nova verze |
| Placeholder auto-detection | Implementovano | Extrakce z PPTX pri uploadu |

---

## 6. Relevantni URL

| Stranka | URL | Ucel |
|---------|-----|------|
| Reports | `/reports` | Seznam reportu, vytvoreni noveho |
| Report Detail | `/reports/:id` | Detail, lifecycle akce, generovani |
| Templates | `/templates` | Sprava PPTX sablon |
| Template Detail | `/templates/:id` | Mapovani placeholderu |
| Batch Generation | `/batch-generation` | Hromadne generovani |
| Generated Reports | `/generated-reports` | Seznam vygenerovanych PPTX |

## 7. API endpointy

| Endpoint | Metoda | Sluzba | Popis |
|----------|--------|--------|-------|
| `/api/reports` | GET | engine-reporting | Seznam reportu |
| `/api/reports` | POST | engine-reporting | Vytvoreni reportu |
| `/api/reports/{id}` | GET | engine-reporting | Detail reportu |
| `/api/reports/{id}/submit` | POST | engine-reporting | Odeslani ke schvaleni |
| `/api/reports/{id}/approve` | POST | engine-reporting | Schvaleni |
| `/api/reports/{id}/reject` | POST | engine-reporting | Zamitnuti |
| `/api/reports/{id}/generate` | POST | engine-reporting | Spustit generovani PPTX |
| `/api/reports/{id}/download` | GET | engine-reporting | Stahnout vygenerovany PPTX |
| `/api/reports/batch-generate` | POST | engine-reporting | Hromadne generovani |
| `/api/templates/pptx` | GET | engine-reporting | Seznam sablon |
| `/api/templates/pptx` | POST | engine-reporting | Nahrat sablonu (multipart) |
| `/api/templates/pptx/{id}/placeholders` | GET | engine-reporting | Seznam placeholderu |
| `/api/templates/pptx/{id}/mapping` | POST | engine-reporting | Ulozit mapovani |
| `/api/dashboards/sql/execute` | POST | engine-data | Custom SQL dotaz |
