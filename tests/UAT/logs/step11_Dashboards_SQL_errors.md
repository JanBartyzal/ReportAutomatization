# UAT Errors - step11_Dashboards_SQL

Timestamp: 2026-03-26T18:32:05

[FAIL] Created dashboard 39f3b62a-08f8-42ed-845e-2fe5c2beca6f found in list
## Assertion Failed
- Message: Created dashboard 39f3b62a-08f8-42ed-845e-2fe5c2beca6f found in list

[FAIL] Viewer can see public dashboard
## Assertion Failed
- Message: Viewer can see public dashboard

[FAIL] Dashboard data contains widget results
## Assertion Failed
- Message: Dashboard data contains widget results

[FAIL] Dashboard data includes 5 project rows
## Assertion Failed
- Message: Dashboard data includes 5 project rows

[FAIL] Expected 200, got 400 for POST dashboard-data-query
## Unexpected Status
- Endpoint: `POST /api/dashboards/39f3b62a-08f8-42ed-845e-2fe5c2beca6f/data`
- Expected: 200
- Got: 400
- Body: `<binary 304 bytes, content-type=application/problem+json>`
