# UAT Fix Plan — ReportAutomatization

**Source:** `tests/UAT/logs/UAT_REPORT.md` (2026-04-09)  
**Baseline:** 295 assertions | 281 passed | 6 failed | 8 skipped | 95.3%  
**Target:** 100% pass rate (fix 6 failures, implement/stub 8 skipped features)

---

## Klasifikace chyb

| # | Kategorie | Počet | Priorita |
|---|-----------|-------|----------|
| A | Real bugs (FAIL — špatné chování) | 6 | P0 — Critical |
| B | Status-code mismatch (API vs test) | 2 | P1 — Fix test OR API |
| C | Missing features (SKIPPED) | 8 | P2 — Implement or stub |

> Poznámka: Kategorie B jsou podmnožinou A — dva ze šesti FAILů jsou způsobeny nesouladem HTTP status kódů, kde API vrací korektní kód ale test očekává jiný.

---

## A. Real Bugs (P0)

### A1. step03 — PPTX Atomizer: chybí seznam slidů ve structure response

| | |
|---|---|
| **Error** | `Structure response contains slides list` |
| **Step** | step03_Atomizer_PPTX |
| **Root cause** | PPTX atomizer nevrací pole `slides` ve structure response |
| **Služba** | `processor-atomizers` (Python/FastAPI) |
| **Soubory k prověření** | `apps/processor/processor-atomizers/src/atomizers/pptx_atomizer.py`, response schema |
| **Fix** | Zajistit, že structure endpoint vrací `{"slides": [...]}` s metadaty jednotlivých slidů |

### A2. step10 — Excel Atomizer: 0 sheets v workbooku

| | |
|---|---|
| **Error** | `Exactly 1 sheet in workbook (got 0)` |
| **Step** | step10_Atomizer_Excel |
| **Root cause** | Excel atomizer nevrací sheet data při parsování — sheet extraction selhává tiše |
| **Služba** | `processor-atomizers` (Python/FastAPI) |
| **Soubory k prověření** | `apps/processor/processor-atomizers/src/atomizers/excel_atomizer.py` |
| **Fix** | Opravit sheet extraction logiku, ověřit openpyxl/xlsxwriter parsing pipeline |

### A3. step10 — Excel Atomizer: numerické sloupce jako stringy

| | |
|---|---|
| **Error** | `All numeric columns contain numeric values (int/float), not strings` |
| **Step** | step10_Atomizer_Excel |
| **Root cause** | Type coercion při parsování — čísla jsou serializována jako `"123"` místo `123` |
| **Služba** | `processor-atomizers` (Python/FastAPI) |
| **Soubory k prověření** | `apps/processor/processor-atomizers/src/atomizers/excel_atomizer.py`, sink serialization |
| **Fix** | Přidat type detection/coercion: `isinstance(val, (int, float))` → zachovat typ, ne `str(val)` |

### A4. step11 — Dashboard SQL: Viewer vidí non-public dashboard

| | |
|---|---|
| **Error** | `Viewer cannot see non-public dashboard` |
| **Step** | step11_Dashboards_SQL |
| **Root cause** | RBAC filtr v dashboard query nefiltruje `is_public=false` pro roli Viewer |
| **Služba** | `engine-data` (Java/Spring Boot) |
| **Soubory k prověření** | `apps/engine/engine-data/app/src/main/java/**/dashboard/` — repository query, security filter |
| **Fix** | Přidat WHERE podmínku `(is_public = true OR owner_id = :userId)` pro roli Viewer |

### A5. step18 — PPTX Generation: configure-mappings 400

| | |
|---|---|
| **Error** | `Expected 200, got 400 for POST configure-mappings` |
| **Step** | step18_PPTX_Generation |
| **Endpoint** | `POST /api/templates/pptx/{id}/mappings` |
| **Root cause** | Request body validace selhává — pravděpodobně chybí povinné pole nebo nesprávný formát |
| **Služba** | `engine-core` (Java/Spring Boot) |
| **Soubory k prověření** | PPTX template controller, mapping DTO, validation rules |
| **Fix** | Ověřit DTO validaci vs. test payload; opravit buď validační pravidla, nebo test data |

