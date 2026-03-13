-- U1_0_2__extend_form_undo.sql
-- Reverses V1_0_2__sinktbl_extend_form_responses.sql
-- WARNING: This will remove columns and drop the audit table

-- Drop RLS policies first
DROP POLICY IF EXISTS form_responses_org_isolation ON form_responses;
DROP POLICY IF EXISTS form_field_audit_org_isolation ON form_field_audit;

-- Disable RLS
ALTER TABLE form_responses DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_field_audit DISABLE ROW LEVEL SECURITY;

-- Drop indexes
DROP INDEX IF EXISTS idx_form_responses_org_period_version;
DROP INDEX IF EXISTS idx_form_field_audit_response_id;
DROP INDEX IF EXISTS idx_form_field_audit_changed_at;

-- Drop audit table
DROP TABLE IF EXISTS form_field_audit CASCADE;

-- Revert column changes on form_responses (PostgreSQL doesn't support DROP COLUMN IF NOT EXISTS in older versions)
ALTER TABLE form_responses DROP COLUMN IF EXISTS form_version_id;
ALTER TABLE form_responses DROP COLUMN IF EXISTS created_by;
ALTER TABLE form_responses DROP COLUMN IF EXISTS updated_by;
ALTER TABLE form_responses DROP COLUMN IF EXISTS updated_at;

-- Revoke permissions
REVOKE SELECT, INSERT, UPDATE, DELETE ON form_responses FROM ms_sink_tbl;
