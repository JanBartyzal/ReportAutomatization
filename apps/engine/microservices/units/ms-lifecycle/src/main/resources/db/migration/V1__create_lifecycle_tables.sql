-- V1__create_lifecycle_tables.sql
-- Report lifecycle: reports, status history, submission checklists

CREATE TABLE IF NOT EXISTS reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          VARCHAR(255) NOT NULL,
    period_id       UUID         NOT NULL,
    report_type     VARCHAR(100) NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'DRAFT',
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    submitted_by    VARCHAR(255),
    submitted_at    TIMESTAMPTZ,
    reviewed_by     VARCHAR(255),
    reviewed_at     TIMESTAMPTZ,
    approved_by     VARCHAR(255),
    approved_at     TIMESTAMPTZ,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(org_id, period_id, report_type)
);

CREATE TABLE IF NOT EXISTS report_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id       UUID         NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    from_status     VARCHAR(50),
    to_status       VARCHAR(50)  NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS submission_checklists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id       UUID         NOT NULL UNIQUE REFERENCES reports(id) ON DELETE CASCADE,
    checklist_json  JSONB        NOT NULL DEFAULT '[]'::jsonb,
    completed_pct   INTEGER      NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Indexes for reports
CREATE INDEX idx_reports_org_id ON reports (org_id);
CREATE INDEX idx_reports_period_id ON reports (period_id);
CREATE INDEX idx_reports_status ON reports (status);
CREATE INDEX idx_reports_org_period ON reports (org_id, period_id);

-- Indexes for history
CREATE INDEX idx_history_report_id ON report_status_history (report_id);
CREATE INDEX idx_history_created_at ON report_status_history (created_at);

-- Updated_at trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_reports_updated_at
    BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_checklists_updated_at
    BEFORE UPDATE ON submission_checklists
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
