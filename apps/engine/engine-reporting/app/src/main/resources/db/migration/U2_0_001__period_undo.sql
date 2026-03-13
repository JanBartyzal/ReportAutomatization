-- U2_0_001__period_undo.sql
-- Reverses V2_0_001__create_period_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop trigger
DROP TRIGGER IF EXISTS trigger_periods_updated_at ON periods;

-- Drop function if no longer needed
DROP FUNCTION IF EXISTS update_period_updated_at();

-- Drop indexes
DROP INDEX IF EXISTS idx_periods_status;
DROP INDEX IF EXISTS idx_periods_holding;
DROP INDEX IF EXISTS idx_periods_deadline;
DROP INDEX IF EXISTS idx_period_assignments_period;
DROP INDEX IF EXISTS idx_period_assignments_org;

-- Drop RLS policies
DROP POLICY IF EXISTS periods_org_isolation ON periods;
DROP POLICY IF EXISTS period_org_assignments_org_isolation ON period_org_assignments;

-- Disable RLS
ALTER TABLE periods DISABLE ROW LEVEL SECURITY;
ALTER TABLE period_org_assignments DISABLE ROW LEVEL SECURITY;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS period_org_assignments CASCADE;
DROP TABLE IF EXISTS periods CASCADE;
