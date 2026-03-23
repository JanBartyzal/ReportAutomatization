# UAT Errors - step12_API_AI_MCP

Timestamp: 2026-03-23T12:51:13

[FAIL] Expected 201, got 500 for POST create-api-key
## Unexpected Status
- Endpoint: `POST /api/admin/api-keys`
- Expected: 201
- Got: 500
- Body: `<binary 140 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/admin/api-keys`
- Description: Endpoint not implemented yet

## Missing Feature (informational)
- Endpoint: `POST /api/query/ai/analyze`
- Description: AI semantic analysis endpoint not available on engine-data

## Missing Feature (informational)
- Endpoint: `GET /api/query/ai/quota`
- Description: AI cost control / quota endpoint not available on engine-data

## Missing Feature (informational)
- Endpoint: `GET /api/query/mcp/health`
- Description: MCP server health endpoint not available on engine-data
