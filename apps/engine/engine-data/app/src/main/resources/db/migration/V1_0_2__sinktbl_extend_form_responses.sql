-- V2__extend_form_responses.sql
-- P3c-W1-003: Extend form_responses with versioned storage and field-level audit trail

-- Add form_version_id column for versioned storage
ALTER TABLE form_responses
    ADD COLUMN IF NOT EXISTS form_version_id VARCHAR(255);

-- Add audit trail columns
ALTER TABLE form_responses
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Field-level audit trail table
CREATE TABLE IF NOT EXISTS form_field_audit (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    response_id     UUID         NOT NULL REFERENCES form_responses(id) ON DELETE CASCADE,
    field_id        VARCHAR(255) NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    changed_by      VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Composite index for fast queries on versioned form responses
CREATE INDEX IF NOT EXISTS idx_form_responses_org_period_version
    ON form_responses(org_id, period_id, form_version_id);

-- Index for audit trail lookups
CREATE INDEX IF NOT EXISTS idx_form_field_audit_response_id
    ON form_field_audit(response_id);

CREATE INDEX IF NOT EXISTS idx_form_field_audit_changed_at
    ON form_field_audit(changed_at);

-- RLS policy on form_responses (org isolation)
ALTER TABLE form_responses ENABLE ROW LEVEL SECURITY;

CREATE POLICY form_responses_org_isolation ON form_responses
    USING (org_id = current_setting('app.current_org_id', true));

-- RLS policy on form_field_audit
ALTER TABLE form_field_audit ENABLE ROW LEVEL SECURITY;

CREATE POLICY form_field_audit_org_isolation ON form_field_audit
    USING (response_id IN (
        SELECT id FROM form_responses
        WHERE org_id = current_setting('app.current_org_id', true)
    ));

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON form_field_audit TO ms_sink_tbl;

COMMENT ON TABLE form_field_audit IS 'Field-level audit trail for form response changes';
COMMENT ON COLUMN form_field_audit.old_value IS 'Previous field value before change';
COMMENT ON COLUMN form_field_audit.new_value IS 'New field value after change';
