# RA Playwright UX Tests

End-to-end UX/UI tests for the ReportAutomatization (RA) frontend, built with **Playwright + TypeScript**.

## What is tested

| FS | Area | Key UX checks |
|----|------|---------------|
| FS09 | Auth & Navigation | Login page a11y, sidebar nav items, session persistence, RBAC |
| FS09 | File Upload | Dropzone visibility, accepted types hint, progress indicator, error feedback |
| FS11 | Dashboards | List/empty state, create button, chart type selector, SQL editor |
| FS12 | Search, AI & MCP | Global search, search results filters, named query catalog, text template renderer |
| FS13 | Notifications | Bell icon a11y, unread badge, panel open/close, notification settings |
| FS14 | Versioning | Version history timeline, version badges, diff view, colour highlights |
| FS15 | Schema Mapping | Templates list, create button, mapping row editor, "Suggest" button |
| FS16 | Audit Log | Table load, required columns, date filter, export button, immutability |
| FS17 | Report Lifecycle | Status badges, matrix view, submission checklist, rejection comment required, timeline |
| FS18 | PPTX Generation | Template list, placeholder display, Generate button, loading indicator |
| FS19 | Form Builder | Canvas, field palette, preview, publish, autosave |
| FS19 | Form Filling | Field labels, aria-required, validation (all errors at once), autosave, per-field comments |
| FS20 | Period Management | Status badges, completion %, create dialog fields, clone, matrix, export |
| FS21 | Local Scope | /local route, scope badges, LOCAL option in form wizard, Release button |
| FS22 | Period Comparison | Comparison page, period selector, org filter, chart/table, delta values |
| FS23 | Integrations | ServiceNow section, connection form fields a11y, Test Connection, schedule, history |
| FS24 | Data Promotion | Smart persistence candidates, promoted tables, DDL preview, approve/dismiss controls |
| FS25 | Sink Browser | List/filters/pagination, checkboxes, detail inline editing, highlighted corrections |
| FS26 | Report Generation | Create report dialog (org/period/type), generate button, download link, batch page |
| FS27 | Excel Sync | Export flows list, create dialog (SQL/target/sheet/trigger), Export Now button |
| FS99 | UX Quality | Page titles, landmarks, all-buttons-accessible, responsive layout, keyboard nav, loading states, toast roles, colour contrast |

## Structure

```
tests/web_play/
├── playwright.config.ts      # Multi-project config (admin, editor, viewer, UX viewports)
├── package.json
├── tsconfig.json
├── global-setup.ts           # Creates auth storageState files before all tests
├── config/
│   └── config.ts             # BASE_URL, USERS, ROUTES, TIMEOUTS
├── fixtures/
│   └── auth.fixture.ts       # Custom test fixtures + helpers (gotoAndWait, featurePresent)
├── pages/
│   ├── AppPage.ts            # Sidebar, notifications, loading states, a11y helpers
│   ├── UploadPage.ts         # File upload interactions
│   ├── ReportsPage.ts        # Reports table, status transitions, approve/reject
│   └── FormsPage.ts          # Form builder canvas, field palette, autosave
├── tests/
│   ├── auth.setup/           # Validates auth state files (runs before other projects)
│   ├── FS09_Auth_Navigation/
│   ├── FS09_File_Upload/
│   ├── FS11_Dashboards/
│   ├── FS12_Search_AI_MCP/
│   ├── FS13_Notifications/
│   ├── FS14_Versioning/
│   ├── FS15_Schema_Mapping/
│   ├── FS16_Audit/
│   ├── FS17_Report_Lifecycle/
│   ├── FS18_PPTX_Generation/
│   ├── FS19_Form_Builder/
│   ├── FS19_Form_Filling/
│   ├── FS20_Period_Management/
│   ├── FS21_Local_Scope/
│   ├── FS22_Period_Comparison/
│   ├── FS23_Integrations/
│   ├── FS24_Data_Promotion/
│   ├── FS25_Sink_Browser/
│   ├── FS26_Report_Generation/
│   ├── FS27_Excel_Sync/
│   └── FS99_UX_Quality/
├── logs/
│   ├── auth/                 # storageState JSON files (auto-generated)
│   └── html-report/          # Playwright HTML report (auto-generated)
└── run.ps1                   # PowerShell test runner
```

## Prerequisites

1. **Node.js 18+** installed and in PATH
2. **Frontend running** on `http://localhost:5173`:
   ```bash
   cd apps/frontend && npm run dev
   ```

## First-time setup

```powershell
cd tests/web_play
npm install
npx playwright install chromium
```

## Running tests

### All tests
```powershell
.\run.ps1
```

### Specific suite
```powershell
.\run.ps1 -Suite lifecycle        # FS17 report lifecycle
.\run.ps1 -Suite forms            # FS19 form builder
.\run.ps1 -Suite search           # FS12 search / query tooling
.\run.ps1 -Suite promotion        # FS24 data promotion
.\run.ps1 -Suite ux               # FS99 UX quality
```

### Headed mode (visible browser)
```powershell
.\run.ps1 -Headed
.\run.ps1 -Suite auth -Headed
```

### Debug mode (Playwright Inspector)
```powershell
.\run.ps1 -Suite lifecycle -Debug
```

### Open HTML report after run
```powershell
.\run.ps1 -Report
# or
npx playwright show-report logs/html-report
```

### Different base URL
```powershell
.\run.ps1 -BaseUrl "http://myserver:5173"
```

### Skip frontend preflight
```powershell
.\run.ps1 -SkipFrontendCheck
```

## Authentication

Tests use **dev bypass mode** — no real Azure credentials needed.  
`global-setup.ts` navigates to the app and saves `storageState` (cookies + localStorage) per role:
- `logs/auth/holding-admin.json`
- `logs/auth/editor.json`
- `logs/auth/viewer.json`

Each Playwright project reuses the appropriate storageState so tests start already logged in.

## Missing features

Tests use the `featurePresent()` helper — when a feature is not yet implemented, the test logs a `[MISSING FEATURE]` warning and skips gracefully rather than failing. This lets the suite run green against a partial implementation while clearly documenting what is missing.

## Output

- **HTML report:** `logs/html-report/index.html`
- **JSON results:** `logs/results.json`
- **Screenshots:** `test-results/` (on failure)
- **Traces:** `test-results/` (on retry)
