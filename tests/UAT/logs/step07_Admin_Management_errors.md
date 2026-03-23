# UAT Errors - step07_Admin_Management

Timestamp: 2026-03-23T12:51:12

[FAIL] Expected 200, got 500 for POST create-api-key
## Unexpected Status
- Endpoint: `POST /api/admin/api-keys`
- Expected: 200
- Got: 500
- Body: `<binary 140 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/admin/api-keys`
- Description: API key creation endpoint not implemented yet

[FAIL] Expected 403, got 500 for POST rbac-viewer-create-key
## Unexpected Status
- Endpoint: `POST /api/admin/api-keys`
- Expected: 403
- Got: 500
- Body: `<binary 140 bytes, content-type=application/problem+json>`