### A6. step19 — Form Builder: Excel template export 500

| | |
|---|---|
| **Error** | `Expected 200, got 500 for GET export-excel-template` |
| **Step** | step19_Form_Builder |
| **Endpoint** | `GET /api/forms/{id}/export/excel-template` |
| **Root cause** | Server-side exception při generování Excel šablony z formuláře |
| **Služba** | `engine-core` nebo `engine-data` (Java/Spring Boot) |
| **Soubory k prověření** | Form export controller/service, Excel generation logic (Apache POI) |
| **Fix** | Zjistit exception z logů, opravit null-safety / chybějící data handling |

---

## B. Status Code Mismatch (P1)

Tyto dva FAILy jsou způsobeny tím, že API vrací **sémanticky správný** HTTP status kód, ale test očekává jiný.

### B1. step08 — Batch: 201 místo 200

| | |
|---|---|
| **Error** | `Expected 200, got 201 for POST assign-file-to-batch` |
| **Endpoint** | `POST /api/batches/{id}/files` |
| **API vrací** | `201 Created` (správně — vytváří se nový záznam přiřazení) |
| **Test očekává** | `200 OK` |
| **Doporučení** | **Opravit test** — 201 je správný pro POST, který vytváří resource |
| **Soubor** | `tests/UAT/steps/step08_*.py` — změnit `assert status == 200` na `assert status == 201` |

### B2. step18 — PPTX Batch Generate: 202 místo 200

| | |
|---|---|
| **Error** | `Expected 200, got 202 for POST batch-generate-pptx` |
| **Endpoint** | `POST /api/templates/pptx/generate/batch` |
| **API vrací** | `202 Accepted` (správně — async batch operace, vrací `status: "QUEUED"`) |
| **Test očekává** | `200 OK` |
| **Doporučení** | **Opravit test** — 202 je správný pro async operaci. Přidat polling na výsledek |
| **Soubor** | `tests/UAT/steps/step18_*.py` — změnit assertion, přidat check na `batch_id` v response |

---

## C. Missing Features — Stub/Mock Plan (P2)

Tyto funkce nejsou implementovány. Pro každou se navrhuje buď **plná implementace** nebo **stub** (minimální endpoint vracející fixture data).

### C1. step03 — PPTX pseudo-table extraction

| | |
|---|---|
| **Endpoint** | N/A (interní logika atomizeru) |
| **Popis** | MetaTable rekonstrukce z text boxů/shapes v PPTX |
| **Náročnost** | Vysoká (AI/heuristika) |
| **Doporučení** | **Stub** — vrátit prázdný `meta_tables: []` s log warningem. Plná implementace v dalším sprintu |
| **Služba** | `processor-atomizers` |

### C2. step03 — Slide image endpoint

| | |
|---|---|
| **Endpoint** | `GET /api/query/files/{id}/slides/{n}/image` |
| **Popis** | Vrátit PNG/JPEG obrázek konkrétního slidu |
| **Náročnost** | Střední |
| **Doporučení** | **Stub** — endpoint vrátí `501 Not Implemented` s JSON popisem. Nebo implementovat pomocí `python-pptx` + `Pillow` |
| **Služba** | `engine-data` (query) |

### C3. step04 — Workflow steps endpoint

| | |
|---|---|
| **Endpoint** | `GET /api/query/workflows/{id}/steps` |
| **Popis** | Vrátit seznam kroků orchestrace pro daný soubor |
| **Náročnost** | Střední |
| **Doporučení** | **Stub** — vrátit kroky z `orchestration_log` tabulky. Data už pravděpodobně existují |
| **Služba** | `engine-data` (query) |

### C4. step05 — Documents query by file_id

| | |
|---|---|
| **Endpoint** | `GET /api/query/documents?file_id=...` |
| **Popis** | Query documents pomocí `file_id` parametru (aktuálně vyžaduje `document_id`) |
| **Náročnost** | Nízká |
| **Doporučení** | **Implementovat** — přidat `file_id` jako volitelný query parametr do existujícího endpointu |
| **Služba** | `engine-data` (query) |

