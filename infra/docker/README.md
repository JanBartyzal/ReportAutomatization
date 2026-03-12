# ReportAutomatization P1 Infrastructure

This directory contains the Docker Compose configuration and infrastructure setup for the P1 MVP of the ReportAutomatization platform.

## Overview

The P1 topology includes:

### Microservices
| Service | Port | Description |
|---------|------|-------------|
| ms-gw (Nginx) | 80, 443 | API Gateway |
| ms-auth | 8081 | Authentication Service |
| ms-ing | 8082 | File Ingestion Service |
| ms-orch | 8083 | Workflow Orchestration |
| ms-scan | 8084 | Security Scanner |
| ms-sink-tbl | 8085 | Table Data Sink |
| ms-sink-doc | 8086 | Document Sink |
| ms-sink-log | 8087 | Log Sink |
| ms-atm-pptx | 8088 | PPTX Processor |
| ms-fe | 5173 | Frontend (React) |

### Infrastructure
| Service | Port | Description |
|---------|------|-------------|
| postgres | 5432 | PostgreSQL 16 with pgVector |
| redis | 6379 | Redis for caching/state |
| azurite | 10000 | Azure Blob Storage emulator |
| clamav | 3310 | Virus Scanner |
| dapr | 3500, 50001 | Dapr sidecar service |

## Quick Start

### Prerequisites
- Docker 24.0+
- Docker Compose 2.20+

### Start All Services

```bash
# Navigate to this directory
cd infra/docker

# Copy environment template
cp .env.example .env

# Start all services
docker compose up -d

# Check status
docker compose ps
```

### Start with Debug Ports

```bash
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d
```

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f ms-auth

# Follow with service name
docker compose logs -f --tail=100 ms-orch
```

### Stop Services

```bash
docker compose down
# Remove volumes
docker compose down -v
```

## Service Health Checks

All services have health check endpoints:

| Service | Health Endpoint |
|---------|-----------------|
| ms-gw | http://localhost/health |
| ms-auth | http://localhost:8081/actuator/health |
| ms-ing | http://localhost:8082/actuator/health |
| ms-orch | http://localhost:8083/actuator/health |
| ms-scan | http://localhost:8084/actuator/health |
| ms-sink-tbl | http://localhost:8085/actuator/health |
| ms-sink-doc | http://localhost:8086/actuator/health |
| ms-sink-log | http://localhost:8087/actuator/health |
| ms-atm-pptx | http://localhost:8088/health |
| ms-fe | http://localhost:5173 |

## Dapr

Dapr dashboard is available at http://localhost:8080 (from dapr service).

### Dapr Components
- **Pub/Sub**: Redis Streams (`reportplatform-pubsub`)
- **State Store**: Redis (`reportplatform-statestore`)

## Database

### PostgreSQL
- Host: localhost
- Port: 5432
- Database: reportplatform
- User: postgres
- Password: postgres (default, change in production)

### Create Database Users

The services use separate database users. Run the following in PostgreSQL:

```sql
-- Create users for each service
CREATE USER auth_user WITH PASSWORD 'auth_pass';
CREATE DATABASE auth_db OWNER auth_user;

CREATE USER ms_ing WITH PASSWORD 'ms_ing_pass';
CREATE DATABASE ms_ing OWNER ms_ing;

CREATE USER orch_user WITH PASSWORD 'orch_pass';
CREATE DATABASE orch_db OWNER orch_user;

-- And so on for other services...
```

### Flyway Migrations

Each service has its own Flyway migrations in `src/main/resources/db/migration/`.
Migrations run automatically on service startup.

## Development

### Rebuild a Service

```bash
docker compose build ms-auth
docker compose up -d ms-auth
```

### Access Container Shell

```bash
docker exec -it ms-auth /bin/bash
```

### View Service Logs

```bash
# Real-time logs
docker compose logs -f ms-orch

# Last 100 lines
docker compose logs --tail=100 ms-orch
```

## Environment Variables

See `.env.example` for all configurable variables.

Key variables:
- `REDIS_PASSWORD`: Redis password
- `AZURE_STORAGE_CONNECTION_STRING`: Blob storage connection
- `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`: Azure Entra ID config

## Network

All services communicate on the `reportnet` Docker network.

### Service Communication
- Services use Docker DNS for service discovery
- Example: `ms-auth` can reach `postgres` at `postgres:5432`

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker compose logs <service-name>

# Check health
docker inspect <container-name> | grep -A 20 Health
```

### Database Connection Issues

```bash
# Check PostgreSQL
docker exec -it postgres pg_isready -U postgres

# Connect to database
docker exec -it postgres psql -U postgres -d reportplatform
```

### Port Conflicts

If a port is already in use, modify the port mapping in `docker-compose.yml` or use the override file.

## Production Considerations

1. **Security**: Change all default passwords
2. **HTTPS**: Configure SSL/TLS certificates in nginx.conf
3. **Secrets**: Use Docker secrets or external secret management
4. **Monitoring**: Add Prometheus/Grafana
5. **Backup**: Set up database backups
6. **Logging**: Use centralized logging (ELK, Loki)
