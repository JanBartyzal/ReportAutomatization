# Admin API

Administrative and internal system endpoints.

## Get All Statistics

`GET /api/admin/all-stats`

Retrieve system-wide statistics. This endpoint is restricted to admin users.

### Parameters
None.

### Responses
- **200 OK**: Returns statistics summary.
- **403 Forbidden**: If the user is not an administrator.

### Authentication
Required (`verify_admin` dependency).

---

## Read Items (Auth Test)

`GET /api/admin/items`

A test endpoint to verify authenticated access. Returns the information of the currently logged-in user.

### Parameters
None.

### Responses
- **200 OK**: Returns user dictionary.
- **401 Unauthorized**: If authentication fails.

### Authentication
Required (`get_current_user` dependency).
