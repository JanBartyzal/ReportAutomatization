# Health API

Endpoints for monitoring the service status.

## Health Check

`GET /api/health`

Returns the current status, service name, and version of the API.

### Response
```json
{
  "status": "ok",
  "service": "report-automation",
  "version": "2.0.0"
}
```

### Authentication
None required.
