-- V1: Create processing_logs table (append-only)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE processing_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id VARCHAR(255) NOT NULL,
    workflow_id VARCHAR(255),
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,  -- STARTED, COMPLETED, FAILED
    duration_ms BIGINT,
    error_detail TEXT,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_processing_logs_file_id ON processing_logs(file_id);
CREATE INDEX idx_processing_logs_workflow_id ON processing_logs(workflow_id);
CREATE INDEX idx_processing_logs_created_at ON processing_logs(created_at);

-- INSERT-only permissions (application user should only have INSERT privilege)
GRANT SELECT, INSERT ON processing_logs TO ms_sink_log;

COMMENT ON TABLE processing_logs IS 'Append-only processing log entries for workflow tracking';
