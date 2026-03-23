# UAT Errors - step09_Frontend_SPA

Timestamp: 2026-03-23T12:51:13

[FAIL] Expected 200, got 500 for POST auth-refresh
## Unexpected Status
- Endpoint: `POST /api/auth/refresh`
- Expected: 200
- Got: 500
- Body: `<binary 138 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/auth/refresh`
- Description: Token refresh not implemented

[FAIL] Expected 200, got 500 for GET sse-stream
## Unexpected Status
- Endpoint: `GET /api/notifications/stream`
- Expected: 200
- Got: 500
- Body: `<binary 139 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `GET /api/notifications/stream`
- Description: SSE not implemented
