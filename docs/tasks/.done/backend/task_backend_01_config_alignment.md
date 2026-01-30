# Task: Backend Config Alignment
**Priority:** 🟢 LOW
**Required Agent:** Fast Agent
**Estimated Effort:** Fast (< 30 mins)

## Context
The `backend/app/core/config.py` file defines default CORS origins as `http://localhost:5173` (Vite Default). However, `docs/project_defaults.md` (and potential older assumptions) might mention port `3000` (React Default). While the code is actually correct for Vite, we should ensure the documentation and code alignment is explicit to avoid confusion for future agents or developers.

## Objectives
1.  **Verify & Update Config Defaults:** Ensure `config.py` explicitly supports both common ports if intended, or just the correct one.
2.  **Update Documentation (Optional):** If `project_defaults.md` is outdated regarding the frontend port, update it.

## Step-by-Step Instructions

### 1. Check `backend/app/core/config.py`
*   **Action:**
    *   Review `cors_origins_str` default value.
    *   Current: `"http://localhost:5173,http://127.0.0.1:5173"`
    *   Proposed Change: Add `http://localhost:3000` just in case, or leave it if we strictly strictly enforce Vite.
    *   **Decision:** Keep it strict to 5173 if that's the standard, but ensure the *Docstring* mentions this.

### 2. Check `docs/project_defaults.md`
*   **Action:**
    *   If the document says "Frontend: http://localhost:3000", change it to `http://localhost:5173` to match the actual Vite configuration.
    *   This prevents "Hallucination" where an Agent tries to run the app on 3000 because the docs said so, but the code is configured for 5173.

## Definition of Done
*   [x] `project_defaults.md` accurately reflects the Vite default port (5173).
*   [x] Backend config is consistent with the documentation.
