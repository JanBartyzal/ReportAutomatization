# UAT Errors - step06_Analytics_Query

Timestamp: 2026-03-23T12:51:11

[FAIL] Expected 200, got 403 for GET search-item4
## Unexpected Status
- Endpoint: `GET /api/search`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `GET /api/search?q=Item4`
- Description: Search endpoint not implemented yet or requires different auth

[FAIL] Expected 200, got 403 for GET dashboard-summary
## Unexpected Status
- Endpoint: `GET /api/dashboards/summary`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `GET /api/dashboards/summary`
- Description: Dashboard summary endpoint not implemented yet or requires different auth

[FAIL] Expected 200, got 403 for POST aggregation-query
## Unexpected Status
- Endpoint: `POST /api/query/aggregate`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/query/aggregate`
- Description: Aggregation query endpoint not implemented yet or requires different auth

[FAIL] Expected 200, got 403 for GET aggregation-fallback-dashboard
## Unexpected Status
- Endpoint: `GET /api/dashboards/summary`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `GET /api/dashboards/summary (aggregation fallback)`
- Description: Dashboard summary endpoint not available for aggregation fallback

[FAIL] Expected 200, got 403 for POST vector-search
## Unexpected Status
- Endpoint: `POST /api/search/semantic`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/search/semantic`
- Description: Vector/semantic search not yet implemented or requires different auth
