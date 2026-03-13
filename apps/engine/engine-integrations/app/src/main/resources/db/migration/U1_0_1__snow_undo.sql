-- U1_0_1__snow_undo.sql
-- Reverses V1_0_1__snow_create_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop RLS policies
DROP POLICY IF EXISTS servicenow_connections_org_isolation ON servicenow_connections;

-- Disable RLS
ALTER TABLE servicenow_connections DISABLE ROW LEVEL SECURITY;

-- Drop index
DROP INDEX IF EXISTS idx_snow_conn_org;
DROP INDEX IF EXISTS idx_snow_conn_enabled;

-- Drop table
DROP TABLE IF EXISTS servicenow_connections CASCADE;
