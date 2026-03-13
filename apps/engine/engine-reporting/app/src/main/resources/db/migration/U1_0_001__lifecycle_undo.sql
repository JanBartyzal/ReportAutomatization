-- U1_0_001__lifecycle_undo.sql
-- Reverses V1_0_001__create_lifecycle_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop triggers first
DROP TRIGGER IF EXISTS trigger_reports_updated_at ON reports;
DROP TRIGGER IF EXISTS trigger_checklists_updated_at ON submission_checklists;

-- Drop function if no longer needed (check if used by other tables)
DROP FUNCTION IF EXISTS update_updated_at_column();

-- Drop indexes
DROP INDEX IF EXISTS idx_reports_org_id;
DROP INDEX IF EXISTS idx_reports_period_id;
DROP INDEX IF EXISTS idx_reports_status;
DROP INDEX IF EXISTS idx_reports_org_period;
DROP INDEX IF EXISTS idx_history_report_id;
DROP INDEX IF EXISTS idx_history_created_at;

-- Drop RLS policies
DROP POLICY IF EXISTS reports_org_isolation ON reports;
DROP POLICY IF EXISTS report_status_history_org_isolation ON report_status_history;
DROP POLICY IF EXISTS submission_checklists_org_isolation ON submission_checklists;

-- Disable RLS
ALTER TABLE reports DISABLE ROW LEVEL SECURITY;
ALTER TABLE report_status_history DISABLE ROW LEVEL SECURITY;
ALTER TABLE submission_checklists DISABLE ROW LEVEL SECURITY;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS submission_checklists CASCADE;
DROP TABLE IF EXISTS report_status_history CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
