# Task: Implement Admin Dashboard Statistics

**Role:** Backend Developer (Python/FastAPI)
**Context:** The Admin Dashboard currently displays dummy data or hidden tiles because the backend endpoint `/api/admin/all-stats` returns a static message.

## Objective
Implement the logic for `GET /api/admin/all-stats` to return real system statistics.

## Input
- `backend/app/routers/admin.py` (Existing endpoint stubs)
- `backend/app/core/models.py` (Database models)

## Requirements
Modify `admin_stats` function in `admin.py` to calculate and return:
1.  **total_users**: Count of users in DB (or Mock if using Entra ID without local user table duplication, might need to query Graph API or just count unique OIDs in `UploadFile` table). -> *Decision: Count unique `ids` from `User` table if it exists, otherwise `UploadFile.oid` distinct count.*
2.  **total_files**: Count of rows in `UploadFile`.
3.  **total_storage**: Sum of file sizes (requires storing file size in DB, or traversing `upload_dir`. *Easier: traverse `settings.upload_dir` and sum bytes*).
4.  **system_health**: Simple check (DB is connected = "Healthy").
5.  **avg_processing_time**: (Optional) Average time from Batch creation to Completion.

## Output Structure (JSON)
```json
{
  "total_users": 150,
  "total_files": 1240,
  "total_storage": "4.5 GB",
  "system_health": "Healthy",
  "active_jobs": 2
}
```

## Definition of Done
-   [ ] `GET /api/admin/all-stats` returns the JSON structure above with dynamic data.
-   [ ] Values are calculated efficiently (caching recommended for storage/counts if slow).
-   [ ] Unit test verifies the structure matches.
