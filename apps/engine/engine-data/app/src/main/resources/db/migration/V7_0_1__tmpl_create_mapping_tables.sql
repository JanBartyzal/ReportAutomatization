-- V1: Create mapping tables for template & schema mapping registry
-- Tables: mapping_templates, mapping_rules, mapping_history

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- mapping_templates: versioned mapping template definitions
CREATE TABLE mapping_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- mapping_rules: individual rules within a template
CREATE TABLE mapping_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    template_id UUID NOT NULL REFERENCES mapping_templates(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    source_pattern VARCHAR(500) NOT NULL,
    target_column VARCHAR(255) NOT NULL,
    confidence DOUBLE PRECISION DEFAULT 1.0,
    priority INT NOT NULL DEFAULT 0,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- mapping_history: successful mapping log for learning
CREATE TABLE mapping_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id VARCHAR(255) NOT NULL,
    source_column VARCHAR(500) NOT NULL,
    target_column VARCHAR(255) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    file_id VARCHAR(255),
    used_count INT NOT NULL DEFAULT 1,
    last_used_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT uk_mapping_history_org_src_tgt UNIQUE (org_id, source_column, target_column)
);

-- Indexes
CREATE INDEX idx_mapping_templates_org ON mapping_templates(org_id);
CREATE INDEX idx_mapping_templates_active ON mapping_templates(is_active);
CREATE INDEX idx_mapping_rules_template ON mapping_rules(template_id);
CREATE INDEX idx_mapping_rules_type ON mapping_rules(rule_type);
CREATE INDEX idx_mapping_history_org ON mapping_history(org_id);
CREATE INDEX idx_mapping_history_org_source ON mapping_history(org_id, source_column);
CREATE INDEX idx_mapping_history_used_count ON mapping_history(org_id, used_count DESC);

-- Permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON mapping_templates TO ms_tmpl;
GRANT SELECT, INSERT, UPDATE, DELETE ON mapping_rules TO ms_tmpl;
GRANT SELECT, INSERT, UPDATE, DELETE ON mapping_history TO ms_tmpl;

-- Comments
COMMENT ON TABLE mapping_templates IS 'Versioned mapping template definitions scoped to org or global';
COMMENT ON TABLE mapping_rules IS 'Individual mapping rules within a template (EXACT_MATCH, SYNONYM, REGEX, AI_SUGGESTED)';
COMMENT ON TABLE mapping_history IS 'Successful mapping log for learning-based auto-suggestions';
COMMENT ON COLUMN mapping_templates.org_id IS 'NULL means global template available to all orgs';
COMMENT ON COLUMN mapping_rules.priority IS 'Higher value = evaluated first in the rule chain';
COMMENT ON COLUMN mapping_rules.rule_type IS 'EXACT_MATCH, SYNONYM, REGEX, or AI_SUGGESTED';
COMMENT ON COLUMN mapping_history.used_count IS 'Number of times this mapping was successfully applied';
