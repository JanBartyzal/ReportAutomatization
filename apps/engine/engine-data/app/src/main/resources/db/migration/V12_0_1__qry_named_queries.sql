-- V12_0_1: Named Query Catalog
-- Stores named, parameterized SQL queries used as data bindings for Text Templates
-- and as standalone data access API. Queries are DATA-SOURCE AGNOSTIC.

CREATE TABLE named_queries (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id           UUID,        -- NULL = system-wide query (visible to all orgs)
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    sql_query        TEXT         NOT NULL,
    params_schema    JSONB        NOT NULL DEFAULT '{}',
    data_source_hint VARCHAR(50)  NOT NULL DEFAULT 'PLATFORM',
    is_system        BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_named_query_org_name UNIQUE (org_id, name)
);

-- Indexes
CREATE INDEX idx_named_queries_org_id         ON named_queries (org_id);
CREATE INDEX idx_named_queries_is_system       ON named_queries (is_system);
CREATE INDEX idx_named_queries_data_source     ON named_queries (data_source_hint);
CREATE INDEX idx_named_queries_is_active       ON named_queries (is_active);

-- RLS: each org sees its own queries + system queries (org_id IS NULL)
ALTER TABLE named_queries ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_named_queries_select ON named_queries
    FOR SELECT
    USING (
        org_id IS NULL
        OR org_id::text = current_setting('app.current_org_id', TRUE)
    );

CREATE POLICY rls_named_queries_insert ON named_queries
    FOR INSERT
    WITH CHECK (
        org_id::text = current_setting('app.current_org_id', TRUE)
    );

CREATE POLICY rls_named_queries_update ON named_queries
    FOR UPDATE
    USING (
        org_id::text = current_setting('app.current_org_id', TRUE)
        AND is_system = FALSE
    );

CREATE POLICY rls_named_queries_delete ON named_queries
    FOR DELETE
    USING (
        org_id::text = current_setting('app.current_org_id', TRUE)
        AND is_system = FALSE
    );

-- Grant to app user
GRANT SELECT, INSERT, UPDATE, DELETE ON named_queries TO engine_data_user;

-- Seed: system-level sample named queries (visible to all orgs)
-- These serve as examples and can be used by system templates
INSERT INTO named_queries (id, org_id, name, description, sql_query, params_schema, data_source_hint, is_system, created_by)
VALUES
(
    gen_random_uuid(),
    NULL,
    'platform_files_summary',
    'Summary of all uploaded files for the current org',
    'SELECT filename, mime_type, size_bytes, created_at FROM files ORDER BY created_at DESC LIMIT :limit',
    '{"properties": {"limit": {"type": "integer", "default": 20}}, "required": []}',
    'PLATFORM',
    TRUE,
    'system'
),
(
    gen_random_uuid(),
    NULL,
    'platform_tables_by_file',
    'Parsed tables for a specific file',
    'SELECT id, source_sheet, created_at FROM parsed_tables WHERE file_id = :fileId ORDER BY created_at DESC',
    '{"properties": {"fileId": {"type": "string", "description": "File UUID"}}, "required": ["fileId"]}',
    'PLATFORM',
    TRUE,
    'system'
);
