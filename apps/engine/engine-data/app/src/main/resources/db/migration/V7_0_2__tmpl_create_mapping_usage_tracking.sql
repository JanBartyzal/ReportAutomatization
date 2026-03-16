-- V2: Create mapping usage tracking table for smart persistence detection
-- Tracks how often mapping templates are used per organization

CREATE TABLE IF NOT EXISTS mapping_usage_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mapping_template_id UUID NOT NULL,
    org_id UUID NOT NULL,
    usage_count BIGINT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP WITH TIME ZONE,
    distinct_org_count INTEGER DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(mapping_template_id, org_id)
);

CREATE INDEX idx_mapping_usage_template ON mapping_usage_tracking(mapping_template_id);
CREATE INDEX idx_mapping_usage_count ON mapping_usage_tracking(usage_count);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON mapping_usage_tracking TO engine_data_user;

-- Comments
COMMENT ON TABLE mapping_usage_tracking IS 'Tracks usage frequency of mapping templates per organization for promotion detection';
COMMENT ON COLUMN mapping_usage_tracking.usage_count IS 'Number of times this mapping template was used by this org';
COMMENT ON COLUMN mapping_usage_tracking.distinct_org_count IS 'Cached count of distinct orgs using this template';
