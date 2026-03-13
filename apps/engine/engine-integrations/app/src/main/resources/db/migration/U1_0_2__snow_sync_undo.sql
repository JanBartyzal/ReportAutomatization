-- U1_0_2__snow_sync_undo.sql
-- Reverses V1_0_2__snow_create_sync_schedule_tables.sql
-- WARNING: This will delete all data in the affected tables

-- Drop RLS policies
DROP POLICY IF EXISTS sync_schedules_org_policy ON sync_schedules;
DROP POLICY IF EXISTS sync_job_history_org_policy ON sync_job_history;

-- Disable RLS
ALTER TABLE sync_schedules DISABLE ROW LEVEL SECURITY;
ALTER TABLE sync_job_history DISABLE ROW LEVEL SECURITY;

-- Drop indexes
DROP INDEX IF EXISTS idx_sync_sched_conn;
DROP INDEX IF EXISTS idx_sync_sched_org;
DROP INDEX IF EXISTS idx_sync_sched_status;
DROP INDEX IF EXISTS idx_sync_sched_next_run;
DROP INDEX IF EXISTS idx_sync_jobs_schedule;
DROP INDEX IF EXISTS idx_sync_jobs_status;
DROP INDEX IF EXISTS idx_sync_jobs_started;

-- Drop tables (order matters due to foreign keys)
DROP TABLE IF EXISTS sync_job_history CASCADE;
DROP TABLE IF EXISTS sync_schedules CASCADE;
