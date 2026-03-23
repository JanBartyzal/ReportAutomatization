# TASK: Step03 — Atomizer PPTX (FS03)

## Goal

Verify the PPTX atomizer pipeline: processing status tracking, extracted slide structure, individual slide content, and slide image artifacts.

## Test Steps

1. **Load state** — tokens and file_ids from previous steps.
2. **Check PPTX processing status** — `GET /api/v1/files/{pptx_file_id}/status` returns status field.
3. **Get extracted structure** — `GET /api/v1/files/{pptx_file_id}/structure` returns slides array.
4. **Get slide content** — `GET /api/v1/files/{pptx_file_id}/slides/1` returns texts/tables.
5. **Get slide image** — `GET /api/v1/files/{pptx_file_id}/slides/1/image` returns artifact_url or binary.
6. If endpoints not implemented, use `missing_feature()`.

## API Endpoints

- `GET /api/v1/files/{file_id}/status` — processing status
- `GET /api/v1/files/{file_id}/structure` — extracted slide structure
- `GET /api/v1/files/{file_id}/slides/{n}` — slide content
- `GET /api/v1/files/{file_id}/slides/{n}/image` — slide image

## Expected Results

- Processing status endpoint returns a status/state field
- Structure endpoint returns a list of slides
- Slide content includes text and/or table data
- Slide image returns a URL or binary image data
- Missing endpoints are logged as informational (not failures)
