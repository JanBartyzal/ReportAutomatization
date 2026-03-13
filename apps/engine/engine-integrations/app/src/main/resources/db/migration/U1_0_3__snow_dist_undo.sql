-- U1_0_3__snow_dist_undo.sql
-- Reverses V1_0_3__snow_create_distribution_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop RLS policies
DROP POLICY IF EXISTS distribution_rules_org_policy ON distribution_rules;
DROP POLICY IF EXISTS distribution_history_org_policy ON distribution_history;

-- Disable RLS
ALTER TABLE distribution_rules DISABLE ROW LEVEL SECURITY;
ALTER TABLE distribution_history DISABLE ROW LEVEL SECURITY;

-- Drop indexes
DROP INDEX IF EXISTS idx_dist_rules_org;
DROP INDEX IF EXISTS idx_dist_rules_schedule;
DROP INDEX IF EXISTS idx_dist_history_org;
DROP INDEX IF EXISTS idx_dist_history_rule;
DROP INDEX IF EXISTS idx_dist_history_status;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS distribution_history CASCADE;
DROP TABLE IF EXISTS distribution_rules CASCADE;
