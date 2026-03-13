-- V1_0_1: Create files table for engine-ingestor
-- Includes Row-Level Security (RLS) setup for multi-tenant isolation

CREATE TABLE IF NOT EXISTS files (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID            NOT NULL,
    user_id         UUID            NOT NULL,
    filename        VARCHAR(512)    NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    mime_type       VARCHAR(128)    NOT NULL,
    blob_url        VARCHAR(2048),
    raw_blob_url    VARCHAR(2048),
    scan_status     VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    upload_purpose  VARCHAR(32)     NOT NULL DEFAULT 'PARSE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT chk_scan_status CHECK (scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'ERROR')),
    CONSTRAINT chk_upload_purpose CHECK (upload_purpose IN ('PARSE', 'FORM_IMPORT'))
);

-- Indexes for common query patterns
CREATE INDEX idx_files_org_id ON files (org_id);
CREATE INDEX idx_files_org_created ON files (org_id, created_at DESC);
CREATE INDEX idx_files_scan_status ON files (scan_status) WHERE scan_status = 'PENDING';

-- Row-Level Security
ALTER TABLE files ENABLE ROW LEVEL SECURITY;

-- Policy: users can only see files belonging to their organization
-- The org_id is expected to be set via SET LOCAL or a session variable
CREATE POLICY files_org_isolation ON files
    USING (org_id = current_setting('app.current_org_id', true)::UUID);

-- Force RLS for table owner as well
ALTER TABLE files FORCE ROW LEVEL SECURITY;
