# TASK: Step04 — Orchestrator Workflow (FS04)

## Goal

Verify the orchestrator workflow engine: workflow status tracking, step inspection, idempotency, and failed jobs listing.

## Test Steps

1. **Load state** — tokens and file_ids from previous steps.
2. **Check workflow status** — `GET /api/v1/workflows/{file_id}/status` returns workflow state.
3. **Check workflow steps** — `GET /api/v1/workflows/{file_id}/steps` returns list of steps with status.
4. **Check idempotency** — re-trigger same file processing, should not duplicate workflows.
5. **Check failed jobs** — `GET /api/v1/admin/failed-jobs` returns 200 with a list.
6. If endpoints not implemented, use `missing_feature()`.

## API Endpoints

- `GET /api/v1/workflows/{file_id}/status` — workflow status
- `GET /api/v1/workflows/{file_id}/steps` — workflow step listing
- `POST /api/v1/workflows/{file_id}/trigger` — re-trigger workflow (idempotency test)
- `GET /api/v1/admin/failed-jobs` — admin failed jobs listing

## Expected Results

- Workflow status endpoint returns state field
- Workflow steps endpoint returns a list of steps
- Re-triggering does not create duplicate workflows
- Failed jobs endpoint is accessible to admin and returns a list
- Missing endpoints are logged as informational (not failures)
