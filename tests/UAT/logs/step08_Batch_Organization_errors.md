# UAT Errors - step08_Batch_Organization

Timestamp: 2026-03-23T12:51:12

[FAIL] Expected 200, got 404 for POST assign-file-to-batch
## Unexpected Status
- Endpoint: `POST /api/batches/b926a4b0-7a2a-422f-abc2-9fceed1ebb99/files`
- Expected: 200
- Got: 404
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/batches/b926a4b0-7a2a-422f-abc2-9fceed1ebb99/files`
- Description: File-to-batch assignment not implemented yet
