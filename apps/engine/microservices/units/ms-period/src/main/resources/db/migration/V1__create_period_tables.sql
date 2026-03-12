-- V1__create_period_tables.sql
-- Reporting periods and organization assignments

CREATE TABLE IF NOT EXISTS periods (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    period_type         VARCHAR(50)  NOT NULL,
    period_code         VARCHAR(50)  NOT NULL UNIQUE,
    start_date          DATE         NOT NULL,
    end_date            DATE         NOT NULL,
    submission_deadline TIMESTAMPTZ  NOT NULL,
    review_deadline     TIMESTAMPTZ  NOT NULL,
    status              VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    holding_id          VARCHAR(255) NOT NULL,
    cloned_from_id      UUID         REFERENCES periods(id),
    created_by          VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS period_org_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id       UUID         NOT NULL REFERENCES periods(id) ON DELETE CASCADE,
    org_id          VARCHAR(255) NOT NULL,
    assigned_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE(period_id, org_id)
);

-- Indexes
CREATE INDEX idx_periods_status ON periods (status);
CREATE INDEX idx_periods_holding ON periods (holding_id);
CREATE INDEX idx_periods_deadline ON periods (submission_deadline);
CREATE INDEX idx_period_assignments_period ON period_org_assignments (period_id);
CREATE INDEX idx_period_assignments_org ON period_org_assignments (org_id);

-- Updated_at trigger
CREATE OR REPLACE FUNCTION update_period_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_periods_updated_at
    BEFORE UPDATE ON periods
    FOR EACH ROW EXECUTE FUNCTION update_period_updated_at();
