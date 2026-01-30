# Task: Frontend Critical Auth Fixes
**Priority:** 🔴 HIGH / CRITICAL
**Required Agent:** Senior Frontend Developer
**Estimated Effort:** Fast (1-2 hours)

## Context
The Gap Analysis identified a critical mismatch in how the application requests Access Tokens for the API. Currently, the `axios` interceptor requests a token using `loginRequest` scopes, which typically target the user (e.g., `User.Read` or client-id based), whereas the Backend API requires a token with a specific resource scope (e.g., `api://<client-id>/user_impersonation`). This will result in 401 Unauthorized errors even if the user is logged in.
Additionally, the configuration file contains "fail-open" defaults with hardcoded placeholders, which is a security and configuration risk.

## Objectives
1.  **Fix Auth Scopes in Axios Interceptor:** Ensure `acquireTokenSilent` uses the correct `tokenRequest` scope defined in `authConfig.ts`.
2.  **Secure Configuration:** Remove unsafe default values ("YOUR_CLIENT_ID") from `authConfig.ts`.

## Step-by-Step Instructions

### 1. Update `frontend/src/api/axios.ts`
*   **Goal:** Use `tokenRequest` instead of `loginRequest` for the `acquireTokenSilent` call.
*   **Action:**
    *   Update imports to include `tokenRequest` from `../authConfig`.
    *   In the interceptor logic (approx line 39), change:
        ```typescript
        // OLD
        const response = await msalInstance.acquireTokenSilent({
            ...loginRequest,
            account: account
        });
        
        // NEW
        const response = await msalInstance.acquireTokenSilent({
            ...tokenRequest,
            account: account
        });
        ```
    *   Verify that `tokenRequest` in `authConfig.ts` correctly points to `api://${clientId}/user_impersonation`.

### 2. Sanitize `frontend/src/authConfig.ts`
*   **Goal:** Force application failure if sensitive configuration is missing, rather than using invalid placeholders.
*   **Action:**
    *   Remove string fallbacks for Client ID and Tenant ID.
    *   Example:
        ```typescript
        // OLD
        const clientId = import.meta.env.VITE_AZURE_CLIENT_ID || "YOUR_CLIENT_ID";
        
        // NEW
        const clientId = import.meta.env.VITE_AZURE_CLIENT_ID;
        if (!clientId) {
            throw new Error("Missing VITE_AZURE_CLIENT_ID in environment variables.");
        }
        ```
    *   Apply this logic for `VITE_AZURE_TENANT_ID` as well.
    *   `VITE_AZURE_REDIRECT_URI` can keep a dynamic default (`window.location.origin`), but ensure it's logged correctly.

## Definition of Done
*   [ ] `axios.ts` imports and uses `tokenRequest`.
*   [ ] `authConfig.ts` throws explicit errors if Environment Variables are missing.
*   [ ] No "YOUR_CLIENT_ID" string exists in the codebase.
*   [ ] Build passes `npm run build`.
