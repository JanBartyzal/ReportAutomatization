# UAT Errors - step21_Local_Forms

Timestamp: 2026-03-23T12:51:16

[FAIL] Expected 201, got 400 for POST create-local-form
## Unexpected Status
- Endpoint: `POST /api/forms`
- Expected: 201
- Got: 400
- Body: `<binary 175 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/forms`
- Description: Endpoint not implemented yet

[FAIL] Expected 201, got 500 for POST create-local-pptx-template
## Unexpected Status
- Endpoint: `POST /api/templates/pptx`
- Expected: 201
- Got: 500
- Body: `<binary 133 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/templates/pptx`
- Description: Endpoint not implemented yet
