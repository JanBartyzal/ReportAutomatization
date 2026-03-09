# Rozhodnutí k otevřeným otázkám & aktualizace FS17–FS20
**Navazuje na:** feature_sets_FS17-FS20.md  
**Verze:** 1.1  
**Datum:** Únor 2026

---

## Decision Log

| # | Otázka | Rozhodnutí | Dopad na FS |
|---|---|---|---|
| D1 | Formulář vs. Excel | Formulář je **doplněk** Excelu. Formulář umožňuje export Excel šablony pro offline vyplnění a import zpět. | FS19 – rozšíření |
| D2 | Granularita formuláře | Formuláře centrální (holding reporting) i **lokální** (na úrovni jednotky). Lokální data lze "uvolnit" pro centralizované použití. | FS19 + nový **FS21** (lokální scope), vlastní fáze |
| D3 | PPTX šablona ownership | Stejný princip jako D2: centrální + lokální šablony. Lokální PPTX = vlastní **FS21**, vlastní fáze. | FS18 (pouze centrální) + FS21 |
| D4 | Workflow customizace | N8N orchestruje různé workflow. Microservices (MS-LIFECYCLE) připravují jen dílčí kroky – neobsahují hardcoded sekvenci. | FS17 – zjednodušení MS-LIFECYCLE |
| D5 | Srovnání period | Srovnání as-is (stejná metrika, různé periody). Granularita srovnání = samostatný **FS22**, implementace later. | FS20 – zúžení + nový FS22 |

---

## Aktualizace FS19 – Dynamic Form Builder & Data Collection

### D1: Excel jako rovnocenný vstup – Export & Import šablony

Formulář **nenahrazuje** Excel, ale nabízí oboustranný most:

#### Export Excel šablony z formuláře
- Každý publikovaný formulář lze exportovat jako strukturovaný Excel soubor (`GET /forms/{form_id}/export/excel-template`).
- Vygenerovaný Excel má list per sekce formuláře. Sloupce odpovídají polím formuláře včetně jejich validačních pravidel (dropdown seznam = Excel data validation, povinná pole = červené záhlaví).
- Soubor obsahuje skrytý metadata list (`__form_meta`) s `form_id` a `form_version_id` – slouží pro párování při importu zpět.
- Uživatel Excel stáhne, vyplní offline (v MS Excel nebo Google Sheets) a nahraje zpět.

#### Import vyplněného Excelu zpět do formuláře
- `POST /forms/{form_id}/import/excel` – systém zkontroluje metadata list, ověří `form_version_id`.
- Pokud verze souhlasí: data jsou přímo namapována bez nutnosti potvrzovat mapování (struktura je garantovaná šablonou).
- Pokud verze nesouhlasí (uživatel použil starý template): systém upozorní a nabídne best-effort mapování přes FS15.
- Po importu jsou data zobrazena v UI formuláře k vizuální kontrole před odesláním – Editor vidí vyplněný formulář, nikoli tabulku.
- Původní Excel soubor je uložen jako příloha reportu (audit).

#### Import libovolného Excelu (bez šablony)
- Zachován původní flow z FS19 – Editor nahraje vlastní Excel, systém nabídne column mapping přes FS15 Schema Mapping.
- Tímto způsobem lze migrovat historická data nebo zpracovat soubory z externích systémů.

### D2: Granularita – lokální formuláře jako rozšíření

Formuláře budou mít příznak `scope`:

| Scope | Vlastník | Viditelnost | Použití |
|---|---|---|---|
| `CENTRAL` | HoldingAdmin | Všechny přiřazené společnosti | Centralizovaný holding reporting |
| `LOCAL` | Editor / CompanyAdmin | Jen vlastní jednotka | Interní reporty společnosti |

- **`LOCAL` formuláře** jsou implementovány jako součást **FS21** (vlastní fáze).
- V FS19 se nyní implementují pouze `CENTRAL` formuláře.
- Datový model MS-FORM musí `scope` a `owner_org_id` zohledňovat od začátku, aby FS21 nevyžadovalo migraci schématu.
- "Uvolnění" lokálních dat pro centralizované použití = změna příznaku `scope: LOCAL → RELEASED` + notifikace HoldingAdminu (implementace v FS21).

