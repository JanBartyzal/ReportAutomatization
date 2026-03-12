# P8 Microservices Consolidation Guide

This document provides a comprehensive guide for migrating from the legacy microservices architecture (29 services) to the P8 consolidated architecture (8 deployment units).

## Architecture Overview

### Before (P7): 29 Individual Services
```
┌─────────────────────────────────────────────────────────────────────┐
│                         NGINX Gateway                                │
└─────────────────────────────────────────────────────────────────────┘
      │       │       │       │       │       │       │       │
   ms-auth  ms-ing  ms-orch ms-scan ms-sink-* ms-qry  ms-dash  ms-admin
   ms-batch ms-ver  ms-audit ms-lifecycle ms-period ms-form ms-notif
   ms-tmpl  ms-atm-* ms-gen-* ...
```

### After (P8): 8 Consolidated Units
```
┌─────────────────────────────────────────────────────────────────────┐
│                         NGINX Gateway                                │
└─────────────────────────────────────────────────────────────────────┘
      │       │       │       │       │       │       │
engine- engine- engine- engine- engine- engine- processor- processor-
 core   ingestor orchestrator scan    data    reporting integrations atomizers generators
```

## Service Mapping

### Java Units

| New Unit | Port | Includes (Legacy Services) |
|----------|------|---------------------------|
| engine-core | 8081 | ms-auth, ms-admin, ms-batch, ms-ver, ms-audit |
| engine-ingestor | 8082 | ms-ing |
| engine-orchestrator | 8083 | ms-orch |
| engine-scan | 8084 | ms-scan |
| engine-data | 8100 | ms-sink-tbl, ms-sink-doc, ms-sink-log, ms-qry, ms-dash, ms-srch, ms-tmpl |
| engine-reporting | 8105 | ms-lifecycle, ms-period, ms-form, ms-tmpl-pptx, ms-notif |
| engine-integrations | 8107 | ms-ext-snow |

### Python Units

| New Unit | Port | Includes (Legacy Services) |
|----------|------|---------------------------|
| processor-atomizers | 8088 | ms-atm-pptx, ms-atm-xls, ms-atm-pdf, ms-atm-csv, ms-atm-ai, ms-atm-cln |
| processor-generators | 8111 | ms-gen-pptx, ms-gen-xls, ms-mcp |

## Port Mapping Reference

### Legacy → Consolidated

| Legacy Service | Old Port | New Unit | New Port |
|---------------|----------|----------|----------|
| ms-auth | 8081 | engine-core | 8081 |
| ms-admin | 8095 | engine-core | 8081 |
| ms-batch | 8096 | engine-core | 8081 |
| ms-ver | 8089 | engine-core | 8081 |
| ms-audit | 8090 | engine-core | 8081 |
| ms-ing | 8082 | engine-ingestor | 8082 |
| ms-orch | 8083 | engine-orchestrator | 8083 |
| ms-scan | 8084 | engine-scan | 8084 |
| ms-sink-tbl | 8085 | engine-data | 8100 |
| ms-sink-doc | 8086 | engine-data | 8100 |
| ms-sink-log | 8087 | engine-data | 8100 |
| ms-qry | 8091 | engine-data | 8100 |
| ms-dash | 8092 | engine-data | 8100 |
| ms-srch | 8093 | engine-data | 8100 |
| ms-tmpl | 8094 | engine-data | 8100 |
| ms-lifecycle | 8097 | engine-reporting | 8105 |
| ms-period | 8098 | engine-reporting | 8105 |
| ms-form | 8099 | engine-reporting | 8105 |
| ms-notif | 8101 | engine-reporting | 8105 |
| ms-tmpl-pptx | 8102 | engine-reporting | 8105 |
| ms-ext-snow | 8107 | engine-integrations | 8107 |
| ms-atm-pptx | 8201 | processor-atomizers | 8088 |
| ms-atm-xls | 8202 | processor-atomizers | 8088 |
| ms-atm-pdf | 8203 | processor-atomizers | 8088 |
| ms-atm-csv | 8204 | processor-atomizers | 8088 |
| ms-atm-ai | 8205 | processor-atomizers | 8088 |
| ms-atm-cln | 8206 | processor-atomizers | 8088 |
| ms-gen-pptx | 8211 | processor-generators | 8111 |
| ms-gen-xls | 8212 | processor-generators | 8111 |
| ms-mcp | 8213 | processor-generators | 8111 |

## Dapr App-ID Mapping

