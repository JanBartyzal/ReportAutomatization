# UAT Errors - step23_ServiceNow_Integration

Timestamp: 2026-03-23T12:51:29

[FAIL] Connection error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))
## Connection Error
- Endpoint: `GET /api/admin/integrations/servicenow`
- Error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))

[FAIL] Connection error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))
## Connection Error
- Endpoint: `POST /api/admin/integrations/servicenow`
- Error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))

## Missing Feature (informational)
- Endpoint: `POST /api/admin/integrations/servicenow`
- Description: Configure ServiceNow connection

[FAIL] Connection error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow/test (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))
## Connection Error
- Endpoint: `POST /api/admin/integrations/servicenow/test`
- Error: HTTPConnectionPool(host='localhost', port=8107): Max retries exceeded with url: /api/admin/integrations/servicenow/test (Caused by NewConnectionError("HTTPConnection(host='localhost', port=8107): Failed to establish a new connection: [WinError 10061] No connection could be made because the target machine actively refused it"))

## Missing Feature (informational)
- Endpoint: `POST /api/admin/integrations/servicenow/test`
- Description: Test ServiceNow connection

## Missing Feature (informational)
- Endpoint: `POST /api/admin/integrations/servicenow/{id}/sync`
- Description: No connection ID available to test sync

## Missing Feature (informational)
- Endpoint: `POST /api/admin/integrations/servicenow/{connId}/schedules`
- Description: No connection ID available to test scheduling

## Missing Feature (informational)
- Endpoint: `POST /api/admin/integrations/servicenow/{connId}/distributions`
- Description: No connection ID available to test distribution
