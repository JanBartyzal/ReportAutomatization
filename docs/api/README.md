# API Documentation

## Microservice OpenAPI Specs (Current)

These YAML files define the REST API contracts for all frontend-facing services:

| File | Service | Description |
|------|---------|-------------|
| ms-auth-openapi.yaml | MS-AUTH | Authentication & user context |
| ms-ing-openapi.yaml | MS-ING | File upload & management |
| ms-qry-openapi.yaml | MS-QRY | Query API (CQRS read model) |
| ms-dash-openapi.yaml | MS-DASH | Dashboard aggregation |
| ms-admin-openapi.yaml | MS-ADMIN | Admin operations |
| ms-lifecycle-openapi.yaml | MS-LIFECYCLE | Report lifecycle |
| ms-form-openapi.yaml | MS-FORM | Form builder & data collection |
| ms-period-openapi.yaml | MS-PERIOD | Reporting period management |
| ms-notif-openapi.yaml | MS-NOTIF | Notifications |
| ms-ver-openapi.yaml | MS-VER | Data versioning |
| ms-audit-openapi.yaml | MS-AUDIT | Audit & compliance |
| ms-srch-openapi.yaml | MS-SRCH | Search |

## Legacy Documentation (*.md files)

The `.md` files in this directory are from a prior monolithic API design and are kept for reference only. The YAML OpenAPI specs above are the authoritative contracts for the current microservices architecture.