---

## Aktualizace FS17 – OPEX Report Lifecycle (D4: N8N workflow customizace)

### Přepracování architektury MS-LIFECYCLE

Původní návrh předpokládal, že MS-LIFECYCLE řídí sekvenci kroků. Po rozhodnutí D4 je rozdělení odpovědností jasné:

**MS-LIFECYCLE dělá:**
- Spravuje stavový automat entity `Report` (DRAFT / SUBMITTED / UNDER_REVIEW / APPROVED / REJECTED).
- Vystavuje endpointy pro přechody stavů: `POST /reports/{id}/submit`, `POST /reports/{id}/approve`, `POST /reports/{id}/reject`.
- Validuje, zda je přechod povolen (RBAC + business rules – nelze schválit bez UNDER_REVIEW).
- Loguje každý přechod do MS-AUDIT.
- Publikuje event `report.status_changed` do Dapr PubSub po každém přechodu.

**N8N dělá:**
- Odebírá event `report.status_changed` a orchestruje co se má stát dál.
- Pro `SUBMITTED`: N8N spustí validační kroky, notifikaci Reviewerovi, případné automatické kontroly dat.
- Pro `APPROVED`: N8N triggeruje generování PPTX (MS-GEN-PPTX), aktualizaci centrálního dashboardu, archivaci.
- Pro `REJECTED`: N8N odesílá notifikaci Editorovi s komentářem.
- Různé `report_type` nebo `period_type` mohou mít různý N8N workflow (různá délka review procesu, různé validace) – N8N router rozhodne dle metadat eventu.

Tím MS-LIFECYCLE zůstává jako jednoduchý, spolehlivý state machine. Veškerá business orchestrace je v N8N a lze ji měnit bez re-deploymentu microservice.

```
Editor klikne "Submit"
        ↓
MS-LIFECYCLE: DRAFT → SUBMITTED (validace, zápis, audit)
        ↓
Dapr PubSub: event { report_id, from: DRAFT, to: SUBMITTED, report_type, org_id }
        ↓
N8N: odebere event → rozhodne dle report_type → spustí odpovídající workflow
  ├─ Měsíční report: notifikace Reviewerovi → čeká na manuální akci
  └─ Roční report: notifikace + spustí automatické kontroly dat → teprve pak notifikace
```

---

## Aktualizace FS18 – PPTX Generator (D3: pouze centrální šablony)

### Zúžení scope FS18

FS18 implementuje **pouze centrální PPTX šablony** (scope `CENTRAL`, owner = HoldingAdmin). Tato rozhodnutí zpřehledňují implementaci:

- MS-TMPL-PPTX spravuje pouze `CENTRAL` šablony.
- Datový model obsahuje `scope` a `owner_org_id` od začátku (příprava na FS21 bez migrace).
- Lokální PPTX šablony (pro interní reporty jednotek) jsou součástí **FS21**.
- Generátor (MS-GEN-PPTX) je architektonicky agnostický vůči `scope` – generuje dle dodané `template_id` bez ohledu na ownership. Tím FS21 jen přidá nové šablony, generátor se nemění.

---

## Aktualizace FS20 – Reporting Period Management (D5: srovnání as-is)

### Zúžení scope FS20 – srovnání period

FS20 implementuje **základní srovnání as-is**:

- Dashboard periody zobrazuje vybranou metriku pro aktuální periodu a libovolnou předchozí periodu vedle sebe.
- Srovnání na úrovni: stejná metrika, stejná organizace, dvě různé periody.
- Vizualizace: sloupcový nebo spojnicový graf, tabulka s delta hodnotami (absolutní i procentuální změna).
- Výběr period: dropdown se seznamem uzavřených period stejného typu (MONTHLY vs. MONTHLY, QUARTERLY vs. QUARTERLY).

