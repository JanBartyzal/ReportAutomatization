-- V5_0_3: Fix RLS policy for dashboards to allow access to public dashboards.
-- The original policy blocked all reads unless org_id matched current_org_id,
-- which prevented viewers from accessing dashboards marked is_public = true.

ALTER TABLE dashboards DISABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS dashboards_org_isolation ON dashboards;

-- New policy: allow rows where org_id matches the current org context,
-- OR the dashboard is explicitly marked as public (readable by any authenticated user).
CREATE POLICY dashboards_org_isolation ON dashboards
    USING (
        org_id = current_setting('app.current_org_id', true)::UUID
        OR is_public = true
    )
    WITH CHECK (
        org_id = current_setting('app.current_org_id', true)::UUID
    );

ALTER TABLE dashboards ENABLE ROW LEVEL SECURITY;
ALTER TABLE dashboards FORCE ROW LEVEL SECURITY;
