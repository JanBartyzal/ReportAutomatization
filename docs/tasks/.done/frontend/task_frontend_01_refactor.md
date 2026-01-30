# Task: Frontend Auth Fix & Structure Refactor (High Priority)

**Role:** Frontend Developer (React/TypeScript)
**Context:** Gap Analysis identified technical debt in Auth Config and Folder Structure.

## Objective
Clean up `authConfig.ts` to prevent potential Auth failures and standardize the project structure.

## Input
- `frontend/src/authConfig.ts`
- `frontend/src/pages/*`

## Steps
1.  **Refactor `authConfig.ts`**
    -   **Remove Duplicate Code:** Delete the commented-out or duplicate `loginRequest` export.
    -   **Standardize Scopes:** Ensure `loginRequest` and `tokenRequest` use the correct `api://` prefix if required by the Backend/Azure.
    -   Add comments explaining *why* specific scopes are used.

2.  **Dashboard Stats Implementation**
    -   Remove hardcoded placeholders or wire them up to valid API endpoints if they exist.
    -   If endpoints don't exist, wrap them in a conditional check `if (featureEnabled)` or remove/hide them to avoid confusing users.

3.  **Folder Structure Standardization (Refactoring)**
    -   Move root pages (`Dashboard.tsx`, `Analytics.tsx`, `Admin.tsx`) into dedicated folders:
        -   `src/pages/dashboard/Dashboard.tsx`
        -   `src/pages/analytics/Analytics.tsx`
        -   `src/pages/admin/Admin.tsx`
    -   Update `App.tsx` imports.

## Definition of Done
-   [ ] `authConfig.ts` is clean, no dead code, single export of config objects.
-   [ ] Folder structure is consistent (all pages in subfolders).
-   [ ] Application builds and runs without lint errors.