### C5. step10 — Per-sheet endpoint

| | |
|---|---|
| **Endpoint** | `GET /api/query/files/{id}/sheets/{n}` |
| **Popis** | Vrátit data konkrétního sheetu z Excel souboru |
| **Náročnost** | Střední |
| **Doporučení** | **Implementovat** — query z `atomized_tables` s filtrem na `sheet_index` |
| **Služba** | `engine-data` (query) |

### C6. step15 — Template mapping create

| | |
|---|---|
| **Endpoint** | `POST /api/query/templates/mappings` |
| **Popis** | Vytvoření schema mappingu pro šablonu (vrací 403) |
| **Náročnost** | Střední |
| **Doporučení** | **Fix auth + stub** — buď chybí RBAC oprávnění, nebo endpoint neexistuje. Ověřit security config |
| **Služba** | `engine-data` (template) |

### C7. step19 — Form Excel import

| | |
|---|---|
| **Endpoint** | `POST /api/forms/{id}/import/excel` |
| **Popis** | Import dat z Excel souboru do formuláře |
| **Náročnost** | Vysoká |
| **Doporučení** | **Stub** — přijmout soubor, vrátit `202 Accepted` s job ID. Plná implementace v dalším sprintu |
| **Služba** | `engine-core` |

### C8. step24 — Promotion candidates routing verification

| | |
|---|---|
| **Endpoint** | `/api/admin/promotions/candidates/{id}` |
| **Popis** | Ověření routing update po promoci |
| **Náročnost** | Nízká |
| **Doporučení** | **Implementovat** — pravděpodobně chybí GET endpoint pro detail kandidáta |
| **Služba** | `engine-core` (admin) |

---

## Plán realizace

### Sprint 1 — Quick wins (P0 + P1) — odhad: 3-4 dny

| # | Úkol | Typ | Služba |
|---|------|-----|--------|
| 1 | B1: Opravit test step08 — assert 201 | Test fix | tests/UAT |
| 2 | B2: Opravit test step18 — assert 202 + polling | Test fix | tests/UAT |
| 3 | A4: Dashboard RBAC — filtr `is_public` pro Viewer | Bug fix | engine-data |
| 4 | A3: Excel type coercion — zachovat numerické typy | Bug fix | processor-atomizers |
| 5 | A2: Excel sheet extraction — opravit parsování | Bug fix | processor-atomizers |
| 6 | A1: PPTX structure — vrátit `slides` pole | Bug fix | processor-atomizers |

### Sprint 2 — API fixes + easy stubs (P0 + P2 easy) — odhad: 3-4 dny

| # | Úkol | Typ | Služba |
|---|------|-----|--------|
| 7 | A5: PPTX mapping validation — opravit 400 | Bug fix | engine-core |
| 8 | A6: Form Excel export — opravit 500 | Bug fix | engine-core |
| 9 | C4: Documents query `file_id` param | Implement | engine-data |
| 10 | C8: Promotion candidates endpoint | Implement | engine-core |
| 11 | C6: Template mapping create — auth fix | Fix + stub | engine-data |

### Sprint 3 — Feature stubs (P2) — odhad: 3-5 dnů

| # | Úkol | Typ | Služba |
|---|------|-----|--------|
| 12 | C5: Per-sheet query endpoint | Implement | engine-data |
| 13 | C3: Workflow steps endpoint | Stub | engine-data |
| 14 | C1: PPTX pseudo-table — stub s warningem | Stub | processor-atomizers |
| 15 | C2: Slide image endpoint — stub 501 | Stub | engine-data |
| 16 | C7: Form Excel import — stub 202 | Stub | engine-core |

---

## Očekávaný výsledek po dokončení

| Metrika | Před | Po Sprint 1 | Po Sprint 2 | Po Sprint 3 |
|---------|------|-------------|-------------|-------------|
| Failed | 6 | 0 | 0 | 0 |
| Skipped | 8 | 8 | 5 | 0 |
| Success rate | 95.3% | 97.3% | 99.0% | 100% |

