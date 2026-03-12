-- P6-W2-002: Add scope support and local report flow to MS-LIFECYCLE
-- Scope: CENTRAL (default, holding-managed), LOCAL (subsidiary-managed)

-- Add scope column with backward-compatible default
ALTER TABLE reports ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'CENTRAL';

-- Add local report completion fields
ALTER TABLE reports ADD COLUMN IF NOT EXISTS completed_by VARCHAR(255);
ALTER TABLE reports ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

-- Add release-to-central fields
ALTER TABLE reports ADD COLUMN IF NOT EXISTS released_by VARCHAR(255);
ALTER TABLE reports ADD COLUMN IF NOT EXISTS released_at TIMESTAMPTZ;

-- Update unique constraint to include scope
-- This allows the same org to have both a CENTRAL and LOCAL report for the same period/type
ALTER TABLE reports DROP CONSTRAINT IF EXISTS reports_org_id_period_id_report_type_key;
ALTER TABLE reports ADD CONSTRAINT reports_org_period_type_scope_key
    UNIQUE(org_id, period_id, report_type, scope);

-- Indexes for scope-based queries
CREATE INDEX IF NOT EXISTS idx_reports_scope ON reports (scope);
CREATE INDEX IF NOT EXISTS idx_reports_org_scope ON reports (org_id, scope);
CREATE INDEX IF NOT EXISTS idx_reports_period_scope ON reports (period_id, scope);
