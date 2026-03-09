# P5 – Wave 2: Monitoring & Alerting (Sonnet)

**Phase:** P5 – DevOps Maturity & Onboarding
**Agent:** Sonnet
**Complexity:** Medium
**Total Effort:** ~7 MD

---

## P5-W2-001: Prometheus Metrics & Grafana Dashboards

**Type:** Infrastructure
**Effort:** 4 MD

**Tasks:**
- [ ] **Prometheus** scrape config for all services:
  - Spring Boot Actuator metrics (JVM, HTTP, DB pool)
  - FastAPI metrics (request count, latency, errors)
  - Custom metrics: workflow queue depth, atomizer processing time
- [ ] **Grafana Dashboards**:
  - System overview: all services health, request rate, error rate
  - Orchestrator: workflow queue, step latencies, DLQ depth
  - Atomizer: per-type processing time, success/failure ratio
  - Database: connection pool utilization, query latency
  - Upload: file size distribution, upload rate, rejection rate
- [ ] **Alerting Rules**:
  - Service down > 1 min
  - Error rate > 5%
  - DLQ depth > 10
  - DB connection pool > 80%
  - Upload latency > 10s

---

## P5-W2-002: Centralized Logging

**Type:** Infrastructure
**Effort:** 3 MD

**Tasks:**
- [ ] **Loki** deployment (or ELK stack)
- [ ] Log collection from all containers
- [ ] Structured JSON logging format standardized:
  - `timestamp`, `level`, `service`, `trace_id`, `user_id`, `org_id`, `message`, `extra`
- [ ] Grafana log exploration (filter by service, trace_id, level)
- [ ] Log retention policy: 30 days hot, 90 days cold
- [ ] Correlation: click trace_id in logs → jump to Jaeger trace
