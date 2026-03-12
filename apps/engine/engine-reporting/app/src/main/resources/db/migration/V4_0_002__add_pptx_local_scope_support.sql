-- V2: Extend PPTX templates for local scope support (P6-W1-002)
-- Adds SHARED_WITHIN_HOLDING scope, owner_org_id, and RLS policies.

-- 1. Expand scope CHECK constraint
ALTER TABLE pptx_templates DROP CONSTRAINT IF EXISTS pptx_templates_scope_check;
ALTER TABLE pptx_templates ADD CONSTRAINT pptx_templates_scope_check
    CHECK (scope IN ('CENTRAL', 'LOCAL', 'SHARED_WITHIN_HOLDING'));

-- 2. Add owner_org_id column
ALTER TABLE pptx_templates ADD COLUMN IF NOT EXISTS owner_org_id VARCHAR(255);

-- 3. Backfill owner_org_id from org_id
UPDATE pptx_templates SET owner_org_id = org_id WHERE owner_org_id IS NULL;

-- 4. Indexes
CREATE INDEX IF NOT EXISTS idx_pptx_templates_owner_org_id ON pptx_templates (owner_org_id);
CREATE INDEX IF NOT EXISTS idx_pptx_templates_scope_owner ON pptx_templates (scope, owner_org_id);

-- 5. Enable RLS
ALTER TABLE pptx_templates ENABLE ROW LEVEL SECURITY;

-- 6. RLS policy: same pattern as forms
CREATE POLICY pptx_templates_scope_access ON pptx_templates
    USING (
        current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        OR scope = 'CENTRAL'
        OR (scope = 'LOCAL' AND owner_org_id = current_setting('app.current_org_id', TRUE))
        OR (scope = 'SHARED_WITHIN_HOLDING'
            AND current_setting('app.current_role', TRUE) IN ('ADMIN', 'COMPANY_ADMIN'))
    );

ALTER TABLE pptx_templates FORCE ROW LEVEL SECURITY;
