# Feature Sets FS17–FS20: Rozšíření scope – OPEX Reporting Lifecycle
**Navazuje na:** Project Charter v4.0  
**Verze:** 1.0 – DRAFT  
**Datum:** Únor 2026  
**Kontext:** Rozšíření platformy z pasivní analytické vrstvy na aktivní systém řízení celého životního cyklu OPEX reportingu.

---

## Změna scope – shrnutí

Původní scope platformy začínal nahráním hotového souboru (PPTX, Excel) a jeho zpracováním. Nový scope posouvá vstupní bod na samý začátek procesu – **vznik dat** – a přidává výstupní vrstvu – **generování standardizovaných reportů**.

```
PŮVODNÍ SCOPE:
[Hotový soubor] → Nahrání → Extrakce → Analýza → Dashboard

NOVÝ SCOPE:
[Vznik dat] → Sběr dat (formulář / Excel) → Schválení → Generování PPTX → Centrální reporting
     ↑                                            ↑
  FS19 (formuláře)                        FS17 (lifecycle)
  FS17 (Excel jako zdroj)                 FS18 (generování PPTX)
                                          FS20 (periody, termíny)
```

---

## FS17 – OPEX Report Lifecycle & Submission Workflow
**Priorita: KRITICKÁ**  
**Tech Stack:** Java 21 + Spring Boot (MS-LIFECYCLE)

### Business kontext

Každá dceřiná společnost musí do stanoveného termínu dodat OPEX data pro holding. Dnes tento proces probíhá přes e-mail, sdílené složky nebo SharePoint – bez transparentního přehledu o tom, kdo dodal, v jakém stavu jsou data a zda jsou připravena k převzetí do centrálního reportingu.

Platforma zavede **stavový automat pro každý report** s jasnými přechody, odpovědnostmi a auditní stopou.

### Stavy reportu (State Machine)

```
DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED
                  ↘              ↘ REJECTED → DRAFT (resubmit)
```

| Stav | Kdo nastavuje | Popis |
|---|---|---|
| `DRAFT` | Systém (při vytvoření) | Report byl vytvořen, data se teprve plní |
| `SUBMITTED` | Editor (dceřiná společnost) | Editor potvrdil, že data jsou kompletní a předává je k revizi |
| `UNDER_REVIEW` | HoldingAdmin / Reviewer | Holding přijal report k revizi |
| `APPROVED` | HoldingAdmin / Reviewer | Report schválen, data přijata do centrálního reportingu |
| `REJECTED` | HoldingAdmin / Reviewer | Report vrácen s komentářem k opravě; přechází zpět do `DRAFT` |

### Požadavky

- **Report entita:** Každý report je vázán na `(org_id, period_id, report_type)`. Jedno období = jeden report na organizaci.
- **Stavové přechody:** Každý přechod je logován v audit logu (kdo, kdy, z jakého stavu, do jakého, volitelný komentář).
- **Komentáře k rejection:** Při zamítnutí musí Reviewer povinně vyplnit důvod. Komentář je viditelný Editorovi v jeho UI.
- **Submission checklist:** Editor vidí checklist podmínek před potvrzením odesláním (`SUBMITTED`): jsou všechna povinná pole vyplněna, jsou všechny listy nahrané, odpovídají data validačním pravidlům?
- **Přehled pro HoldingAdmin:** Dashboard s matricí `[Společnost × Perioda]` a stavem každého reportu. Okamžitý přehled, kdo ještě nedodal.
- **Hromadné akce:** HoldingAdmin může schválit nebo zamítnout více reportů najednou.
- **Uzamčení dat po schválení:** Po přechodu do `APPROVED` jsou zdrojová data read-only. Jakákoli změna vyžaduje nový přechod do `DRAFT` (vytvoří novou verzi – viz FS14).

### Acceptance kritéria

- Editor nemůže odeslat report (`SUBMITTED`), dokud checklist nehlásí 100 % kompletnost.
- Přechod `APPROVED` automaticky triggeruje zahrnutí dat do centrálního reportingu.
- Přechod `REJECTED` automaticky odesílá notifikaci Editorovi (FS13) s komentářem.
- Audit log záznamu přechodu stavu obsahuje: `user_id`, `from_state`, `to_state`, `timestamp`, `comment`.
- Historie všech stavových přechodů jednoho reportu je zobrazitelná v UI (timeline view).

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-LIFECYCLE** | Report Lifecycle Service | Správa stavového automatu reportů, submission checklist, rejection flow, hromadné akce | Java 21 + Spring Boot | **L** |

