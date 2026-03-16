-- V1: Create materialized views for MS-QRY read model
-- These views aggregate data from sink tables for efficient querying

-- Materialized View: File summary with processing status
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_file_summary AS
SELECT
    f.id AS file_id,
    f.org_id,
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

CREATE UNIQUE INDEX idx_mv_file_summary_file_id ON mv_file_summary(file_id);
CREATE INDEX idx_mv_file_summary_org_id ON mv_file_summary(org_id);

-- Materialized View: Cross-file table data for org dashboards
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_org_tables AS
SELECT
    pt.id AS table_id,
    pt.file_id,
    pt.org_id,
    pt.source_sheet,
    pt.headers,
    pt.metadata,
    pt.created_at,
    f.filename,
    f.mime_type
FROM parsed_tables pt
LEFT JOIN files f ON pt.file_id = f.id::text;

CREATE INDEX idx_mv_org_tables_org_id ON mv_org_tables(org_id);
CREATE INDEX idx_mv_org_tables_file_id ON mv_org_tables(file_id);

-- Function to refresh materialized views (called after new data arrives)
CREATE OR REPLACE FUNCTION refresh_query_views()
RETURNS void
LANGUAGE plpgsql
AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_file_summary;
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_org_tables;
END;
$$;

-- Grant read permissions to engine_data_user user
GRANT SELECT ON mv_file_summary TO engine_data_user;
GRANT SELECT ON mv_org_tables TO engine_data_user;
GRANT SELECT ON parsed_tables TO engine_data_user;
GRANT SELECT ON documents TO engine_data_user;
GRANT SELECT ON processing_logs TO engine_data_user;
GRANT SELECT ON files TO engine_data_user;
GRANT EXECUTE ON FUNCTION refresh_query_views() TO engine_data_user;
