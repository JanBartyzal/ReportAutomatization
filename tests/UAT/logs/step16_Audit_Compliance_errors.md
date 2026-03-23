# UAT Errors - step16_Audit_Compliance

Timestamp: 2026-03-23T11:53:07

[FAIL] Expected 200, got 403 for GET read-file-for-audit
## Unexpected Status
- Endpoint: `GET /api/query/files/9583988f-70be-4a2f-aa82-8e97ef515ea8`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

[FAIL] Expected 200, got 500 for GET audit-export-csv
## Unexpected Status
- Endpoint: `GET /api/audit/logs/export`
- Expected: 200
- Got: 500
- Body: `<binary 143 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `GET /api/audit/logs/export?format=csv`
- Description: Endpoint not implemented yet

[FAIL] Expected 200, got 500 for GET audit-export-json
## Unexpected Status
- Endpoint: `GET /api/audit/logs/export`
- Expected: 200
- Got: 500
- Body: `<binary 143 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `GET /api/audit/logs/export?format=json`
- Description: Endpoint not implemented yet