---

## FS18 – PPTX Report Generation (Template Engine)
**Priorita: KRITICKÁ**  
**Tech Stack:** Python + FastAPI (MS-GEN-PPTX) + Java (MS-TMPL-PPTX)

### Business kontext

Dnes každá společnost generuje PPTX report sama – různé šablony, různé formáty grafů, různé pojmenování sekcí. HoldingAdmin pak manuálně sjednocuje vizuální podobu před prezentací vedení.

Nová funkce umožní **generovat standardizovaný PPTX report automaticky** ze strukturovaných zdrojových dat uložených v platformě, na základě centrálně spravované šablony.

Toto je **"obrácený Atomizer"** – místo extrakce dat z PPTX do DB jde o renderování dat z DB do PPTX.

### Požadavky

#### Správa PPTX šablon (MS-TMPL-PPTX)
- HoldingAdmin nahraje PPTX soubor jako šablonu (`POST /templates/pptx`).
- Šablona obsahuje **placeholder tagy** ve formátu `{{variable_name}}` v textových polích, `{{TABLE:table_name}}` pro tabulky, `{{CHART:metric_name}}` pro grafy.
- Systém šablonu naparsuje a extrahuje seznam všech placeholderů → zobrazí v UI jako "požadované datové vstupy".
- Šablony jsou verzovány (v1, v2). Přiřazení šablony k `period_id` nebo `report_type`.
- Náhled šablony v UI bez dat (placeholder hodnoty zobrazeny jako ukázka).

#### Generování reportu (MS-GEN-PPTX)
- `POST /generate/pptx` s `{ template_id, report_id }` → systém načte schválená zdrojová data z DB a vyrenderuje PPTX.
- Generátor nahradí textové placeholdery hodnotami, vyplní tabulky, vygeneruje grafy (python-pptx + matplotlib/plotly pro grafy).
- Výsledný PPTX uložen do Blob Storage, URL uložena k `report_id`.
- Generování probíhá asynchronně – výsledek doručen přes notifikaci (FS13) a WebSocket/SSE (FS09).
- **Batch generování:** HoldingAdmin může spustit generování PPTX pro všechny schválené reporty v periodě najednou.

#### Mapování dat na šablonu
- UI pro přiřazení: "Placeholder `{{it_costs}}` → pole `amount_czk` z formuláře / sloupec z Excel uploadu".
- Toto mapování je součástí konfigurace šablony, ne per-report.
- Pokud zdrojová data neobsahují hodnotu pro placeholder → report je vygenerován s výrazným vizuálním upozorněním (červený rámeček, text `DATA MISSING`), nikoli selháním.

### Acceptance kritéria

- Generování PPTX reportu ze 20 slidů dokončeno za < 60 s.
- Výsledný PPTX je validní soubor otevíratelný v MS PowerPoint a LibreOffice.
- Chybějící data nezpůsobí selhání generování – slide s chybějícím polem je označen `DATA MISSING`.
- Batch generování 10 reportů najednou dokončeno za < 15 minut.
- Vygenerovaný soubor ke stažení přímo z UI reportu.

### Nové microservices

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-TMPL-PPTX** | PPTX Template Manager | Nahrávání, verzování a správa PPTX šablon; extrakce placeholderů; mapování na datové zdroje | Java 21 + Spring Boot | **L** |
| **MS-GEN-PPTX** | PPTX Generator | Renderování PPTX ze zdrojových dat + šablony; placeholder substituce; grafy; batch generování | Python + FastAPI (python-pptx, matplotlib) | **L** |

---

## FS19 – Dynamic Form Builder & Data Collection
**Priorita: KRITICKÁ**  
**Tech Stack:** Java 21 + Spring Boot (MS-FORM) + React (součást MS-FE)

### Business kontext

