# FS27 Multi-Batch Excel Import Traceability

This document maps `batch_import_reporting.spec.ts` to the project charter areas it protects.

## Covered Charter Areas

| Area | Coverage |
| --- | --- |
| FS02 File Ingestor | Multi-file Excel upload contract, `FORM_IMPORT` purpose, batch assignment. |
| FS11 Dashboards | Dashboard SQL combines sink rows and persisted rows by `batch_id`. |
| FS25 Sink Browser / Persistence | Sink rows are tied to `sink_opex_lines` and persisted into `opex_actuals_persisted`. |
| FS26 Report Generation | Batch output contract includes Excel summary, PDF report, and combined reconciliation workbook. |
| FS27 Excel Sync | Export flow materializes SQL-backed batch data into Excel outputs. |

## Test Data

Source workbooks live in `tests/web_play/sourcedata`.

- `BATCH-A-2026-04`: 4 Excel files across holding, company, division, and cost-center hierarchy levels.
- `BATCH-B-2026-05`: 3 Excel files across holding, company, and division hierarchy levels.

Every source workbook contains:

- `Metadata`
- `OpexLines`
- `PersistPlan`

## Hard Assertions

The spec fails if:

- source XLSX files are missing or malformed,
- workbook sheet names or required rows do not match the manifest,
- an import batch has fewer than 2 or more than 4 Excel files,
- source rows point to a different `batch_id`,
- source rows target a different persistent table than `opex_actuals_persisted`,
- API-level sink results leak rows from another batch,
- dashboard SQL results leak rows from another batch,
- generated Excel/PDF artifacts are missing, empty, unreadable, or contain rows from another batch.

## Soft UI Checks

The current Playwright suite style keeps some incomplete UI surfaces as soft warnings through `featurePresent()`.

Known soft warnings at the time this traceability note was added:

- Excel source file input on `/upload`
- Dashboard SQL editor on `/dashboards/new`
- New export flow button on `/admin/export-flows`

These warnings should be converted into hard failures once the corresponding UI surfaces are considered complete.
