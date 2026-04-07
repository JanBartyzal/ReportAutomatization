# UAT Errors - step18_PPTX_Generation

Timestamp: 2026-04-07T14:13:18

[FAIL] Expected 200, got 400 for POST configure-mappings
## Unexpected Status
- Endpoint: `POST /api/templates/pptx/4164b4ca-3a73-40e0-8569-3caf4b81ef54/mappings`
- Expected: 200
- Got: 400
- Body: `<binary 462 bytes, content-type=application/problem+json>`

[FAIL] Expected 200, got 202 for POST batch-generate-pptx
## Unexpected Status
- Endpoint: `POST /api/templates/pptx/generate/batch`
- Expected: 200
- Got: 202
- Body: `{"job_id": "adbb2ceb-cc64-4643-b377-758f271f1b93", "template_id": "4164b4ca-3a73-40e0-8569-3caf4b81ef54", "report_ids": ["95175d9f-7dab-4d7e-a795-fe32e2f0d2c7", "3ee200ae-a77e-4d0e-a811-394f516e816f"], "batch_id": "adbb2ceb-cc64-4643-b377-758f271f1b93", "message": "Batch generation queued", "status"`
