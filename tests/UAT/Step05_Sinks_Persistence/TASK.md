# TASK: Step05 — Sinks & Persistence (FS05)

## Goal

Verify data persistence layer: stored table data, document data, Row-Level Security (RLS) per organization, and cross-tenant isolation.

## Test Steps

1. **Load state** — tokens and file_ids from previous steps.
2. **Query stored table data** — `GET /api/v1/data/tables?file_id={pptx_file_id}` returns data.
3. **Query stored document data** — `GET /api/v1/data/documents?file_id={pptx_file_id}` returns data.
4. **RLS: admin1 sees own org data** — `GET /api/v1/data/tables` returns only org1 data.
5. **RLS: admin2 sees own org data** — login as admin2, `GET /api/v1/data/tables` returns only org2 data (or empty).
6. **Cross-tenant isolation** — admin2 cannot see admin1's file data.
7. If endpoints not implemented, use `missing_feature()`.

## API Endpoints

- `GET /api/v1/data/tables` — query stored table data (with optional file_id filter)
- `GET /api/v1/data/documents` — query stored document data (with optional file_id filter)

## Expected Results

- Table and document data endpoints return stored data for the uploaded file
- Admin1 sees only org1 data
- Admin2 sees only org2 data
- Admin2 cannot access admin1's file data (cross-tenant isolation)
- Missing endpoints are logged as informational (not failures)
