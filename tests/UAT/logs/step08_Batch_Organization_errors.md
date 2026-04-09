# UAT Errors - step08_Batch_Organization

Timestamp: 2026-04-09T13:01:48

[FAIL] Expected 200, got 201 for POST assign-file-to-batch
## Unexpected Status
- Endpoint: `POST /api/batches/42a7669f-b08f-432d-99f0-dfb77bce2d6b/files`
- Expected: 200
- Got: 201
- Body: `{"id": "2db37007-7084-4314-a2de-ed2f75c10c53", "batchId": "42a7669f-b08f-432d-99f0-dfb77bce2d6b", "fileId": "72cdf1e9-ae90-4302-a51f-9386bf5837ba", "addedAt": "2026-04-09T11:01:48.525765914Z", "addedBy": "6bbc3213-00ac-4d30-bf27-7477b207c515"}`