Dnešní proces sběru dat funguje tak, že HoldingAdmin pošle e-mailem Excel šablonu, každá společnost ji vyplní a pošle zpět. Výsledkem jsou desítky různě modifikovaných Excelů, které musí analytik rukou konsolidovat.

Nová funkce nahradí tento proces **centrálním formulářem v platformě**. HoldingAdmin definuje, jaká data chce sbírat. Uživatelé dceřiných společností data vyplní přímo v platformě. Data jsou okamžitě strukturovaně uložena – bez parsování, bez deduplikace, bez manuální práce.

Platforma zároveň nadále podporuje **nahrávání Excel souborů jako datový vstup** pro případy, kdy jsou data rozsáhlá nebo vznikají v externím systému.

### Požadavky

#### Form Builder (pro HoldingAdmin / Editor)
- UI editor pro tvorbu formulářů metodou drag & drop.
- Typy polí: `text`, `number` (s volitelnou měnou/jednotkou), `percentage`, `date`, `dropdown` (výběr z předdefinovaných hodnot), `table` (uživatel vyplňuje tabulku s pevnými sloupci), `file_attachment` (příloha).
- Povinnost pole (`required: true/false`).
- Validační pravidla na úrovni pole: min/max hodnota, regex pattern, závislost na jiném poli (`if field_A > 0 then field_B is required`).
- Sekce a popisné texty pro strukturování formuláře.
- Náhled formuláře před publikováním.
- **Verzování formuláře:** Změna publikovaného formuláře vytvoří novou verzi. Existující vyplněná data jsou vázána na verzi, ve které byla vyplněna.

#### Správa formulářů
- Formulář je přiřazen k `period_id` a `report_type`.
- Přiřazení formuláře ke konkrétním společnostem (ne vždy všechny vyplňují stejný formulář – holdingová struktura).
- Stav formuláře: `DRAFT` (jen admin vidí), `PUBLISHED` (viditelný přiřazeným uživatelům), `CLOSED` (nelze vyplňovat, deadline).
- **Deadline:** Formulář lze uzavřít ručně nebo automaticky k datu (napojení na FS20).

#### Vyplňování formuláře (pro Editor / dceřiná společnost)
- Editor vidí seznam formulářů k vyplnění v aktuálním období.
- Průběžné ukládání (`auto-save` každých 30 s nebo při přechodu mezi sekcemi).
- Validace v reálném čase – chybová pole označena před odesláním.
- Možnost uložit jako `DRAFT` a vrátit se later.
- Po odeslání (`SUBMITTED`) přechod do submission workflow (FS17).
- **Komentáře na úrovni pole:** Editor může přidat vysvětlující komentář k jakékoli hodnotě (např. "Toto číslo zahrnuje jednorázový odpis z Q1.").

#### Excel jako alternativní datový vstup
- Vedle formuláře může Editor nahrát Excel soubor jako datový vstup.
- Systém Excel naparsuje (MS-ATM-XLS) a nabídne **mapování sloupců → pole formuláře** (napojení na FS15 Schema Mapping).
- Editor zkontroluje a potvrdí mapování → data jsou importována do formuláře.
- Po importu jsou data editovatelná jako kdyby byla zadána ručně.
- Původní Excel soubor je uložen jako příloha reportu (auditní stopa).

### Acceptance kritéria

- HoldingAdmin vytvoří a publikuje nový formulář do 10 minut bez technických znalostí.
- Auto-save funguje; po ztrátě připojení a znovuotevření jsou data zachována.
- Validace formuláře vrátí seznam všech chybných polí najednou, nikoli po jednom.
- Import z Excelu: mapování sloupců navrženo automaticky na základě FS15; Editor potvrdí za < 2 minuty.
- Vyplněná data jsou okamžitě dostupná v centrálním reportingu bez dalšího zpracování.
- Formulář verze v1 a v2 jsou uložena odděleně; historická data nejsou přepsána upgradem formuláře.

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-FORM** | Form Builder & Data Collection | Definice formulářů, správa verzí, sběr dat, validace, Excel import, napojení na MS-LIFECYCLE | Java 21 + Spring Boot | **XL** |

---

## FS20 – Reporting Period & Deadline Management
**Priorita: VYSOKÁ**  
**Tech Stack:** Java 21 + Spring Boot (MS-PERIOD)

### Business kontext

