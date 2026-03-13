-- V1_0_3__snow_create_distribution_tables.sql
-- Distribution rules and history tables

CREATE TABLE IF NOT EXISTS distribution_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    schedule_id UUID NOT NULL REFERENCES sync_schedules(id) ON DELETE CASCADE,
    report_template_id UUID NOT NULL,
    recipients TEXT[] NOT NULL DEFAULT '{}',
    format VARCHAR(10) NOT NULL DEFAULT 'XLSX',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE distribution_rules ENABLE ROW LEVEL SECURITY;
CREATE POLICY distribution_rules_org_policy ON distribution_rules
    USING (org_id = rls.get_current_org_id());

CREATE INDEX idx_dist_rules_org ON distribution_rules(org_id);
CREATE INDEX idx_dist_rules_schedule ON distribution_rules(schedule_id);

CREATE TABLE IF NOT EXISTS distribution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    rule_id UUID NOT NULL REFERENCES distribution_rules(id) ON DELETE SET NULL,
    recipients TEXT[] NOT NULL,
    report_blob_url TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE distribution_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY distribution_history_org_policy ON distribution_history
    USING (org_id = rls.get_current_org_id());

CREATE INDEX idx_dist_history_org ON distribution_history(org_id);
CREATE INDEX idx_dist_history_rule ON distribution_history(rule_id);
CREATE INDEX idx_dist_history_status ON distribution_history(status);
