# UAT Errors - step18_PPTX_Generation

Timestamp: 2026-03-23T12:51:15

[FAIL] Expected 201, got 500 for POST upload-pptx-template
## Unexpected Status
- Endpoint: `POST /api/templates/pptx`
- Expected: 201
- Got: 500
- Body: `<binary 133 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/templates/pptx/generate/batch`
- Description: No template_id available for batch generation test