**Co FS20 neřeší** (odkládáme do FS22):
- Srovnání napříč různými typy period (Q1 vs. celý rok).
- Konfigurovatelné srovnávací metriky (uživatel si definuje vlastní KPI pro srovnání).
- Srovnání na úrovni portfolia (více metrik najednou, více organizací najednou).
- Drill-down do subdimenzí (cost center, divize).

---

## Nový FS21 – Local Forms & Local PPTX Templates (Lokální scope)
**Priorita: STŘEDNÍ**  
**Fáze:** P5 (vlastní fáze po stabilizaci centrálního reportingu)

### Business kontext

Po nasazení centrálního reportingového cyklu vznikne přirozená poptávka ze strany dceřiných společností: "Chceme platformu využít i pro naše interní reporty a dashboardy, ne jen pro holding." FS21 tento požadavek řeší a zároveň vytváří zárodek pro budoucí horizontální expanzi platformy na standalone produkt pro jednotlivé společnosti.

### Požadavky

#### Lokální formuláře
- CompanyAdmin (nová sub-role Editora s rozšířenými právy v rámci vlastní organizace) může vytvářet formuláře se `scope: LOCAL`.
- Lokální formuláře jsou viditelné a vyplnitelné pouze uživateli v rámci dané `org_id`.
- Data z lokálních formulářů jsou primárně pro interní use – neproudí automaticky do centrálního reportingu.
- **"Uvolnění" dat:** CompanyAdmin může označit konkrétní lokální formulář nebo jeho data jako `RELEASED`. HoldingAdmin obdrží notifikaci a může data zahrnout do centrálního reportingu (manuální pull, nikoli automatický push).
- Lokální formuláře sledují stejný lifecycle jako centrální (DRAFT / PUBLISHED / CLOSED), ale bez holdingového approval workflow.

#### Lokální PPTX šablony
- CompanyAdmin může nahrát vlastní PPTX šablonu (scope: `LOCAL`).
- Generátor (MS-GEN-PPTX) je schopný generovat PPTX z lokální šablony pro potřeby interního reportu.
- Vygenerovaný lokální report není automaticky sdílen s holdingem.

#### Sdílení lokálních šablon
- CompanyAdmin může sdílet lokální šablonu nebo formulář s jiným CompanyAdminem v rámci stejného holdingu (`scope: SHARED_WITHIN_HOLDING`).
- HoldingAdmin má přehled o všech lokálních a sdílených šablonách/formulářích v holdingu.

### Nová microservice / rozšíření

| Dopad | Popis |
|---|---|
| MS-FORM (rozšíření) | Podpora `scope: LOCAL` a `scope: RELEASED`. Nová role CompanyAdmin. |
| MS-TMPL-PPTX (rozšíření) | Podpora lokálních šablon. |
| MS-ADMIN (rozšíření) | Správa role CompanyAdmin, přehled lokálních šablon/formulářů pro HoldingAdmin. |
| MS-LIFECYCLE (rozšíření) | Lokální lifecycle bez holdingového approval (zjednodušený stavový automat). |

*Žádná nová standalone microservice – FS21 rozšiřuje stávající.*

---

## Nový FS22 – Advanced Period Comparison (Granularita srovnání)
**Priorita: NÍZKÁ**  
**Fáze:** P6 (po stabilizaci FS20)  
**Status: PLACEHOLDER – implementace later**

### Scope (zatím jen definice, ne implementace)

- Konfigurovatelné KPI pro srovnání: uživatel si definuje vlastní srovnávací metriky a kombinace dimenzí.
- Srovnání across types: Q1 vs. celý rok (normalizace na denní/měsíční bázi).
- Multi-org srovnání: holding vidí stejnou metriku pro všechny dceřiné společnosti vedle sebe.
- Drill-down: srovnání na úrovni cost center nebo divize (vyžaduje granularitu dat z FS19 lokálních formulářů).
- Export srovnávacích reportů jako PPTX (napojení na FS18 generátor).

*Tento FS bude detailně specifikován až po nasazení FS20 a prvních zkušenostech z provozu.*

