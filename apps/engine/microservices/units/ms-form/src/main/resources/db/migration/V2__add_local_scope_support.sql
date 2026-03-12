-- V2: Extend forms for local scope support (P6-W1-001)
-- Adds SHARED_WITHIN_HOLDING scope, release tracking, and RLS policies.

-- 1. Expand scope CHECK constraint to include SHARED_WITHIN_HOLDING
ALTER TABLE forms DROP CONSTRAINT IF EXISTS forms_scope_check;
ALTER TABLE forms ADD CONSTRAINT forms_scope_check
    CHECK (scope IN ('CENTRAL', 'LOCAL', 'SHARED_WITHIN_HOLDING'));

-- 2. Add release tracking columns
ALTER TABLE forms ADD COLUMN IF NOT EXISTS released_at TIMESTAMPTZ;
ALTER TABLE forms ADD COLUMN IF NOT EXISTS released_by VARCHAR(255);

-- 3. Backfill owner_org_id from org_id where NULL
UPDATE forms SET owner_org_id = org_id WHERE owner_org_id IS NULL;

-- 4. Index for owner_org_id and scope-based queries
CREATE INDEX IF NOT EXISTS idx_forms_owner_org_id ON forms (owner_org_id);
CREATE INDEX IF NOT EXISTS idx_forms_scope_owner ON forms (scope, owner_org_id);

-- 5. Enable RLS on forms table
ALTER TABLE forms ENABLE ROW LEVEL SECURITY;

-- 6. RLS policy: scope-based access control
-- CENTRAL forms: visible to all users in the holding
-- LOCAL forms: visible only to users with matching org_id
-- SHARED_WITHIN_HOLDING: visible to all CompanyAdmins+ in same holding
-- HOLDING_ADMIN: read-only overview of all forms
CREATE POLICY forms_scope_access ON forms
    USING (
        -- HOLDING_ADMIN sees everything
        current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        -- CENTRAL forms visible to everyone in the holding
        OR scope = 'CENTRAL'
        -- LOCAL forms visible only to own org
        OR (scope = 'LOCAL' AND owner_org_id = current_setting('app.current_org_id', TRUE))
        -- SHARED forms visible to CompanyAdmins and above
        OR (scope = 'SHARED_WITHIN_HOLDING'
            AND current_setting('app.current_role', TRUE) IN ('ADMIN', 'COMPANY_ADMIN'))
    );

ALTER TABLE forms FORCE ROW LEVEL SECURITY;

-- 7. RLS on form_responses: inherit visibility from form's org context
ALTER TABLE form_responses ENABLE ROW LEVEL SECURITY;

CREATE POLICY form_responses_org_access ON form_responses
    USING (
        current_setting('app.current_role', TRUE) = 'HOLDING_ADMIN'
        OR org_id = current_setting('app.current_org_id', TRUE)
    );

ALTER TABLE form_responses FORCE ROW LEVEL SECURITY;
