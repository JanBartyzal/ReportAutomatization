# UAT Errors - step14_Data_Versioning

Timestamp: 2026-03-23T12:51:14

[FAIL] Expected 201, got 500 for POST create-new-version
## Unexpected Status
- Endpoint: `POST /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1`
- Expected: 201
- Got: 500
- Body: `<binary 176 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1`
- Description: Version creation endpoint returned error — feature not fully implemented yet

[FAIL] Expected 200, got 404 for GET diff-versions
## Unexpected Status
- Endpoint: `GET /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1/diff`
- Expected: 200
- Got: 404
- Body: `<binary 206 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `GET /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1/diff`
- Description: Version diff endpoint not implemented

[FAIL] Expected 405, got 500 for PUT immutable-version-check
## Unexpected Status
- Endpoint: `PUT /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1`
- Expected: 405
- Got: 500
- Body: `<binary 176 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `PUT /api/versions/file/480fbe07-538d-4b5d-aa76-10dea0064cb1`
- Description: Version immutability check — endpoint not fully implemented yet
