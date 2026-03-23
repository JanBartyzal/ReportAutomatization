# UAT Errors - step24_Smart_Persistence

Timestamp: 2026-03-23T12:51:29

[FAIL] Expected 200, got 500 for GET list-promotion-candidates
## Unexpected Status
- Endpoint: `GET /api/admin/promotions/candidates`
- Expected: 200
- Got: 500
- Body: `<binary 153 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `/api/admin/promotions/candidates`
- Description: List promotion candidates

[FAIL] Expected 200, got 500 for GET get-schema-proposal
## Unexpected Status
- Endpoint: `GET /api/admin/promotions/candidates/candidate-1/schema`
- Expected: 200
- Got: 500
- Body: `<binary 172 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `/api/admin/promotions/candidates/candidate-1/schema`
- Description: Get schema proposal for promotion candidate

[FAIL] Expected 200, got 500 for POST approve-promotion
## Unexpected Status
- Endpoint: `POST /api/admin/promotions/candidates/candidate-1/approve`
- Expected: 200
- Got: 500
- Body: `<binary 173 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `/api/admin/promotions/candidates/candidate-1/approve`
- Description: Approve promotion of candidate to dedicated table

## Missing Feature (informational)
- Endpoint: `/api/admin/promotions/candidates/{id}`
- Description: Verify routing update after promotion
