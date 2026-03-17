# API Documentation

## Microservice OpenAPI Specs (Current)

These YAML files define the REST API contracts for all frontend-facing services:

| File | Service | Description |
|------|---------|-------------|
| ms-auth-openapi.yaml | engine-core:auth | Authentication & user context |
| ms-ing-openapi.yaml | engine-ingestor | File upload & management |
| ms-qry-openapi.yaml | engine-data:query | Query API (CQRS read model) |
| ms-dash-openapi.yaml | engine-data:dashboard | Dashboard aggregation |
| ms-admin-openapi.yaml | engine-core:admin | Admin operations |
| ms-lifecycle-openapi.yaml | engine-reporting:lifecycle | Report lifecycle |
| ms-form-openapi.yaml | engine-reporting:form | Form builder & data collection |
| ms-period-openapi.yaml | engine-reporting:period | Reporting period management |
| ms-notif-openapi.yaml | engine-reporting:notification | Notifications |
| ms-ver-openapi.yaml | engine-core:versioning | Data versioning |
| ms-audit-openapi.yaml | engine-core:audit | Audit & compliance |
| ms-srch-openapi.yaml | engine-data:search | Search |

## Legacy Documentation (*.md files)

The `.md` files in this directory are from a prior monolithic API design and are kept for reference only. The YAML OpenAPI specs above are the authoritative contracts for the current microservices architecture.
