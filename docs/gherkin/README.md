# Gherkin Analysis – PPTX Analyzer & Automation Platform

BDD (Behavior-Driven Development) feature files derived from **Project Charter v5.0 (Consolidated Topology P8)**.

## Feature Files

| File | Feature Set | Priority | Scenarios | Microservices |
|------|-------------|----------|-----------|---------------|
| `FS01_infrastructure_core.feature` | FS01 – Infrastructure & Core | KRITICKÁ | 20 | router, engine-core:auth |
| `FS02_file_ingestor.feature` | FS02 – File Ingestor (Input) | KRITICKÁ | 18 | engine-ingestor, engine-ingestor:scanner |
| `FS03_atomizers.feature` | FS03 – Atomizers – Stateless Extractors | KRITICKÁ | 22 | processor-atomizers (pptx, xls, pdf, csv, ai, cleanup) |
| `FS04_orchestrator.feature` | FS04 – Custom Orchestrator | KRITICKÁ | 18 | engine-orchestrator |
| `FS05_sinks_persistence.feature` | FS05 – Sinks – Storage & Persistence | KRITICKÁ | 15 | engine-data (sink-tbl, sink-doc, sink-log) |
| `FS06_analytics_query.feature` | FS06 – Analytics & Query (CQRS) | VYSOKÁ | 13 | engine-data:query, engine-data:dashboard, engine-data:search |
| `FS07_admin_backend.feature` | FS07 – Admin Backend & UI | VYSOKÁ | 17 | engine-core:auth, engine-core:admin |
| `FS08_batch_management.feature` | FS08 – Batch Management | VYSOKÁ | 12 | engine-core:admin, engine-core:batch |
| `FS09_frontend_spa.feature` | FS09 – Frontend SPA (React) | KRITICKÁ | 20 | frontend |
| `FS10_excel_parsing.feature` | FS10 – Excel Parsing Logic | VYSOKÁ | 13 | processor-atomizers:xls |
| `FS11_dashboards.feature` | FS11 – Dashboards & SQL Reporting | STŘEDNÍ | 20 | frontend, engine-data:dashboard |
| `FS12_api_ai_mcp.feature` | FS12 – API & AI Integration (MCP) | STŘEDNÍ | 16 | processor-atomizers:ai, processor-generators:mcp |
| `FS13_notifications.feature` | FS13 – Notification Center & Alerts | STŘEDNÍ | 17 | engine-reporting:notification |
| `FS14_data_versioning.feature` | FS14 – Data Versioning & Diff Tool | STŘEDNÍ | 12 | engine-core:versioning |
| `FS15_schema_mapping.feature` | FS15 – Template & Schema Mapping | STŘEDNÍ | 15 | engine-data:template |
| `FS16_audit_compliance.feature` | FS16 – Audit & Compliance Log | STŘEDNÍ | 20 | engine-core:audit |
| `FS17_report_lifecycle.feature` | FS17 – OPEX Report Lifecycle | KRITICKÁ | 22 | engine-reporting:lifecycle, engine-orchestrator |
| `FS18_pptx_generation.feature` | FS18 – PPTX Report Generation | KRITICKÁ | 20 | engine-reporting:pptx-template, processor-generators:pptx |
| `FS19_form_builder.feature` | FS19 – Dynamic Form Builder | KRITICKÁ | 30 | engine-reporting:form, frontend |
| `FS20_period_management.feature` | FS20 – Reporting Period & Deadlines | VYSOKÁ | 28 | engine-reporting:period |
| `FS21_local_scope.feature` | FS21 – Local Forms & Templates | STŘEDNÍ | 16 | engine-reporting:form, engine-reporting:pptx-template, engine-core:admin |
| `FS22_advanced_comparison.feature` | FS22 – Advanced Period Comparison | NÍZKÁ | 10 | engine-data:dashboard, engine-reporting:period |
| `FS23_servicenow.feature` | FS23 – ServiceNow Integration | STŘEDNÍ | 13 | engine-integrations:servicenow, processor-generators:xls, engine-reporting:notification |
| `FS24_smart_persistence.feature` | FS24 – Smart Persistence Promotion | STŘEDNÍ | 14 | engine-core:admin, engine-data:sink-tbl, engine-data:template |
| `FS99_devops.feature` | FS99 – DevOps & Observability | VYSOKÁ | 15 | CI/CD, Observability stack |

**Total: 25 feature files, ~391 scenarios**

## Tags

| Tag | Meaning |
|-----|---------|
| `@critical` | KRITICKÁ priority feature set |
| `@high` | VYSOKÁ priority |
| `@medium` | STŘEDNÍ priority |
| `@low` | NÍZKÁ priority |
| `@placeholder` | Scenario defined but not yet implemented (FS22) |
| `@security` | Security-related scenario |
| `@performance` | Performance requirement scenario |
| `@error-handling` | Error/edge case scenario |

## Rollout Phases

| Phase | Feature Sets |
|-------|-------------|
| **P1** MVP Core | FS01, FS02, FS03-PPTX, FS04, FS05, FS09-basic |
| **P2** Extended Parsing | FS03-rest, FS10, FS06 |
| **P3a** Intelligence & Admin | FS07, FS08, FS12, FS15 |
| **P3b** Lifecycle + Period | FS17, FS20 |
| **P3c** Form Builder | FS19 |
| **P4a** Enterprise Features | FS11, FS13, FS14, FS16 |
| **P4b** PPTX Generator | FS18 |
| **P5** DevOps Maturity | FS99 |
| **P6** Local Scope | FS21, FS22 |
| **P7** Integrations | FS23, FS24 |