| Legacy App ID | New App ID |
|---------------|------------|
| ms-auth | engine-core |
| ms-admin | engine-core |
| ms-batch | engine-core |
| ms-ver | engine-core |
| ms-audit | engine-core |
| ms-ing | engine-ingestor |
| ms-orch | engine-orchestrator |
| ms-scan | engine-scan |
| ms-sink-tbl | engine-data |
| ms-sink-doc | engine-data |
| ms-sink-log | engine-data |
| ms-qry | engine-data |
| ms-dash | engine-data |
| ms-srch | engine-data |
| ms-tmpl | engine-data |
| ms-lifecycle | engine-reporting |
| ms-period | engine-reporting |
| ms-form | engine-reporting |
| ms-notif | engine-reporting |
| ms-tmpl-pptx | engine-reporting |
| ms-ext-snow | engine-integrations |
| ms-atm-pptx | processor-atomizers |
| ms-atm-xls | processor-atomizers |
| ms-atm-pdf | processor-atomizers |
| ms-atm-csv | processor-atomizers |
| ms-atm-ai | processor-atomizers |
| ms-atm-cln | processor-atomizers |
| ms-gen-pptx | processor-generators |
| ms-gen-xls | processor-generators |
| ms-mcp | processor-generators |

## API Routing Changes

All frontend API calls go through Nginx which routes to the appropriate consolidated unit:

| Path Pattern | Legacy Service | New Unit |
|-------------|---------------|----------|
| /api/auth/* | ms-auth | engine-core |
| /api/admin/* | ms-admin | engine-core |
| /api/batch/* | ms-batch | engine-core |
| /api/versions/* | ms-ver | engine-core |
| /api/audit/* | ms-audit | engine-core |
| /api/upload/* | ms-ing | engine-ingestor |
| /api/orch/* | ms-orch | engine-orchestrator |
| /api/query/* | ms-qry | engine-data |
| /api/dashboards/* | ms-dash | engine-data |
| /api/search/* | ms-srch | engine-data |
| /api/templates/* | ms-tmpl | engine-data |
| /api/reports/* | ms-lifecycle | engine-reporting |
| /api/periods/* | ms-period | engine-reporting |
| /api/forms/* | ms-form | engine-reporting |
| /api/notifications/* | ms-notif | engine-reporting |
| /api/integrations/* | ms-ext-snow | engine-integrations |
| /api/generate/* | ms-gen-* | processor-generators |

## Migration Checklist

### Phase 1: Infrastructure Setup
- [ ] Deploy new Docker Compose configuration
- [ ] Configure Dapr sidecars for 8 units
- [ ] Update Nginx routing configuration
- [ ] Verify all health endpoints respond

### Phase 2: Database Migration
- [ ] Create consolidated database schemas
- [ ] Run Flyway migrations in order
- [ ] Migrate data from legacy databases
- [ ] Verify foreign key relationships

### Phase 3: Service Deployment
- [ ] Deploy engine-core and verify auth flows
- [ ] Deploy engine-ingestor and test file uploads
- [ ] Deploy engine-orchestrator and verify workflows
- [ ] Deploy engine-data and verify queries
- [ ] Deploy engine-reporting and verify reports
- [ ] Deploy processor-atomizers and test processing
- [ ] Deploy processor-generators and test generation

### Phase 4: Frontend Update
- [ ] Update environment variables if needed
- [ ] Test all user workflows
- [ ] Verify authentication flow

### Phase 5: Cleanup (Post-Migration)
- [ ] Archive old service directories
- [ ] Remove legacy Docker configurations
- [ ] Update CI/CD pipelines
- [ ] Update documentation

## Environment Variables

See [`infra/docker/.env.example`](infra/docker/.env.example) for the consolidated environment configuration.

### Key Changes:
- 8 database variables instead of 20+
- Consolidated Redis configuration
- New port variables for each unit

## Testing

### Health Check Endpoints
```bash
# Check all units
curl http://localhost:8081/actuator/health  # engine-core
curl http://localhost:8082/actuator/health  # engine-ingestor
curl http://localhost:8083/actuator/health  # engine-orchestrator
curl http://localhost:8100/actuator/health  # engine-data
curl http://localhost:8105/actuator/health  # engine-reporting
curl http://localhost:8107/actuator/health  # engine-integrations
curl http://localhost:8088/health            # processor-atomizers
curl http://localhost:8111/health            # processor-generators
```

### API Tests
```bash
# Test auth
curl http://localhost/api/auth/login

# Test data query
curl http://localhost/api/query/reports

# Test report generation
curl -X POST http://localhost/api/generate/pptx
```

## Rollback Plan

If issues occur during migration:

1. **Keep legacy services running** in parallel during transition
2. **Use feature flags** to route traffic between old and new
3. **Database rollback**: Restore from backup if schema changes fail
4. **Dapr subscriptions**: Ensure no orphaned PubSub consumers

## Support

For issues or questions:
- Check logs: `docker compose logs <service-name>`
- Check Dapr sidecar: `docker logs <service-name>-dapr`
- Review metrics: Access Grafana at http://localhost:3000
