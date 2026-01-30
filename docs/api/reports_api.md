# Reports API

Endpoints for generating and managing user reports.

## Generate Report

`POST /api/report/generate-report`

Trigger creation of a final report from processed data.

### Request Body
- `file_data` (object): Dictionary containing the data to be used in report generation.

### Responses
- **200 OK**: Status message and user ID.

### Authentication
Required. Data is associated with the user via id for RLS.

---

## My Reports

`GET /api/report/my-reports`

List all reports owned by the authenticated user.

### Responses
- **200 OK**: List of report objects (ID, title, owner).

### Authentication
Required. Implements Row Level Security (RLS) - users can only see their own reports.
