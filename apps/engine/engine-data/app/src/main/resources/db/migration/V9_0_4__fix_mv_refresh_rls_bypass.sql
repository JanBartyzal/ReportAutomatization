-- V9_0_4: Fix materialized view refresh blocked by FORCE ROW LEVEL SECURITY
--
-- Problem: refresh_query_views() runs as engine_data_user (table owner).
-- With FORCE ROW LEVEL SECURITY on parsed_tables and documents, even the
-- owner is subject to RLS. Since app.current_org_id is not set during MV
-- refresh, the lateral subqueries return 0 rows → stale/empty MVs.
--
-- Fix: Remove FORCE (keep ENABLE). The table owner (engine_data_user) now
-- bypasses RLS for internal operations like MV refresh, while non-owner
-- roles remain subject to RLS. Application-level security (X-Org-Id header,
-- orgId filtering in QueryService) provides the access control layer.

ALTER TABLE parsed_tables NO FORCE ROW LEVEL SECURITY;
ALTER TABLE documents NO FORCE ROW LEVEL SECURITY;

-- Also make refresh_query_views() SECURITY DEFINER so it always has
-- owner-level access regardless of the calling role.
ALTER FUNCTION refresh_query_views() SECURITY DEFINER;

-- Some materialized views were missing UNIQUE indexes, which are required
-- for REFRESH MATERIALIZED VIEW CONCURRENTLY.
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_org_tables_pk ON mv_org_tables(table_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_processing_stats_pk ON mv_processing_stats(org_id, processing_date, step_name);
