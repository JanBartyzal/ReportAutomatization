-- Excel Sync Integration Test Seed Data
-- Run against the 'integrations' schema before Step26 UAT tests
-- Usage: psql -U engine_integrations_user -d reportplatform -v ON_ERROR_STOP=1 -f seed_data.sql

SET search_path TO integrations, rls, public;

-- ============================================================
-- Export Flow Definitions
-- ============================================================
INSERT INTO export_flow_definitions (
    id, org_id, name, description,
    sql_query, target_type, target_path, target_sheet,
    file_naming, custom_file_name,
    trigger_type, trigger_filter,
    is_active, created_by, created_at, updated_at
) VALUES
(
    '11111111-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'OPEX Monthly Report – Local',
    'Exports monthly OPEX data to a local network share in CSV format',
    'SELECT department, category, SUM(amount) AS total FROM opex_entries WHERE period = ''{{period}}'' GROUP BY department, category ORDER BY department',
    'LOCAL_PATH',
    '/mnt/exports/opex',
    'OPEX_Data',
    'CUSTOM',
    'opex_monthly_{{period}}.xlsx',
    'AUTO',
    '{"eventType": "data-imported", "batchType": "OPEX"}',
    true,
    'seed-script',
    NOW() - INTERVAL '30 days',
    NOW() - INTERVAL '30 days'
),
(
    '11111111-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'OPEX Monthly Report – SharePoint',
    'Pushes monthly OPEX data to the Finance SharePoint library',
    'SELECT department, category, SUM(amount) AS total FROM opex_entries WHERE period = ''{{period}}'' GROUP BY department, category ORDER BY department',
    'SHAREPOINT',
    'Finance/Reports/2026',
    'OPEX_Data',
    'BATCH_NAME',
    NULL,
    'MANUAL',
    '{}',
    true,
    'seed-script',
    NOW() - INTERVAL '15 days',
    NOW() - INTERVAL '5 days'
)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Export Flow Executions
-- ============================================================
INSERT INTO export_flow_executions (
    id, flow_id, org_id,
    trigger_source, trigger_event_id,
    status, rows_exported, target_path_used,
    error_message, started_at, completed_at
) VALUES
(
    '22222222-0000-0000-0000-000000000001',
    '11111111-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'AUTO', '33333333-0000-0000-0000-000000000001',
    'SUCCESS', 120, '/mnt/exports/opex/opex_monthly_2026-03.xlsx',
    NULL,
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '10 days' + INTERVAL '45 seconds'
),
(
    '22222222-0000-0000-0000-000000000002',
    '11111111-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'AUTO', '33333333-0000-0000-0000-000000000002',
    'FAILED', NULL, NULL,
    'gRPC call to processor-generators timed out after 30s',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '5 days' + INTERVAL '30 seconds'
),
(
    '22222222-0000-0000-0000-000000000003',
    '11111111-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    'MANUAL', NULL,
    'RUNNING', NULL, NULL,
    NULL,
    NOW() - INTERVAL '1 minute',
    NULL
)
ON CONFLICT (id) DO NOTHING;
