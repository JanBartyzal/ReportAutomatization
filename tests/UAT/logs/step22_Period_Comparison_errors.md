# UAT Errors - step22_Period_Comparison

Timestamp: 2026-03-23T12:51:17

[FAIL] Expected 200, got 400 for POST basic-period-comparison
## Unexpected Status
- Endpoint: `POST /api/periods/compare`
- Expected: 200
- Got: 400
- Body: `<binary 192 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/periods/compare`
- Description: Basic period-over-period comparison

[FAIL] Expected 200, got 403 for POST kpi-comparison-post
## Unexpected Status
- Endpoint: `POST /api/comparisons/kpis`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/comparisons/kpis`
- Description: KPI comparison between periods

[FAIL] Expected 200, got 403 for GET kpi-comparison-get
## Unexpected Status
- Endpoint: `GET /api/comparisons/kpis`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `GET /api/comparisons/kpis`
- Description: KPI comparison listing

[FAIL] Expected 200, got 403 for POST multi-org-comparison
## Unexpected Status
- Endpoint: `POST /api/comparisons/multi-org`
- Expected: 200
- Got: 403
- Body: `<binary 0 bytes, content-type=>`

## Missing Feature (informational)
- Endpoint: `POST /api/comparisons/multi-org`
- Description: Multi-org comparison across organizations

[FAIL] Expected 200, got 500 for POST export-comparison-pptx
## Unexpected Status
- Endpoint: `POST /api/periods/compare/export`
- Expected: 200
- Got: 500
- Body: `<binary 141 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `POST /api/periods/compare/export`
- Description: Export period comparison as PPTX
