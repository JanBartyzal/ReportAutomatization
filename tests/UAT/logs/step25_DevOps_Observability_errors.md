# UAT Errors - step25_DevOps_Observability

Timestamp: 2026-03-23T12:51:30

[FAIL] Expected 200, got 500 for GET log-level-management
## Unexpected Status
- Endpoint: `GET /actuator/loggers`
- Expected: 200
- Got: 500
- Body: `<binary 138 bytes, content-type=application/problem+json>`

## Missing Feature (informational)
- Endpoint: `GET /actuator/loggers`
- Description: Endpoint may not be exposed (404/500)
