# Dapr Service Registry

## Service Configuration

| Service | Dapr app-id | Protocol | Port | Description |
|---------|------------|----------|------|-------------|
| MS-AUTH | `ms-auth` | http | 8000 | Auth token validation |
| MS-ING | `ms-ing` | http | 8000 | File ingestor |
| MS-ORCH | `ms-orch` | grpc | 50051 | Workflow orchestrator |
| MS-ATM-PPTX | `ms-atm-pptx` | grpc | 50051 | PPTX atomizer |
| MS-ATM-XLS | `ms-atm-xls` | grpc | 50051 | Excel atomizer |
| MS-ATM-PDF | `ms-atm-pdf` | grpc | 50051 | PDF/OCR atomizer |
| MS-ATM-CSV | `ms-atm-csv` | grpc | 50051 | CSV atomizer |
| MS-ATM-AI | `ms-atm-ai` | grpc | 50051 | AI gateway |
| MS-SINK-TBL | `ms-sink-tbl` | grpc | 50051 | Table data sink |
| MS-SINK-DOC | `ms-sink-doc` | grpc | 50051 | Document sink |
| MS-SINK-LOG | `ms-sink-log` | grpc | 50051 | Log sink |
| MS-TMPL | `ms-tmpl` | grpc | 50051 | Template & schema mapping |
| MS-SCAN | `ms-scan` | grpc | 50051 | Security scanner |
| MS-NOTIF | `ms-notif` | grpc | 50051 | Notification center |
| MS-QRY | `ms-qry` | http | 8080 | Query API (read) |
| MS-DASH | `ms-dash` | http | 8080 | Dashboard aggregation |
| MS-LIFECYCLE | `ms-lifecycle` | http | 8080 | Report lifecycle |
| MS-GEN-PPTX | `ms-gen-pptx` | grpc | 50051 | PPTX generator |

## Protocol Rules

- **gRPC services**: Internal-only, called via Dapr service invocation
- **HTTP services**: Edge-facing, exposed through API Gateway (Nginx)
- See `access-control.yaml` for invocation policies