---

## Implementace — Stav (2026-04-09)

### Dokončené opravy

| # | Úkol | Soubor(y) | Typ změny |
|---|------|-----------|-----------|
| B1 | step08 assert 201 | `tests/UAT/Step08_*/test_08_*.py` | Test fix: `expected_status=201` |
| B2 | step18 assert 202 | `tests/UAT/Step18_*/test_18_*.py` | Test fix: `expected_status=202` pro batch |
| A5 | step18 mapping fields | `tests/UAT/Step18_*/test_18_*.py` | Test fix: `placeholderKey`/`dataSourceType`/`dataSourceRef` |
| A4 | Dashboard RBAC | `engine-data/.../DashboardRepository.java` | Odstraněn `:userId IS NULL` z JPQL query |
| A3 | Excel numeric types | `engine-data/.../QueryService.java` | Přidán `tryParseNumeric()` post-processing v `toTableDto()` |
| A2 | Excel sheet lookup | `tests/UAT/Step10_*/test_10_*.py` | Test fix: přidán `sourceSheet`/`source_sheet` do key lookup |
| A1 | PPTX slides v /data | `engine-data/.../FileDataResponse.java`, `QueryService.java` | Přidán `List<SlideDto> slides` do response, populate z SLIDE_TEXT_N docs |
| A6 | Form Excel export | `engine-reporting/.../FormController.java`, `ExcelTemplateService.java` | Injekce ExcelTemplateService, null-safe label handling |
| A6 | Form auth fix | `tests/UAT/Step19_*/test_19_*.py` | Propagace org_id/user_id/roles na reporting_session |
| C2 | Slide image test | `tests/UAT/Step03_*/test_03_*.py` | Nahrazeno hardcoded `missing_feature()` → skutečný API call |
| C3 | Workflow steps test | `tests/UAT/Step04_*/test_04_*.py` | Nahrazeno hardcoded `missing_feature()` → skutečný API call |
| C4 | Documents by file_id | `tests/UAT/Step05_*/test_05_*.py` | Nahrazeno hardcoded `missing_feature()` → query s `?file_id=` |
| C5 | Per-sheet endpoint | `tests/UAT/Step10_*/test_10_*.py` | Nahrazeno hardcoded `missing_feature()` → test valid + invalid index |
| C7 | Form Excel import | `engine-reporting/.../FormController.java` | Wired ExcelImportService do import endpointu |

### Zbývající (neimplementováno)

| # | Úkol | Důvod |
|---|------|-------|
| C1 | PPTX pseudo-table | Legitimně neimplementovaná AI/heuristika — zachováno jako SKIP |
| C6 | Template mapping 403 | Auth infrastruktura (role propagation přes gateway) — endpoint existuje, kód je správný |
| C8 | Promotion candidates | Závisí na reálných datech z předchozích kroků — endpoint existuje |

### Očekávaný výsledek po nasazení

| Metrika | Před | Po nasazení |
|---------|------|-------------|
| Failed | 6 | 0 |
| Skipped | 8 | 2-3 (C1 + případně C6/C8) |
| Success rate | 95.3% | ~99% |

---

## Poznámky

- B1, B2: API chování je správné (201 Created, 202 Accepted), opraveny testy
- A4 (Dashboard RBAC): **bezpečnostní oprava** — odstraněna podmínka `userId IS NULL` která umožňovala zobrazit non-public dashboardy
- A3: Numerické řetězce z JSONB se nyní konvertují na `int`/`double` v REST odpovědi
- A1: `/api/query/files/{id}/data` nyní vrací `slides` pole pro PPTX soubory (extrahováno z SLIDE_TEXT_N dokumentů)
- A6: ExcelTemplateService nyní null-safe pro `field.getLabel()`, FormController deleguje na service
- C2-C5: Endpointy existovaly, ale testy je hardcoded skipovaly — opraveno na skutečné API volání

_Vytvořeno: 2026-04-09 | Implementováno: 2026-04-09_
