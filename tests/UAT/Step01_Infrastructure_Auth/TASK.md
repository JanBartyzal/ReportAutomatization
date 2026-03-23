# TASK: Step01 — Infrastructure & Auth (FS01)

## Goal

Verify platform health, authentication flow (login, token validation), and RBAC enforcement across admin, editor, and viewer roles.

## Test Steps

1. **Health check**: `GET /health` returns 200.
2. **Auth without token**: `GET /api/v1/auth/me` without Authorization header returns 401.
3. **Auth with invalid token**: `GET /api/v1/auth/me` with `Bearer invalid` returns 401 or 403.
4. **Login as admin1**: `POST /api/v1/auth/login` with admin1 credentials returns 200 + token.
5. **Auth with valid token**: `GET /api/v1/auth/me` with admin1 token returns 200, verify email and role fields.
6. **RBAC admin access**: admin1 can access `GET /api/v1/admin/organizations` (200).
7. **RBAC editor denied**: user1 (editor) cannot access `GET /api/v1/admin/organizations` (403).
8. **RBAC viewer denied**: user2 (viewer) cannot access `GET /api/v1/admin/organizations` (403).

## API Endpoints

- `GET /health` — health check (expected 200)
- `POST /api/v1/auth/login` — login (expected 200 with token)
- `GET /api/v1/auth/me` — current user info (expected 200 with valid token)
- `GET /api/v1/admin/organizations` — admin-only endpoint

## Expected Results

- Health endpoint returns 200
- Unauthenticated requests are rejected with 401
- Invalid tokens are rejected with 401 or 403
- Valid login returns JWT token
- /auth/me returns user email and role
- Admin endpoint is accessible only to admin role users
- Editor and viewer roles are denied access to admin endpoints (403)
