-- V2: Add indexes for JSONB columns and RLS policies
-- Enhanced query performance and security for MS-QRY

-- =============================================================================
-- INDEXES FOR JSONB COLUMNS (common query patterns)
-- =============================================================================

-- Index on parsed_tables.metadata JSONB column for common queries
CREATE INDEX IF NOT EXISTS idx_parsed_tables_metadata ON parsed_tables USING gin (metadata jsonb_path_ops);

-- Index on parsed_tables.headers JSONB column
CREATE INDEX IF NOT EXISTS idx_parsed_tables_headers ON parsed_tables USING gin (headers jsonb_path_ops);

-- Index on documents.metadata JSONB column
CREATE INDEX IF NOT EXISTS idx_documents_metadata ON documents USING gin (metadata jsonb_path_ops);

-- Index on files.metadata JSONB column
CREATE INDEX IF NOT EXISTS idx_files_metadata ON files USING gin (metadata jsonb_path_ops);

-- Composite index for org-based queries on parsed_tables
CREATE INDEX IF NOT EXISTS idx_parsed_tables_org_created ON parsed_tables (org_id, created_at DESC);

-- Composite index for org-based queries on documents
CREATE INDEX IF NOT EXISTS idx_documents_org_created ON documents (org_id, created_at DESC);

-- Composite index for org-based queries on processing_logs
CREATE INDEX IF NOT EXISTS idx_processing_logs_org_created ON processing_logs (org_id, created_at DESC);

-- Index for file lookup by status
CREATE INDEX IF NOT EXISTS idx_files_scan_status ON files (scan_status);

-- Index for file lookup by mime_type
CREATE INDEX IF NOT EXISTS idx_files_mime_type ON files (mime_type);

-- =============================================================================
-- RLS POLICIES (Row-Level Security)
-- =============================================================================

-- Enable RLS on tables (if not already enabled)
ALTER TABLE IF EXISTS mv_file_summary ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS mv_org_tables ENABLE ROW LEVEL SECURITY;

-- RLS policy: Users can only see files from their organization
CREATE POLICY IF NOT EXISTS mv_file_summary_org_policy ON mv_file_summary
    FOR SELECT
    USING (org_id = current_setting('app.current_org_id', true)::text);

-- RLS policy: Users can only see tables from their organization
CREATE POLICY IF NOT EXISTS mv_org_tables_org_policy ON mv_org_tables
    FOR SELECT
    USING (org_id = current_setting('app.current_org_id', true)::text);

-- =============================================================================
-- ADDITIONAL MATERIALIZED VIEW: File type breakdown for dashboards
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_file_type_stats AS
SELECT
    org_id,
    mime_type,
    COUNT(*) AS file_count,
    SUM(size_bytes) AS total_size_bytes,
    COUNT(CASE WHEN scan_status = 'CLEAN' THEN 1 END) AS clean_count,
    COUNT(CASE WHEN scan_status = 'INFECTED' THEN 1 END) AS infected_count
FROM files
GROUP BY org_id, mime_type;

CREATE UNIQUE INDEX idx_mv_file_type_stats_org_mime ON mv_file_type_stats(org_id, mime_type);

-- =============================================================================
-- ADDITIONAL MATERIALIZED VIEW: Processing time analytics
-- =============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_processing_stats AS
SELECT
    f.org_id,
    DATE_TRUNC('day', pl.created_at) AS processing_date,
    pl.step_name,
    COUNT(*) AS step_count,
    AVG(EXTRACT(EPOCH FROM (pl.updated_at - pl.created_at))) AS avg_duration_seconds
FROM processing_logs pl
JOIN files f ON pl.file_id = f.id::text
GROUP BY f.org_id, DATE_TRUNC('day', pl.created_at), pl.step_name;

CREATE INDEX idx_mv_processing_stats_org_date ON mv_processing_stats (org_id, processing_date DESC);

-- =============================================================================
-- REFRESH FUNCTION UPDATE (include new views)
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
END;
$$;

-- Grant additional permissions
GRANT SELECT ON mv_file_type_stats TO ms_qry;
GRANT SELECT ON mv_processing_stats TO ms_qry;
GRANT EXECUTE ON FUNCTION refresh_query_views() TO ms_qry;

-- Grant RLS permissions
GRANT SELECT ON mv_file_summary TO ms_qry;
GRANT SELECT ON mv_org_tables TO ms_qry;
