# UAT traceability matrix.
# Maps project charter Feature Sets to executable UAT steps and the main
# acceptance areas each step is expected to exercise.

FEATURE_COVERAGE = [
    {
        "fs": "FS01",
        "name": "Infrastructure & Core",
        "step": "step01_Infrastructure_Auth",
        "areas": "Gateway health, ForwardAuth/Auth service, RBAC, org context",
    },
    {
        "fs": "FS02",
        "name": "File Ingestor",
        "step": "step02_File_Upload",
        "areas": "Upload purpose, MIME/magic validation, AV scan, metadata, auth denial",
    },
    {
        "fs": "FS03",
        "name": "Atomizers",
        "step": "step03_Atomizer_PPTX",
        "areas": "PPTX extraction, slide/table/image artifacts, blob references",
    },
    {
        "fs": "FS04",
        "name": "Custom Orchestrator",
        "step": "step04_Orchestrator_Workflow",
        "areas": "Workflow status, steps, idempotent retrigger, failed jobs",
    },
    {
        "fs": "FS05",
        "name": "Sinks & Persistence",
        "step": "step05_Sinks_Persistence",
        "areas": "Table/document persistence, sink logs, RLS isolation",
    },
    {
        "fs": "FS06",
        "name": "Analytics & Query",
        "step": "step06_Analytics_Query",
        "areas": "CQRS read model, filters, search/query access",
    },
    {
        "fs": "FS07",
        "name": "Admin Backend & UI",
        "step": "step07_Admin_Management",
        "areas": "Organizations, users, API keys, admin-only actions",
    },
    {
        "fs": "FS08",
        "name": "Batch Management",
        "step": "step08_Batch_Organization",
        "areas": "Batches/reporting periods, membership, cross-org rules",
    },
    {
        "fs": "FS09",
        "name": "Frontend SPA",
        "step": "step09_Frontend_SPA",
        "areas": "SPA availability, file list/detail, notification stream",
    },
    {
        "fs": "FS10",
        "name": "Excel Parsing",
        "step": "step10_Atomizer_Excel",
        "areas": "Workbook/sheet parsing, tables, formulas, merged cells, validation",
    },
    {
        "fs": "FS11",
        "name": "Dashboards & SQL",
        "step": "step11_Dashboards_SQL",
        "areas": "Dashboard CRUD, widgets, SQL-like aggregation, visibility",
    },
    {
        "fs": "FS12",
        "name": "API & AI MCP",
        "step": "step12_API_AI_MCP",
        "areas": "API keys, AI analyze/quota, MCP health",
    },
    {
        "fs": "FS13",
        "name": "Notifications",
        "step": "step13_Notifications",
        "areas": "Notification CRUD, read/unread, SSE/WebSocket stream",
    },
    {
        "fs": "FS14",
        "name": "Data Versioning",
        "step": "step14_Data_Versioning",
        "areas": "Versions, diff, rollback/revert, immutable history",
    },
    {
        "fs": "FS15",
        "name": "Schema Mapping Registry",
        "step": "step15_Schema_Mapping",
        "areas": "Mappings, suggestions, Excel-to-form mapping, slide metadata",
    },
    {
        "fs": "FS16",
        "name": "Audit & Compliance",
        "step": "step16_Audit_Compliance",
        "areas": "Audit search/export, immutable audit events, security events",
    },
    {
        "fs": "FS17",
        "name": "Report Lifecycle",
        "step": "step17_Report_Lifecycle",
        "areas": "State transitions, submissions, approvals, checklist rules",
    },
    {
        "fs": "FS18",
        "name": "PPTX Generation",
        "step": "step18_PPTX_Generation",
        "areas": "Template upload, placeholders, mappings, generation/download",
    },
    {
        "fs": "FS19",
        "name": "Dynamic Form Builder",
        "step": "step19_Form_Builder",
        "areas": "Forms, publish, submissions, autosave, Excel import/export",
    },
    {
        "fs": "FS20",
        "name": "Period Management",
        "step": "step20_Period_Management",
        "areas": "Periods, deadlines, closing/locking, assignment",
    },
    {
        "fs": "FS21",
        "name": "Local Forms",
        "step": "step21_Local_Forms",
        "areas": "Local scope ownership, sharing, release to holding",
    },
    {
        "fs": "FS22",
        "name": "Advanced Period Comparison",
        "step": "step22_Period_Comparison",
        "areas": "Period/KPI/multi-org comparisons and PPTX export",
    },
    {
        "fs": "FS23",
        "name": "ServiceNow Integration",
        "step": "step23_ServiceNow_Integration",
        "areas": "Connections, tests, sync, schedules, project/RAG sync",
    },
    {
        "fs": "FS24",
        "name": "Smart Persistence Promotion",
        "step": "step24_Smart_Persistence",
        "areas": "Promotion candidates, schema proposal, approval",
    },
    {
        "fs": "FS25",
        "name": "Sink Browser & Learning",
        "step": "step05_Sinks_Persistence",
        "areas": "Sink browsing, manual corrections, selected sinks, learning feedback",
    },
    {
        "fs": "FS26",
        "name": "Report Generation UI & Export",
        "step": "step18_PPTX_Generation",
        "areas": "Generation UI/API export flow, batch report output",
    },
    {
        "fs": "FS27",
        "name": "Live Excel Export & External Sync",
        "step": "step26_Excel_Sync",
        "areas": "Export flow CRUD/RBAC/RLS, dry-run, execution, partial sheet update",
    },
    {
        "fs": "FS99",
        "name": "DevOps & Observability",
        "step": "step25_DevOps_Observability",
        "areas": "Health, metrics, probes, info, loggers, gateway reachability",
    },
]

