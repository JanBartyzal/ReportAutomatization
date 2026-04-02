# UAT Errors - step05_Sinks_Persistence

Timestamp: 2026-04-02T13:58:57

[FAIL] XLSX table has 5 records (got 1)
## Assertion Failed
- Message: XLSX table has 5 records (got 1)

[FAIL] XLSX contains all project names {'Item4', 'Item1', 'Item3', 'Item5', 'Item2'} (found set())
## Assertion Failed
- Message: XLSX contains all project names {'Item4', 'Item1', 'Item3', 'Item5', 'Item2'} (found set())

## Missing Feature (skipped)
- Endpoint: `GET /api/query/documents?file_id=...`
- Description: Documents endpoint requires document_id param, not file_id — use /api/query/documents/{document_id}

[FAIL] Cross-tenant: admin2 sees 0 records for admin1's file (got 2)
## Assertion Failed
- Message: Cross-tenant: admin2 sees 0 records for admin1's file (got 2)
