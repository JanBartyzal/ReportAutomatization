-- V1: Create dashboards table for dashboard configurations
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE dashboards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL,
    created_by UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    config JSONB NOT NULL DEFAULT '{}',
    chart_type VARCHAR(50) NOT NULL DEFAULT 'bar',
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_chart_type CHECK (chart_type IN ('bar', 'line', 'pie', 'heatmap', 'table'))
);

CREATE INDEX idx_dashboards_org_id ON dashboards(org_id);
CREATE INDEX idx_dashboards_created_by ON dashboards(created_by);
CREATE INDEX idx_dashboards_is_public ON dashboards(is_public) WHERE is_public = true;

-- RLS
ALTER TABLE dashboards ENABLE ROW LEVEL SECURITY;
CREATE POLICY dashboards_org_isolation ON dashboards
    USING (org_id = current_setting('app.current_org_id', true)::UUID);
ALTER TABLE dashboards FORCE ROW LEVEL SECURITY;

COMMENT ON TABLE dashboards IS 'Dashboard configurations with JSONB-stored query definitions';
COMMENT ON COLUMN dashboards.config IS 'JSON config: {data_source, group_by, order_by, filters, date_range, aggregation}';
