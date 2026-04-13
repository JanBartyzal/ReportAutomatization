-- U1_0_4__excel_sync_undo.sql
-- Undo Excel Sync integration tables

DROP POLICY IF EXISTS export_flow_executions_org_isolation ON export_flow_executions;
DROP TABLE IF EXISTS export_flow_executions;

DROP POLICY IF EXISTS export_flow_definitions_org_isolation ON export_flow_definitions;
DROP TABLE IF EXISTS export_flow_definitions;
