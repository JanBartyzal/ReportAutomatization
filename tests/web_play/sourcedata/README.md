# Batch Import Source Data

These XLSX workbooks are intentionally small, deterministic E2E fixtures for FS02, FS11, FS25, FS26, and FS27.

They model two isolated OPEX import batches:

- `BATCH-A-2026-04`: four workbooks across holding, company, division, and cost-center levels.
- `BATCH-B-2026-05`: three workbooks across holding, company, and division levels.

Each workbook contains:

- `Metadata`: batch id, period, holding/company/division/cost center, and entity level.
- `OpexLines`: normalized sink rows with a stable `sink_key`.
- `PersistPlan`: the target sink table, persistent SQL table, merge key, batch column, and expected output report filenames.

Regenerate the XLSX files with:

```powershell
.\generate-source-excels.ps1
```
