-- U3_0_1__batch_undo.sql
-- Reverses V3_0_1__batch_create_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop indexes first
DROP INDEX IF EXISTS idx_batches_holding;
DROP INDEX IF EXISTS idx_batches_status;
DROP INDEX IF EXISTS idx_batches_period;
DROP INDEX IF EXISTS idx_batch_files_batch;
DROP INDEX IF EXISTS idx_batch_files_file;
DROP INDEX IF EXISTS idx_periods_holding;
DROP INDEX IF EXISTS idx_periods_code;
DROP INDEX IF EXISTS idx_periods_batch;

-- Drop RLS policies first (if they exist)
DROP POLICY IF EXISTS batches_org_isolation ON batches;
DROP POLICY IF EXISTS batch_files_org_isolation ON batch_files;
DROP POLICY IF EXISTS periods_org_isolation ON periods;

-- Disable RLS before dropping tables
ALTER TABLE batches DISABLE ROW LEVEL SECURITY;
ALTER TABLE batch_files DISABLE ROW LEVEL SECURITY;
ALTER TABLE periods DISABLE ROW LEVEL SECURITY;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS batch_files CASCADE;
DROP TABLE IF EXISTS periods CASCADE;
DROP TABLE IF EXISTS batches CASCADE;
