-- V4: Feature flags configuration for P6-W3-002
-- Controls local scope and advanced comparison features

-- Create feature_flags table
CREATE TABLE IF NOT EXISTS feature_flags (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key       VARCHAR(100) NOT NULL UNIQUE,
    flag_name      VARCHAR(255) NOT NULL,
    description    TEXT,
    is_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    enabled_for    JSONB NOT NULL DEFAULT '[]'::jsonb,  -- List of org_ids where enabled
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(255) DEFAULT 'system'
);

-- Index for querying enabled flags
CREATE INDEX IF NOT EXISTS idx_feature_flags_enabled ON feature_flags (is_enabled) WHERE is_enabled = TRUE;

-- Seed feature flags (all disabled by default as per P6-W3-002)
INSERT INTO feature_flags (flag_key, flag_name, description, is_enabled, created_by) VALUES
    ('ENABLE_LOCAL_SCOPE', 'Local Scope Feature', 'Enable subsidiary self-service: local forms, templates, and reports', FALSE, 'system'),
    ('ENABLE_ADVANCED_COMPARISON', 'Advanced Period Comparison', 'Enable multi-period comparison and trend analysis features', FALSE, 'system')
ON CONFLICT (flag_key) DO NOTHING;

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON feature_flags TO ms_lifecycle;
GRANT SELECT ON feature_flags TO ms_qry;
GRANT SELECT ON feature_flags TO ms_form;
GRANT SELECT ON feature_flags TO ms_tmpl_pptx;

-- Comments
COMMENT ON TABLE feature_flags IS 'Feature flags for controlling experimental and rollout features';
COMMENT ON COLUMN feature_flags.flag_key IS 'Unique identifier: ENABLE_LOCAL_SCOPE, ENABLE_ADVANCED_COMPARISON';
COMMENT ON COLUMN feature_flags.is_enabled IS 'Global enable/disable switch';
COMMENT ON COLUMN feature_flags.enabled_for IS 'Org-specific override: list of org_ids where feature is enabled even if global is disabled';
