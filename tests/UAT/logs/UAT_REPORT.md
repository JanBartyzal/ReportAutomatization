# UAT Report — ReportAutomatization (RA)

Generated: 2026-05-11T09:27:32

## Summary

| Metric | Value |
|--------|-------|
| Total steps executed | 26 |
| Total assertions | 295 |
| Passed | 281 |
| Failed | 6 |
| Skipped (missing features) | 8 |
| Overall success rate | 95.3% |

## Step Results

| Step | Total | Passed | Failed | Skipped | Success% | Status |
|------|-------|--------|--------|---------|----------|--------|
| step00_Init | 9 | 9 | 0 | 0 | 100% | PASS |
| step01_Infrastructure_Auth | 13 | 13 | 0 | 0 | 100% | PASS |
| step02_File_Upload | 12 | 12 | 0 | 0 | 100% | PASS |
| step03_Atomizer_PPTX | 7 | 5 | 0 | 2 | 71% | PASS |
| step04_Orchestrator_Workflow | 6 | 5 | 0 | 1 | 83% | PASS |
| step05_Sinks_Persistence | 10 | 9 | 0 | 1 | 90% | PASS |
| step06_Analytics_Query | 15 | 15 | 0 | 0 | 100% | PASS |
| step07_Admin_Management | 11 | 11 | 0 | 0 | 100% | PASS |
| step08_Batch_Organization | 12 | 11 | 1 | 0 | 92% | PARTIAL |
| step09_Frontend_SPA | 8 | 8 | 0 | 0 | 100% | PASS |
| step10_Atomizer_Excel | 21 | 19 | 1 | 1 | 90% | PARTIAL |
| step11_Dashboards_SQL | 22 | 21 | 1 | 0 | 95% | PARTIAL |
| step12_API_AI_MCP | 8 | 8 | 0 | 0 | 100% | PASS |
| step13_Notifications | 13 | 13 | 0 | 0 | 100% | PASS |
| step14_Data_Versioning | 7 | 7 | 0 | 0 | 100% | PASS |
| step15_Schema_Mapping | 9 | 8 | 0 | 1 | 89% | PASS |
| step16_Audit_Compliance | 13 | 13 | 0 | 0 | 100% | PASS |
| step17_Report_Lifecycle | 22 | 22 | 0 | 0 | 100% | PASS |
| step18_PPTX_Generation | 11 | 9 | 2 | 0 | 82% | PARTIAL |
| step19_Form_Builder | 21 | 19 | 1 | 1 | 90% | PARTIAL |
| step20_Period_Management | 14 | 14 | 0 | 0 | 100% | PASS |
| step21_Local_Forms | 9 | 9 | 0 | 0 | 100% | PASS |
| step22_Period_Comparison | 1 | 1 | 0 | 0 | 100% | PASS |
| step23_ServiceNow_Integration | 7 | 7 | 0 | 0 | 100% | PASS |
| step24_Smart_Persistence | 5 | 4 | 0 | 1 | 80% | PASS |
| step25_DevOps_Observability | 9 | 9 | 0 | 0 | 100% | PASS |

## Charter Traceability

