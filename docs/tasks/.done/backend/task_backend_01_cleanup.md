# Task: Backend Cleanup & Auth Verification (High Priority)

**Role:** Senior Backend Developer (Python/FastAPI)
**Context:** Result of Gap Analysis Audit (Status: Risk in Auth Config).

## Objective
Verify authentication scopes and ensure backend routers are correctly implementing the required CRUD operations for Dashboard stats.

## Input
- `docs/gap_analysis_report.md`
- `frontend/src/auth/authConfig.ts` (Reference for scopes)
- `backend/app/core/auth.py` (or equivalent auth config)

## Steps
1.  **Auth Scope Verification**
    -   Check `backend/app/core/config.py` (or auth module) to see what Audience/Scope is expected by `fastapi-azure-auth`.
    -   Ensure it matches `api://<client_id>/user_impersonation` (or whatever is correctly configured in Azure).
    -   Update `docs/project_defaults.md` if the default scope differs.

2.  **Dashboard Stats Endpoints**
    -   Check if endpoints exist for "Avg Processing Time" and "Regions Covered" (referenced in `Dashboard.tsx`).
    -   If missing, create a plan to implement them in `analytics.py` or `admin.py`.
    -   If not planned, mark them as "Future" in `gap_analysis_report.md`.

## Definition of Done
-   [ ] Auth Scopes are verified and consistent between BE/FE.
-   [ ] Dashboard endpoints status is clarified (Implemented or Backlog ticket created).
