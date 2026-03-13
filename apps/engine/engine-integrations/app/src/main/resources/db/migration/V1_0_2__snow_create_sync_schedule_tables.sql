-- V1_0_2__snow_create_sync_schedule_tables.sql
-- Sync schedule and job history tables

CREATE TABLE IF NOT EXISTS sync_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id UUID NOT NULL REFERENCES servicenow_connections(id) ON DELETE CASCADE,
    org_id UUID NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_run_at TIMESTAMP WITH TIME ZONE,
    next_run_at TIMESTAMP WITH TIME ZONE,
    last_sync_timestamp VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE' CHECK (status IN ('IDLE', 'RUNNING', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE sync_schedules ENABLE ROW LEVEL SECURITY;
CREATE POLICY sync_schedules_org_policy ON sync_schedules
    USING (org_id = rls.get_current_org_id());

CREATE INDEX idx_sync_sched_conn ON sync_schedules(connection_id);
CREATE INDEX idx_sync_sched_org ON sync_schedules(org_id);
CREATE INDEX idx_sync_sched_status ON sync_schedules(status);
CREATE INDEX idx_sync_sched_next_run ON sync_schedules(next_run_at) WHERE enabled = true;

CREATE TABLE IF NOT EXISTS sync_job_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL REFERENCES sync_schedules(id) ON DELETE CASCADE,
    org_id UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_fetched INTEGER DEFAULT 0,
    records_stored INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL')),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE sync_job_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY sync_job_history_org_policy ON sync_job_history
    USING (org_id = rls.get_current_org_id());

CREATE INDEX idx_sync_jobs_schedule ON sync_job_history(schedule_id);
CREATE INDEX idx_sync_jobs_status ON sync_job_history(status);
CREATE INDEX idx_sync_jobs_started ON sync_job_history(started_at DESC);