| FS | Charter area | UAT step | Covered areas | Step status |
|----|--------------|----------|---------------|-------------|
| FS01 | Infrastructure & Core | step01_Infrastructure_Auth | Gateway health, ForwardAuth/Auth service, RBAC, org context | PASS |
| FS02 | File Ingestor | step02_File_Upload | Upload purpose, MIME/magic validation, AV scan, metadata, auth denial | PASS |
| FS03 | Atomizers | step03_Atomizer_PPTX | PPTX extraction, slide/table/image artifacts, blob references | PASS |
| FS04 | Custom Orchestrator | step04_Orchestrator_Workflow | Workflow status, steps, idempotent retrigger, failed jobs | PASS |
| FS05 | Sinks & Persistence | step05_Sinks_Persistence | Table/document persistence, sink logs, RLS isolation | PASS |
| FS06 | Analytics & Query | step06_Analytics_Query | CQRS read model, filters, search/query access | PASS |
| FS07 | Admin Backend & UI | step07_Admin_Management | Organizations, users, API keys, admin-only actions | PASS |
| FS08 | Batch Management | step08_Batch_Organization | Batches/reporting periods, membership, cross-org rules | PARTIAL |
| FS09 | Frontend SPA | step09_Frontend_SPA | SPA availability, file list/detail, notification stream | PASS |
| FS10 | Excel Parsing | step10_Atomizer_Excel | Workbook/sheet parsing, tables, formulas, merged cells, validation | PARTIAL |
| FS11 | Dashboards & SQL | step11_Dashboards_SQL | Dashboard CRUD, widgets, SQL-like aggregation, visibility | PARTIAL |
| FS12 | API & AI MCP | step12_API_AI_MCP | API keys, AI analyze/quota, MCP health | PASS |
| FS13 | Notifications | step13_Notifications | Notification CRUD, read/unread, SSE/WebSocket stream | PASS |
| FS14 | Data Versioning | step14_Data_Versioning | Versions, diff, rollback/revert, immutable history | PASS |
| FS15 | Schema Mapping Registry | step15_Schema_Mapping | Mappings, suggestions, Excel-to-form mapping, slide metadata | PASS |
| FS16 | Audit & Compliance | step16_Audit_Compliance | Audit search/export, immutable audit events, security events | PASS |
| FS17 | Report Lifecycle | step17_Report_Lifecycle | State transitions, submissions, approvals, checklist rules | PASS |
| FS18 | PPTX Generation | step18_PPTX_Generation | Template upload, placeholders, mappings, generation/download | PARTIAL |
| FS19 | Dynamic Form Builder | step19_Form_Builder | Forms, publish, submissions, autosave, Excel import/export | PARTIAL |
| FS20 | Period Management | step20_Period_Management | Periods, deadlines, closing/locking, assignment | PASS |
| FS21 | Local Forms | step21_Local_Forms | Local scope ownership, sharing, release to holding | PASS |
| FS22 | Advanced Period Comparison | step22_Period_Comparison | Period/KPI/multi-org comparisons and PPTX export | PASS |
| FS23 | ServiceNow Integration | step23_ServiceNow_Integration | Connections, tests, sync, schedules, project/RAG sync | PASS |
| FS24 | Smart Persistence Promotion | step24_Smart_Persistence | Promotion candidates, schema proposal, approval | PASS |
| FS25 | Sink Browser & Learning | step05_Sinks_Persistence | Sink browsing, manual corrections, selected sinks, learning feedback | PASS |
| FS26 | Report Generation UI & Export | step18_PPTX_Generation | Generation UI/API export flow, batch report output | PARTIAL |
| FS27 | Live Excel Export & External Sync | step26_Excel_Sync | Export flow CRUD/RBAC/RLS, dry-run, execution, partial sheet update | NOT_RUN |
| FS99 | DevOps & Observability | step25_DevOps_Observability | Health, metrics, probes, info, loggers, gateway reachability | PASS |

## Error Details

The following steps produced error reports:

- [step03_Atomizer_PPTX](step03_Atomizer_PPTX_errors.md)
- [step04_Orchestrator_Workflow](step04_Orchestrator_Workflow_errors.md)
- [step05_Sinks_Persistence](step05_Sinks_Persistence_errors.md)
- [step08_Batch_Organization](step08_Batch_Organization_errors.md)
- [step10_Atomizer_Excel](step10_Atomizer_Excel_errors.md)
- [step11_Dashboards_SQL](step11_Dashboards_SQL_errors.md)
- [step15_Schema_Mapping](step15_Schema_Mapping_errors.md)
- [step18_PPTX_Generation](step18_PPTX_Generation_errors.md)
- [step19_Form_Builder](step19_Form_Builder_errors.md)
- [step24_Smart_Persistence](step24_Smart_Persistence_errors.md)

---

_Report generated by `shared/report_generator.py`_
