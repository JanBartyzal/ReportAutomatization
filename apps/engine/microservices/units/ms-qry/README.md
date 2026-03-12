# MS-QRY - Query API

CQRS read model microservice for the Report Platform. Provides REST endpoints for querying parsed tables, documents, processing logs, and file summaries.

## Architecture

```mermaid
graph LR
    FE[Frontend] -->|GET /api/query/files/{id}/data| QRY[MS-QRY]
    FE -->|GET /api/query/tables| QRY
    FE -->|GET /api/query/documents/{id}| QRY
    FE -->|GET /api/query/processing-logs/{id}| QRY
    QRY -->|Check cache| Redis[(Redis)]
    QRY -->|Query if cache miss| PG[(PostgreSQL)]
    Dapr[Dapr Pub/Sub] -->|data-stored event| QRY
    QRY -->|Invalidate cache| Redis
    QRY -->|Refresh views| PG
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/query/files/{file_id}/data` | All parsed data (tables + documents) for a file |
| GET | `/api/query/files/{file_id}/slides` | Slide content with image URLs |
| GET | `/api/query/tables` | Paginated table data query with filters |
| GET | `/api/query/documents/{document_id}` | Single document by ID |
| GET | `/api/query/processing-logs/{file_id}` | Processing step timeline |

### Required Headers

- `X-Org-Id` - Organization UUID (required, used for RLS)
- `X-User-Id` - User UUID (required)

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| DB_HOST | localhost | PostgreSQL host |
| DB_PORT | 5432 | PostgreSQL port |
| DB_NAME | reportplatform | Database name |
| DB_USERNAME | ms_qry | Database user |
| DB_PASSWORD | ms_qry_pass | Database password |
| REDIS_HOST | localhost | Redis host |
| REDIS_PORT | 6379 | Redis port |
| REDIS_PASSWORD | redis_pass | Redis password |
| DAPR_PUBSUB_NAME | reportplatform-pubsub | Dapr pub/sub component name |

## Running Locally

```bash
mvn spring-boot:run
```

## Building Docker Image

From the repository root:
```bash
docker build -f apps/engine/microservices/units/ms-qry/Dockerfile -t ms-qry .
```
