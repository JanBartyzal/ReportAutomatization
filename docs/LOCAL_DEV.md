# Local Development Quickstart

## Prerequisites

- **Docker Desktop** (v4.x+) with at least 8GB RAM allocated
- **Node.js** 20+ (for frontend and tests)
- **Tilt** (optional, for enhanced local dev): [install guide](https://docs.tilt.dev/install.html)
- **k6** (optional, for performance tests): [install guide](https://k6.io/docs/get-started/installation/)
- **Java 21** + Maven (if building Java services locally)
- **Python 3.12+** (if building Python services locally)

## Quick Start (Docker Compose)

```bash
# 1. Navigate to docker infrastructure
cd infra/docker

# 2. Copy environment template
cp .env.example .env

# 3. Start all services
docker compose up -d

# 4. Wait for services to be healthy (~2-3 minutes)
docker compose ps

# 5. Access the application
open http://localhost        # Frontend (via Nginx gateway)
open http://localhost:5173   # Frontend (direct Vite dev server)
```

## Quick Start (Tilt)

```bash
# 1. From project root
./scripts/dev-start.sh

# 2. Open Tilt UI
open http://localhost:10350

# 3. Stop everything
./scripts/dev-stop.sh
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Nginx Gateway | 80 | http://localhost |
| Frontend (Vite) | 5173 | http://localhost:5173 |
| ms-auth | 8081 | http://localhost:8081/actuator/health |
| ms-ing | 8082 | http://localhost:8082/actuator/health |
| ms-orch | 8083 | http://localhost:8083/actuator/health |
| ms-scan | 8084 | http://localhost:8084/actuator/health |
| ms-sink-tbl | 8085 | http://localhost:8085/actuator/health |
| ms-sink-doc | 8086 | http://localhost:8086/actuator/health |
| ms-sink-log | 8087 | http://localhost:8087/actuator/health |
| ms-atm-pptx | 8088 | http://localhost:8088/health |
| ms-atm-xls | 8089 | http://localhost:8089/health |
| ms-qry | 8091 | http://localhost:8091/actuator/health |
| ms-dash | 8092 | http://localhost:8092/actuator/health |
| PostgreSQL | 5432 | `psql -h localhost -U postgres -d reportplatform` |
| Redis | 6379 | `redis-cli -h localhost -a redis_pass` |
| Azurite | 10000 | http://localhost:10000 |
| ClamAV | 3310 | TCP |
| Dapr | 3500 | http://localhost:3500/v1.0/healthz |
| LiteLLM | 4000 | http://localhost:4000/health |
| MailHog | 8025 | http://localhost:8025 |

## Debug Mode

Start with debug ports enabled (Java JDWP, Python debugger):

```bash
cd infra/docker
docker compose -f docker-compose.yml -f docker-compose.override.yml up -d
```

Debug ports: ms-auth=5005, ms-ing=5006, ms-orch=5007, ms-scan=5008, ms-sink-tbl=5009, ms-sink-doc=5010, ms-sink-log=5011, ms-atm-pptx=5678

## Observability Stack

Start with full observability (Prometheus, Grafana, Loki, Tempo):

```bash
cd infra/docker
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

| Service | Port | URL |
|---------|------|-----|
| Grafana | 3000 | http://localhost:3000 (admin/admin) |
| Prometheus | 9090 | http://localhost:9090 |
| Loki | 3100 | http://localhost:3100 |
| Tempo | 3200 | http://localhost:3200 |
| OTEL Collector | 4317/4318 | gRPC/HTTP |

## Running Tests

### E2E Tests (Playwright)

```bash
cd tests/e2e
npm install
npx playwright install --with-deps
npx playwright test           # Run all tests
npx playwright test --headed  # Run with browser visible
npx playwright test --ui      # Interactive UI mode
```

### API Integration Tests

```bash
cd tests/integration
npm install
npx vitest run                # Run all tests
npx vitest run --reporter=verbose  # Verbose output
```

### Performance Tests (k6)

```bash
cd tests/performance
k6 run scripts/query-latency.js                    # Query latency test
k6 run scripts/upload-throughput.js                 # Upload throughput test
k6 run scripts/dashboard-aggregation.js             # Dashboard aggregation
k6 run --vus 1 --duration 10s scripts/query-latency.js  # Smoke test
bash run-all.sh                                     # Run all with reports
```

## Troubleshooting

### Services not starting
```bash
# Check service logs
docker compose logs ms-auth
docker compose logs postgres

# Restart a specific service
docker compose restart ms-ing

# Full reset (removes data volumes)
docker compose down -v
docker compose up -d
```

### Port conflicts
If ports are already in use, check:
```bash
# Linux/Mac
lsof -i :8081
# Windows
netstat -ano | findstr :8081
```

### Database issues
```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d reportplatform

# Check databases
\l

# Check if init scripts ran
docker logs postgres 2>&1 | grep "init"
```

### Frontend not loading
```bash
# Check Vite dev server
docker logs ms-fe

# Check if Nginx is routing correctly
curl -v http://localhost/
```
