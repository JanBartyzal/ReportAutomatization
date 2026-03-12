-- V2: Advanced Period Comparison support (P6-W1-004)
-- Configurable KPIs and comparison configurations for cross-period/cross-org analytics.

-- Comparison configurations: saved comparison setups
CREATE TABLE IF NOT EXISTS comparison_configs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    config          JSONB NOT NULL DEFAULT '{}',
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comparison_configs_org_id ON comparison_configs(org_id);
CREATE INDEX idx_comparison_configs_created_by ON comparison_configs(created_by);

-- RLS on comparison_configs
ALTER TABLE comparison_configs ENABLE ROW LEVEL SECURITY;
CREATE POLICY comparison_configs_org_isolation ON comparison_configs
    USING (
        org_id = current_setting('app.current_org_id', true)::UUID
        OR current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
    );
ALTER TABLE comparison_configs FORCE ROW LEVEL SECURITY;

-- KPI definitions: user-defined comparison metrics
CREATE TABLE IF NOT EXISTS comparison_kpis (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    value_field     VARCHAR(255) NOT NULL,
    aggregation     VARCHAR(10) NOT NULL DEFAULT 'SUM'
                    CHECK (aggregation IN ('SUM', 'AVG', 'COUNT', 'MIN', 'MAX')),
    group_by        JSONB NOT NULL DEFAULT '[]',
    source_type     VARCHAR(20) DEFAULT 'ALL'
                    CHECK (source_type IN ('FILE', 'FORM', 'ALL')),
    normalization   VARCHAR(20) DEFAULT 'NONE'
                    CHECK (normalization IN ('NONE', 'MONTHLY', 'DAILY', 'ANNUAL')),
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_comparison_kpis_org_id ON comparison_kpis(org_id);
CREATE INDEX idx_comparison_kpis_active ON comparison_kpis(is_active) WHERE is_active = true;

-- RLS on comparison_kpis
ALTER TABLE comparison_kpis ENABLE ROW LEVEL SECURITY;
CREATE POLICY comparison_kpis_org_isolation ON comparison_kpis
    USING (
        org_id = current_setting('app.current_org_id', true)::UUID
        OR current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
    );
ALTER TABLE comparison_kpis FORCE ROW LEVEL SECURITY;

-- Auto-update triggers
CREATE TRIGGER trg_comparison_configs_updated_at
    BEFORE UPDATE ON comparison_configs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_comparison_kpis_updated_at
    BEFORE UPDATE ON comparison_kpis
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Reuse the update_updated_at_column function if it doesn't exist yet
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE comparison_configs IS 'Saved comparison setups for cross-period/cross-org analytics';
COMMENT ON TABLE comparison_kpis IS 'User-defined KPI metrics for comparison queries';
COMMENT ON COLUMN comparison_kpis.normalization IS 'Period normalization: NONE (raw), MONTHLY/DAILY/ANNUAL (normalize to comparable basis)';
