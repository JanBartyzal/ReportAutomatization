# UAT Errors - step18_PPTX_Generation

Timestamp: 2026-04-09T13:02:02

[FAIL] Expected 200, got 400 for POST configure-mappings
## Unexpected Status
- Endpoint: `POST /api/templates/pptx/04575e1d-efbc-4d20-b891-d3f2e4ba298f/mappings`
- Expected: 200
- Got: 400
- Body: `<binary 462 bytes, content-type=application/problem+json>`

[FAIL] Expected 200, got 202 for POST batch-generate-pptx
## Unexpected Status
- Endpoint: `POST /api/templates/pptx/generate/batch`
- Expected: 200
- Got: 202
- Body: `{"batch_id": "4fcb1ed7-185a-4c31-b39e-9e78663de6e6", "message": "Batch generation queued", "status": "QUEUED", "job_id": "4fcb1ed7-185a-4c31-b39e-9e78663de6e6", "template_id": "04575e1d-efbc-4d20-b891-d3f2e4ba298f", "report_ids": ["6905e3ff-86b5-41cf-a67d-85da41fe4209", "b6c09d13-d5c3-4e30-b4c5-5d9c`