OPEX reporting probíhá v opakujících se cyklech (měsíčně, kvartálně, ročně). Každý cyklus má deadline, v rámci kterého musí všechny společnosti dodat data. Dnes tyto termíny existují jen v e-mailech a kalendářích – systém o nich neví a nemůže automaticky upozorňovat, uzavírat formuláře nebo generovat eskalace.

### Požadavky

#### Správa period
- HoldingAdmin vytvoří reportovací periodu: `{ name, type: MONTHLY|QUARTERLY|ANNUAL, start_date, submission_deadline, review_deadline, period_code }`.
- Perioda je přiřazena k holdingu a viditelná všem dceřiným společnostem.
- Stav periody: `OPEN` → `COLLECTING` → `REVIEWING` → `CLOSED`.
- Perioda lze klonovat z předchozí (přenese přiřazení formulářů a šablon).

#### Deadline management
- **Submission deadline:** Datum, do kdy musí společnosti odeslat data (`SUBMITTED`). Po deadlinu formuláře automaticky přejdou do `CLOSED` (nelze již vyplňovat). Opozdilé submisse vyžadují explicitní override od HoldingAdmina.
- **Review deadline:** Datum, do kdy musí Holding schválit/zamítnout přijaté reporty.
- **Automatické upozornění:** X dní před deadlinem systém odešle notifikaci (FS13) všem, kdo ještě nemají `SUBMITTED`. Konfigurovatelný počet dní (default: 7, 3, 1 den před).
- **Eskalace:** Pokud společnost nepodá v termínu, HoldingAdmin dostane upozornění s přehledem neplničů.

#### Completion tracking
- Dashboard periody: matice `[Společnost × Stav]` s barvovým rozlišením (šedá = DRAFT, žlutá = SUBMITTED, zelená = APPROVED, červená = REJECTED/overdue).
- Procento dokončenosti periody (počet APPROVED / celkový počet povinných reportů).
- Export statusu periody jako PDF nebo Excel pro vedení.

#### Historická data a srovnání period
- Každá uzavřená perioda je archivována a přístupná pro srovnávací analýzu.
- Dashboard umožní srovnání stejné metriky napříč periodami (Q1/2024 vs. Q1/2025).
- Napojení na MS-VER (FS14): opravy v rámci periody vytvářejí verze, nikoli přepisují historii.

### Acceptance kritéria

- Vytvoření nové periody klonem z předchozí trvá < 2 minuty.
- Automatické uzavření formulářů po submission deadlinu bez manuálního zásahu.
- Notifikace odeslána 7/3/1 den před deadlinem všem uživatelům s `DRAFT` nebo nevyplněným formulářem.
- Dashboard periody se načte se stavem všech společností za < 3 s.
- Export statusu periody funkční pro 50+ společností.

### Nová microservice

| Unit ID | Název | Popis | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-PERIOD** | Reporting Period Manager | Správa period a deadlinů, automatické uzavírání, completion tracking, eskalace, historické srovnání | Java 21 + Spring Boot | **M** |

---

## Dopad na stávající Feature Sets

Nový scope nevyžaduje přepsání FS01–FS16, ale mění nebo rozšiřuje kontext několika z nich:

