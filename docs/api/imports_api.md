# Import API

Endpoints for uploading and listing files.

## Upload PPTX

`POST /api/import/upload`

Upload a PowerPoint (.pptx) file. The system performs MD5 deduplication and stores the file with a hash suffix.

### Request
- **Body**: Multi-part form data containing the `file`.

### Responses
- **200 OK**: Message confirming success and the associated user ID.
- **401 Unauthorized**: If authentication fails.

### Authentication
Required.

---

## Upload OPEX

`POST /api/import/uploadopex`

Upload an OPEX (.xlsx) file. Similar to the regular upload but optimized for operational expenditure data.

### Request
- **Body**: Multi-part form data containing the `file`.

### Responses
- **200 OK**: Message confirming success and the associated user email.

### Authentication
Required.

---

## List Uploaded Files

`GET /api/import/get-list-uploaded-files`

Retrieve a list of all files uploaded by the current user.

### Responses
- **200 OK**: Returns a list of file metadata (ID, filename, MD5 hash, region, creation date).

### Authentication
Required. Implements Row Level Security (RLS) - returns only the user's files.
