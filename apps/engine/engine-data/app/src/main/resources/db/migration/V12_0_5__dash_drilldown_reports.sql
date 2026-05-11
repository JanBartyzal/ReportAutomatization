-- FS28: Drill-down analytical report definitions and view states.

CREATE TABLE drilldown_report_definitions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(50) NOT NULL DEFAULT 'ANALYTICAL',
    base_period_type VARCHAR(50),
    default_filters JSONB NOT NULL DEFAULT '{}',
    layout_config JSONB NOT NULL DEFAULT '{}',
    is_public BOOLEAN NOT NULL DEFAULT false,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE drilldown_report_sections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES drilldown_report_definitions(id) ON DELETE CASCADE,
    org_id UUID NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    component_type VARCHAR(30) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    source_ref_id UUID,
    query_config JSONB NOT NULL DEFAULT '{}',
    drill_config JSONB NOT NULL DEFAULT '{}',
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_drilldown_section_key UNIQUE (report_id, section_key),
    CONSTRAINT chk_drilldown_component_type CHECK (component_type IN ('KPI', 'CHART', 'TABLE', 'TEXT')),
    CONSTRAINT chk_drilldown_source_type CHECK (source_type IN (
        'DASHBOARD_WIDGET', 'NAMED_QUERY', 'SINK_SELECTION', 'REPORT_FORM', 'RAW_SQL', 'AGGREGATION'
    ))
);

CREATE TABLE drilldown_report_views (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_id UUID NOT NULL REFERENCES drilldown_report_definitions(id) ON DELETE CASCADE,
    org_id UUID NOT NULL,
    user_id UUID NOT NULL,
    view_state JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_drilldown_reports_org ON drilldown_report_definitions(org_id);
CREATE INDEX idx_drilldown_reports_created_by ON drilldown_report_definitions(created_by);
CREATE INDEX idx_drilldown_reports_public ON drilldown_report_definitions(is_public) WHERE is_public = true;
CREATE INDEX idx_drilldown_sections_report ON drilldown_report_sections(report_id, display_order);
CREATE INDEX idx_drilldown_sections_org ON drilldown_report_sections(org_id);
CREATE INDEX idx_drilldown_views_report_user ON drilldown_report_views(report_id, user_id, created_at DESC);
CREATE INDEX idx_drilldown_views_expiry ON drilldown_report_views(expires_at) WHERE expires_at IS NOT NULL;

ALTER TABLE drilldown_report_definitions ENABLE ROW LEVEL SECURITY;
CREATE POLICY drilldown_reports_org_isolation ON drilldown_report_definitions
    USING (
        org_id::text = current_setting('app.current_org_id', true)
        OR is_public = true
    )
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
ALTER TABLE drilldown_report_definitions FORCE ROW LEVEL SECURITY;

ALTER TABLE drilldown_report_sections ENABLE ROW LEVEL SECURITY;
CREATE POLICY drilldown_sections_org_isolation ON drilldown_report_sections
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
ALTER TABLE drilldown_report_sections FORCE ROW LEVEL SECURITY;

ALTER TABLE drilldown_report_views ENABLE ROW LEVEL SECURITY;
CREATE POLICY drilldown_views_org_isolation ON drilldown_report_views
    USING (org_id::text = current_setting('app.current_org_id', true))
    WITH CHECK (org_id::text = current_setting('app.current_org_id', true));
ALTER TABLE drilldown_report_views FORCE ROW LEVEL SECURITY;

COMMENT ON TABLE drilldown_report_definitions IS 'FS28 analytical report definitions composed from dashboard widgets, named queries and sink selections';
COMMENT ON TABLE drilldown_report_sections IS 'FS28 report sections with summary query and drill configuration';
COMMENT ON TABLE drilldown_report_views IS 'FS28 persisted deep-link view states for drill-down report sessions';