| Stávající FS | Změna / Rozšíření |
|---|---|
| **FS02 – File Ingestor** | Excel upload nyní slouží jako **datový vstup do formuláře** (FS19), nejen jako soubor k parsování. Ingestor musí rozlišit `upload_purpose: PARSE` (původní flow) vs. `upload_purpose: FORM_IMPORT` (nový flow → MS-FORM). |
| **FS03 – Atomizers** | Přidává se **obrácený směr**: MS-GEN-PPTX (generování). Cleanup Worker (MS-ATM-CLN) musí zahrnout i dočasné soubory generátoru. |
| **FS05 – Sinks** | MS-SINK-TBL musí ukládat i data z formulářů (FS19). Schéma: `form_responses` tabulka s `(org_id, period_id, form_version_id, field_id, value, submitted_at)`. |
| **FS06 – Analytics & Query** | MS-DASH musí zobrazovat i data pocházející z formulářů, nejen z parsovaných souborů. Zdroj dat je transparentní (flag `source_type: FORM / FILE`). |
| **FS07 – Admin Backend** | Přidávají se správa formulářů a šablon jako admin sekce. Role "Reviewer" jako nová sub-role HoldingAdmina pro schvalování reportů (FS17). |
| **FS08 – Batch Management** | "Batch" se nově mapuje přímo na "Reporting Period" (FS20). Koncepty se slučují – `period_id` nahrazuje generický `batch_id` tam, kde jde o OPEX reporting. |
| **FS09 – Frontend** | Nové obrazovky: Form Builder UI, formulář pro vyplnění, submission workflow UI, period dashboard, PPTX generator trigger. Celkový rozsah MS-FE roste z **XL** na **XXL**. |
| **FS13 – Notifications** | Přidávají se notifikační triggery z FS17 (stavové přechody) a FS20 (deadliny, eskalace). Typy notifikací rozšířeny o: `REPORT_SUBMITTED`, `REPORT_APPROVED`, `REPORT_REJECTED`, `DEADLINE_APPROACHING`, `DEADLINE_MISSED`. |
| **FS15 – Schema Mapping** | Nově voláno i z FS19 (Excel import do formuláře), nejen z N8N pipeline. MS-TMPL dostane nový endpoint `POST /map/excel-to-form`. |
| **FS16 – Audit Log** | Auditovány i stavové přechody z FS17 a veškeré akce ve formuláři (pole změněno, komentář přidán, import potvrzen). |

---

## Aktualizovaný katalog – nové microservices (přehled)

| Unit ID | Název | FS | Tech Stack | Effort |
|---|---|---|---|---|
| **MS-LIFECYCLE** | Report Lifecycle Service | FS17 | Java 21 + Spring Boot | **L** |
| **MS-TMPL-PPTX** | PPTX Template Manager | FS18 | Java 21 + Spring Boot | **L** |
| **MS-GEN-PPTX** | PPTX Generator | FS18 | Python + FastAPI | **L** |
| **MS-FORM** | Form Builder & Data Collection | FS19 | Java 21 + Spring Boot | **XL** |
| **MS-PERIOD** | Reporting Period Manager | FS20 | Java 21 + Spring Boot | **M** |

**Celkový počet microservices po rozšíření: 30** (původně 25)

---

## Aktualizovaný Rollout – Fáze

Nové FS jsou navrženy tak, aby navazovaly na stávající fáze a neblokují MVP:

| Fáze | Název | Nové součásti | Napojení na původní fáze |
|---|---|---|---|
| **P3b** | Core Lifecycle | MS-LIFECYCLE, MS-PERIOD (základní) | Po P2 – vyžaduje fungující Sinks a Auth |
| **P3c** | Data Collection | MS-FORM (základní formulář + Excel import) | Paralelně s P3b |
| **P4b** | Report Generation | MS-TMPL-PPTX, MS-GEN-PPTX | Po P3b – vyžaduje schválená data v DB |
| **P4c** | Advanced Period Mgmt | MS-PERIOD (deadliny, eskalace, srovnání) | Po P3b + P4b |

---

## Otevřené otázky k rozhodnutí

Před implementací je třeba upřesnit:

1. **Formulář vs. Excel** – Bude formulář **povinný** vstup, nebo alternativní k Excel uploadu? Nebo záleží na type reportu? Toto ovlivňuje submission checklist v FS17.
2. **Granularita formuláře** – Formulář pro celou společnost, nebo per-divize/cost center? (Ovlivňuje datový model MS-FORM a RLS.)
3. **PPTX šablona – ownership** – Kdo smí šablonu měnit? Pouze HoldingAdmin, nebo i lokální Editori mohou mít vlastní varianty? (Ovlivňuje MS-TMPL-PPTX verzování.)
4. **Workflow customizace** – Je stavový automat v FS17 pevný, nebo bude různý pro různé typy reportů? (Např. měsíční report má jen 2 kroky, roční má 4.)
5. **Srovnání period** – Jaká je granularita historického srovnání? Pouze stejná perioda v jiném roce, nebo libovolné porovnání? (Ovlivňuje komplexitu MS-DASH a MS-PERIOD.)

---

*Rozšíření scope: FS17–FS20 | PPTX Analyzer & Automation Platform | Únor 2026*
