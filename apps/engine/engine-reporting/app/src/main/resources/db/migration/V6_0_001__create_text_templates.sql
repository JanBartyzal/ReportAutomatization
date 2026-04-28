-- V6_0_001: Text Template Engine
-- Unified text-based report templates with data bindings to Named Queries.
-- Works with ANY data source accessible via Named Queries (platform files,
-- ServiceNow ITSM, projects, form responses, etc.).

CREATE TABLE text_templates (
    id              UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id          UUID,        -- NULL = system-wide (visible to all orgs)
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    template_type   VARCHAR(20)  NOT NULL DEFAULT 'MARKDOWN',
    content         TEXT         NOT NULL,
    output_formats  JSONB        NOT NULL DEFAULT '["PPTX"]',
    data_bindings   JSONB        NOT NULL DEFAULT '{"bindings":[]}',
    scope           VARCHAR(20)  NOT NULL DEFAULT 'CENTRAL',
    is_system       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    version         INTEGER      NOT NULL DEFAULT 1,
    created_by      VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_text_template_org_name UNIQUE (org_id, name)
);

CREATE INDEX idx_text_tmpl_org_id   ON text_templates (org_id);
CREATE INDEX idx_text_tmpl_system   ON text_templates (is_system);
CREATE INDEX idx_text_tmpl_active   ON text_templates (is_active);
CREATE INDEX idx_text_tmpl_scope    ON text_templates (scope);

ALTER TABLE text_templates ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_text_tmpl_select ON text_templates
    FOR SELECT USING (org_id IS NULL OR org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_text_tmpl_insert ON text_templates
    FOR INSERT WITH CHECK (org_id::text = current_setting('app.current_org_id', TRUE));
CREATE POLICY rls_text_tmpl_update ON text_templates
    FOR UPDATE USING (org_id::text = current_setting('app.current_org_id', TRUE) AND is_system = FALSE);
CREATE POLICY rls_text_tmpl_delete ON text_templates
    FOR DELETE USING (org_id::text = current_setting('app.current_org_id', TRUE) AND is_system = FALSE);

GRANT SELECT, INSERT, UPDATE, DELETE ON text_templates TO engine_reporting_user;

-- Version history
CREATE TABLE text_template_versions (
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    template_id  UUID        NOT NULL REFERENCES text_templates(id) ON DELETE CASCADE,
    version      INTEGER     NOT NULL,
    content      TEXT        NOT NULL,
    data_bindings JSONB      NOT NULL DEFAULT '{"bindings":[]}',
    created_by   VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_text_tmpl_version UNIQUE (template_id, version)
);

CREATE INDEX idx_text_tmpl_ver_template ON text_template_versions (template_id);

GRANT SELECT, INSERT ON text_template_versions TO engine_reporting_user;

-- ─── Seed: System text templates ──────────────────────────────────────────────

INSERT INTO text_templates (id, org_id, name, description, template_type, content, output_formats, data_bindings, scope, is_system, created_by)
VALUES
(
    gen_random_uuid(),
    NULL,
    'ITSM Group Status Report',
    'Incident and Request status report for a resolver group. Works with ServiceNow ITSM data.',
    'MARKDOWN',
    E'# ITSM Status Report\n\n**Group:** {{GROUP_NAME}}  \n**Generated:** {{GENERATED_DATE}}\n\n---\n\n## Summary\n\n| Metric | Value |\n|--------|-------|\n| Open Incidents | {{OPEN_COUNT}} |\n| SLA Breach Rate | {{SLA_BREACH_PCT}}% |\n| Avg Resolution Time | {{AVG_RESOLUTION_HRS}} hrs |\n\n---\n\n## Open Incidents by Priority\n\n{{PRIORITY_CHART}}\n\n---\n\n## Top 10 Oldest Open Incidents\n\n{{TOP10_INCIDENTS}}\n\n---\n\n## 7-Day Trend\n\n{{TREND_CHART}}\n',
    '["PPTX","EXCEL"]',
    '{"bindings":['
    || '{"placeholder":"{{OPEN_COUNT}}","type":"SCALAR","queryId":"__itsm_incident_open_count__","params":{"groupId":"{{input.groupId}}"},"label":"Open Incidents"},'
    || '{"placeholder":"{{PRIORITY_CHART}}","type":"CHART","queryId":"__itsm_incident_priority_distribution__","params":{"groupId":"{{input.groupId}}"},"chartType":"PIE","label":"Priority Distribution"},'
    || '{"placeholder":"{{SLA_BREACH_PCT}}","type":"SCALAR","queryId":"__itsm_incident_sla_breach_rate__","params":{"groupId":"{{input.groupId}}"},"label":"SLA Breach %"},'
    || '{"placeholder":"{{TOP10_INCIDENTS}}","type":"TABLE","queryId":"__itsm_incident_top10_open__","params":{"groupId":"{{input.groupId}}"},"label":"Oldest Open Incidents"},'
    || '{"placeholder":"{{TREND_CHART}}","type":"CHART","queryId":"__itsm_incident_trend_7d__","params":{"groupId":"{{input.groupId}}"},"chartType":"LINE","label":"7-Day Trend"}'
    || ']}',
    'CENTRAL',
    TRUE,
    'system'
),
(
    gen_random_uuid(),
    NULL,
    'Universal Data Report',
    'Generic report template that works with ANY named query. Suitable for any data source.',
    'MARKDOWN',
    E'# {{REPORT_TITLE}}\n\n{{REPORT_DESCRIPTION}}\n\n---\n\n## Data\n\n{{MAIN_TABLE}}\n',
    '["PPTX","EXCEL"]',
    '{"bindings":['
    || '{"placeholder":"{{MAIN_TABLE}}","type":"TABLE","queryId":"__universal_query__","params":{},"label":"Data"}'
    || ']}',
    'CENTRAL',
    TRUE,
    'system'
);
