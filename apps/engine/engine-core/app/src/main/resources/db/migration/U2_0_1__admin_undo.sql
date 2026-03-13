-- U2_0_1__admin_undo.sql
-- Reverses V2_0_1__admin_create_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop indexes first
DROP INDEX IF EXISTS idx_api_keys_org;
DROP INDEX IF EXISTS idx_api_keys_created_by;
DROP INDEX IF EXISTS idx_role_audit_user;
DROP INDEX IF EXISTS idx_role_audit_performed_by;
DROP INDEX IF EXISTS idx_role_audit_performed_at;
DROP INDEX IF EXISTS idx_org_parent;
DROP INDEX IF EXISTS idx_org_tenant;
DROP INDEX IF EXISTS idx_org_type;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS api_keys CASCADE;
DROP TABLE IF EXISTS role_audit_log CASCADE;
DROP TABLE IF EXISTS organizations CASCADE;
