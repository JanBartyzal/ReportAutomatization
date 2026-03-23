# TASK: Step02 — File Upload (FS02)

## Goal

Verify file upload functionality: valid PPTX/XLSX uploads succeed, invalid file types are rejected, unauthenticated uploads are denied, and uploaded file metadata is retrievable.

## Test Steps

1. **Load tokens** from shared state (step00).
2. **Upload valid PPTX** — `POST /api/v1/upload` (multipart) with a minimal PPTX blob. Expect 200/201. Save file_id.
3. **Upload valid XLSX** — `POST /api/v1/upload` (multipart) with a minimal XLSX blob. Expect 200/201. Save file_id.
4. **Upload invalid file type (.exe)** — `POST /api/v1/upload` with .exe file. Expect 415.
5. **Upload without auth** — `POST /api/v1/upload` without token. Expect 401.
6. **Check uploaded file metadata** — `GET /api/v1/files/{file_id}` returns 200 with filename, mimeType, status.
7. **Save file_ids** to state for subsequent steps.

## API Endpoints

- `POST /api/v1/upload` — multipart file upload (expected 200/201)
- `GET /api/v1/files/{file_id}` — file metadata (expected 200)

## Expected Results

- Valid PPTX and XLSX uploads return 200/201 with file_id
- Invalid file type (.exe) is rejected with 415
- Unauthenticated upload returns 401
- File metadata includes filename, mimeType, and status fields
- file_ids are persisted in state for downstream steps