---

## Aktualizovaný přehled všech FS a fází

### Feature Sets – kompletní seznam

| FS | Název | Fáze | Status |
|---|---|---|---|
| FS01 | Infrastructure & Core | P1 | Beze změny |
| FS02 | File Ingestor | P1 | Rozšíření: `upload_purpose` flag |
| FS03 | Atomizers | P1–P2 | Rozšíření: MS-GEN-PPTX jako "obrácený atomizer" |
| FS04 | N8N Orchestrator | P1 | Rozšíření: workflow pro lifecycle eventy (D4) |
| FS05 | Sinks & Persistence | P1 | Rozšíření: `form_responses` tabulka |
| FS06 | Analytics & Query | P2 | Rozšíření: `source_type` flag (FORM / FILE) |
| FS07 | Admin Backend | P3 | Rozšíření: správa formulářů, šablon, role Reviewer |
| FS08 | Batch Management | P3 | `batch_id` → sjednocení s `period_id` z FS20 |
| FS09 | Frontend SPA | P1–P4 | Rozšíření: Form Builder UI, lifecycle UI, period dashboard |
| FS10 | Excel Parsing | P2 | Beze změny |
| FS11 | Dashboards & SQL | P4 | Beze změny |
| FS12 | API & AI (MCP) | P3 | Beze změny |
| FS13 | Notifications | P3 | Rozšíření: nové triggery z FS17 a FS20 |
| FS14 | Versioning & Diff | P4 | Beze změny |
| FS15 | Schema Mapping | P3 | Rozšíření: `POST /map/excel-to-form` |
| FS16 | Audit & Compliance | P4 | Rozšíření: auditování lifecycle přechodů a formulářových akcí |
| FS99 | DevOps & Observability | P5 | Beze změny |
| **FS17** | **Report Lifecycle** | **P3b** | **Nový – zjednodušen (D4)** |
| **FS18** | **PPTX Generator** | **P4b** | **Nový – pouze centrální šablony (D3)** |
| **FS19** | **Form Builder** | **P3c** | **Nový – Excel export/import (D1), pouze CENTRAL scope (D2)** |
| **FS20** | **Reporting Period Mgmt** | **P3b** | **Nový – základní srovnání as-is (D5)** |
| **FS21** | **Local Forms & Templates** | **P5** | **Nový – vlastní fáze (D2, D3)** |
| **FS22** | **Advanced Period Comparison** | **P6** | **Placeholder – later (D5)** |

### Fáze – aktualizovaný rollout

| Fáze | Obsah | Výstup |
|---|---|---|
| P1 | MVP Core (GW, Auth, Ingestor, PPTX Atomizer, N8N základní, Sinks, FE základní) | Fungující upload + extrakce + viewer |
| P2 | Extended Parsing (XLS, PDF, CSV, Query, Dash) | Plná podpora formátů + BI |
| P3 | Intelligence & Admin (Admin UI, Batch, AI, MCP, Schema Mapping) | Holdingová hierarchie + AI |
| **P3b** | **Lifecycle + Period Mgmt (MS-LIFECYCLE, MS-PERIOD)** | **Řízení OPEX cyklu, deadliny, stavový automat** |
| **P3c** | **Form Builder – centrální (MS-FORM, Excel export/import)** | **Sběr dat bez Excelu po e-mailu** |
| P4 | Enterprise Features (Notif, Versioning, Audit, Search) | Compliance + versioning |
| **P4b** | **PPTX Generator (MS-TMPL-PPTX, MS-GEN-PPTX)** | **Automatické generování standardizovaných reportů** |
| **P4c** | **Advanced Period Mgmt (deadliny, eskalace, as-is srovnání)** | **Plná správa reportingových cyklů** |
| P5 | DevOps Maturity + **FS21 Local Forms & Templates** | Production-ready + lokální scope |
| P6 | **FS22 Advanced Comparison** (placeholder) | Granulární srovnání period |

---

*Decision Log v1.1 | PPTX Analyzer & Automation Platform | Únor 2026*
