-- P6-W2-001: Add scope support to MS-QRY for multi-scope queries
-- Scope: CENTRAL (default), LOCAL (subsidiary-managed)

-- =============================================================================
-- ADD SCOPE COLUMN TO BASE TABLES (backward-compatible default)
-- =============================================================================

ALTER TABLE parsed_tables ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'CENTRAL';
ALTER TABLE documents ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'CENTRAL';
ALTER TABLE files ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'CENTRAL';

-- Indexes for scope-based queries
CREATE INDEX IF NOT EXISTS idx_parsed_tables_scope ON parsed_tables (scope);
CREATE INDEX IF NOT EXISTS idx_parsed_tables_org_scope ON parsed_tables (org_id, scope);
CREATE INDEX IF NOT EXISTS idx_documents_scope ON documents (scope);
CREATE INDEX IF NOT EXISTS idx_documents_org_scope ON documents (org_id, scope);
CREATE INDEX IF NOT EXISTS idx_files_scope ON files (scope);
CREATE INDEX IF NOT EXISTS idx_files_org_scope ON files (org_id, scope);

-- =============================================================================
-- NEW MATERIALIZED VIEW: Scope-aware file summary
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_file_summary_scoped AS
SELECT
    f.id AS file_id,
    f.org_id,
    f.scope,
    f.user_id,
    f.filename,
    f.size_bytes,
    f.mime_type,
    f.scan_status,
    f.upload_purpose,
    f.created_at,
    f.updated_at,
    pl.latest_step,
    pl.latest_status,
    pl.latest_step_at,
    COALESCE(pt.table_count, 0) AS table_count,
    COALESCE(d.document_count, 0) AS document_count
FROM files f
LEFT JOIN LATERAL (
    SELECT step_name AS latest_step, status AS latest_status, created_at AS latest_step_at
    FROM processing_logs
    WHERE file_id = f.id::text
    ORDER BY created_at DESC
    LIMIT 1
) pl ON true
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS table_count FROM parsed_tables WHERE file_id = f.id::text
) pt ON true
LEFT JOIN LATERAL (
    SELECT COUNT(*) AS document_count FROM documents WHERE file_id = f.id::text
) d ON true;

CREATE UNIQUE INDEX idx_mv_file_summary_scoped_pk ON mv_file_summary_scoped(file_id);
CREATE INDEX idx_mv_file_summary_scoped_org_scope ON mv_file_summary_scoped(org_id, scope);

-- =============================================================================
-- NEW MATERIALIZED VIEW: Scope aggregation summary for dashboards
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_scope_summary AS
SELECT
    org_id,
    scope,
    COUNT(*) AS file_count,
    SUM(size_bytes) AS total_size_bytes,
    COUNT(CASE WHEN scan_status = 'CLEAN' THEN 1 END) AS clean_count
FROM files
GROUP BY org_id, scope;

CREATE UNIQUE INDEX idx_mv_scope_summary_pk ON mv_scope_summary(org_id, scope);

-- =============================================================================
-- NOTE: RLS cannot be applied to materialized views in PostgreSQL.
-- Access control for materialized views is handled at the application layer
-- via the app.current_org_id session variable in queries.
-- =============================================================================

-- =============================================================================
-- UPDATE REFRESH FUNCTION (include new views)
-- =============================================================================

CREATE OR REPLACE FUNCTION refresh_query_views()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_file_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_org_tables;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_file_type_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_processing_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_file_summary_scoped;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_scope_summary;
END;
$$;

-- =============================================================================
-- PERMISSIONS
-- =============================================================================

GRANT SELECT ON mv_file_summary_scoped TO engine_data_user;
GRANT SELECT ON mv_scope_summary TO engine_data_user;
GRANT EXECUTE ON FUNCTION refresh_query_views() TO engine_data_user;
