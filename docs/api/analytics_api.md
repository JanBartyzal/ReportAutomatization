# Analytics API

Endpoints for template aggregation and schema detection across multiple files.

## Preview Aggregation

`POST /api/analytics/aggregate/preview`

Analyzes specified files to detect matching table schemas and provides statistics about potential aggregations.

### Request Body
```json
{
  "file_ids": ["string"]
}
```

### Responses
- **200 OK**: Returns a list of detected schemas with metadata.
- **400 Bad Request**: If no file IDs are provided.
- **401 Unauthorized**: If authentication fails.

### Authentication
Required. Implements Row Level Security (RLS) - only analyzes files owned by the user.

---

## Get Aggregated Data

`GET /api/analytics/aggregate/{schema_fingerprint}`

Retrieves aggregated data for a specific schema fingerprint by performing a virtual UNION ALL across all matching tables.

### Path Parameters
- `schema_fingerprint` (string): SHA-256 fingerprint of the schema (64 characters).

### Responses
- **200 OK**: Returns aggregated dataset with columns, rows, and source metadata.
- **400 Bad Request**: If fingerprint format is invalid.
- **404 Not Found**: If no data is found for the given fingerprint.

### Authentication
Required. Implements Row Level Security (RLS) - only aggregates data from the user's files.
