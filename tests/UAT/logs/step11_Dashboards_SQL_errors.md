# UAT Errors - step11_Dashboards_SQL

Timestamp: 2026-03-23T12:51:13

[FAIL] Expected 201, got 403 for POST create-dashboard
## Unexpected Status
- Endpoint: `POST /api/dashboards`
- Expected: 201
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/dashboards`
- Description: Endpoint not implemented yet or auth headers not forwarded to engine-data

[FAIL] Expected 200, got 403 for GET list-dashboards
## Unexpected Status
- Endpoint: `GET /api/dashboards`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `GET /api/dashboards`
- Description: Endpoint not implemented yet or auth headers not forwarded to engine-data

## Missing Feature (informational)
- Endpoint: `POST /api/dashboards/{id}/data`
- Description: No dashboard_id available to test data query
