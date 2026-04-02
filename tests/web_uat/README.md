# Web UAT Tests - ReportAutomatization

## Overview

Web UI UAT tests for ReportAutomatization (RA) frontend using Selenium WebDriver.

## Structure

```
tests/web_uat/
├── config/
│   └── web_config.py          # RA-specific config (BASE_URL, users, timeouts)
├── shared/
│   └── web_common.py         # WebTestSession class with Selenium helpers
├── pages/
│   └── ra_pages.py           # Page objects for RA pages
├── tests/
│   ├── Step09_Auth_Navigation/   # FS09: Auth, sidebar nav
│   ├── Step09_File_Upload/      # FS09: Upload manager
│   ├── Step17_Report_Lifecycle/ # FS17: State transitions
│   ├── Step18_PPTX_Generation/  # FS18: Template & generation
│   ├── Step19_Form_Builder/     # FS19: Form creation
│   ├── Step19_Form_Filling/     # FS19: Form filling
│   └── Step20_Period_Dashboard/ # FS20: Period management
├── run_all_tests.py          # Test runner
├── run.ps1                   # PowerShell runner
└── logs/                     # Test outputs
```

## Prerequisites

1. **Start the frontend dev server:**
   ```bash
   cd apps/frontend
   npm run dev
   # Frontend runs on http://localhost:5173
   ```

2. **Install Python dependencies:**
   ```bash
   pip install selenium requests
   ```

3. **Install ChromeDriver** (if using Chrome):
   - Download matching version for your Chrome
   - Add to PATH or use webdriver-manager

## Running Tests

### Python runner:
```bash
cd tests/web_uat
python run_all_tests.py
```

### PowerShell runner:
```powershell
cd tests/web_uat
.\run.ps1
```

### With headless browser:
```bash
$env:SELENIUM_HEADLESS="true"
python run_all_tests.py
```

## Features Tested

### FS09 - Frontend SPA
- Login/logout with MSAL authentication
- Session persistence
- Sidebar navigation
- Role-based access (HoldingAdmin, Editor, Viewer)

### FS17 - Report Lifecycle
- Report list with status badges
- Matrix view (Company × Period)
- State transitions: DRAFT → SUBMITTED → APPROVED/REJECTED
- Rejection flow with comment

### FS18 - PPTX Generation
- Template listing
- Placeholder preview
- Generate button
- PPTX download

### FS19 - Form Builder & Filling
- Form builder with drag & drop fields
- Field types: text, number, date, dropdown, table
- Auto-save indicator
- Validation errors
- Excel import/export
- Submission checklist

### FS20 - Period Management
- Period listing with status badges
- Matrix view (Company × Period)
- Completion percentage
- Deadline tracking
- Status legend

## Integration with Backend UAT

The web UI tests share state with backend UAT tests via `logs/uat_state.json`:
- Auth tokens written by `Step00_Init`
- Read by frontend tests for session restoration

## Output

- **Logs:** `tests/web_uat/logs/`
- **UAT Report:** `tests/web_uat/logs/web_uat_report_YYYYMMDD_HHMMSS.md`

## Notes

- Tests use **dev bypass authentication** - no real credentials needed
- Frontend must be running on port 5173
- Tests are designed for **Chrome** (default) but support Firefox and Edge
- Missing features are flagged as `[MISSING_FEATURE]` - not test failures
