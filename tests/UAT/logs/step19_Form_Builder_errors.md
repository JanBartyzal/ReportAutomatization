# UAT Errors - step19_Form_Builder

Timestamp: 2026-04-09T13:02:03

[FAIL] Expected 200, got 500 for GET export-excel-template
## Unexpected Status
- Endpoint: `GET /api/forms/42d42968-7807-40c8-a22a-8a1a922c78a1/export/excel-template`
- Expected: 200
- Got: 500
- Body: `<binary 183 bytes, content-type=application/problem+json>`

[FAIL] Expected 200, got 500 for POST import-excel
## Missing Feature (skipped)
- Endpoint: `POST /api/forms/42d42968-7807-40c8-a22a-8a1a922c78a1/import/excel`
- Description: Excel import not yet implemented
