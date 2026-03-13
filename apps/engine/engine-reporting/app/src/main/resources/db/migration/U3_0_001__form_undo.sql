-- U3_0_001__form_undo.sql
-- Reverses V3_0_001__create_form_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop triggers
DROP TRIGGER IF EXISTS trigger_forms_updated_at ON forms;
DROP TRIGGER IF EXISTS trigger_form_responses_updated_at ON form_responses;
DROP TRIGGER IF EXISTS trigger_form_field_values_updated_at ON form_field_values;
DROP TRIGGER IF EXISTS trigger_form_field_comments_updated_at ON form_field_comments;
DROP TRIGGER IF EXISTS trigger_form_assignments_updated_at ON form_assignments;

-- Drop indexes
DROP INDEX IF EXISTS idx_forms_org_id;
DROP INDEX IF EXISTS idx_forms_status;
DROP INDEX IF EXISTS idx_forms_scope;
DROP INDEX IF EXISTS idx_form_versions_form_id;
DROP INDEX IF EXISTS idx_form_fields_version_id;
DROP INDEX IF EXISTS idx_form_fields_section;
DROP INDEX IF EXISTS idx_form_responses_form_id;
DROP INDEX IF EXISTS idx_form_responses_org_id;
DROP INDEX IF EXISTS idx_form_responses_org_period_version;
DROP INDEX IF EXISTS idx_form_responses_status;
DROP INDEX IF EXISTS idx_form_field_values_response_id;
DROP INDEX IF EXISTS idx_form_field_comments_response_id;
DROP INDEX IF EXISTS idx_form_field_comments_response_field;
DROP INDEX IF EXISTS idx_form_assignments_form_id;
DROP INDEX IF EXISTS idx_form_assignments_org_id;

-- Drop RLS policies
DROP POLICY IF EXISTS forms_org_isolation ON forms;
DROP POLICY IF EXISTS form_versions_org_isolation ON form_versions;
DROP POLICY IF EXISTS form_fields_org_isolation ON form_fields;
DROP POLICY IF EXISTS form_responses_org_isolation ON form_responses;
DROP POLICY IF EXISTS form_field_values_org_isolation ON form_field_values;
DROP POLICY IF EXISTS form_field_comments_org_isolation ON form_field_comments;
DROP POLICY IF EXISTS form_assignments_org_isolation ON form_assignments;

-- Disable RLS
ALTER TABLE forms DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_versions DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_fields DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_responses DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_field_values DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_field_comments DISABLE ROW LEVEL SECURITY;
ALTER TABLE form_assignments DISABLE ROW LEVEL SECURITY;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS form_field_comments CASCADE;
DROP TABLE IF EXISTS form_field_values CASCADE;
DROP TABLE IF EXISTS form_assignments CASCADE;
DROP TABLE IF EXISTS form_responses CASCADE;
DROP TABLE IF EXISTS form_fields CASCADE;
DROP TABLE IF EXISTS form_versions CASCADE;
DROP TABLE IF EXISTS forms CASCADE;
