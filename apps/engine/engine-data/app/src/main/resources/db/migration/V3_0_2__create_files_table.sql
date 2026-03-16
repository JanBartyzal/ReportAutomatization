-- Create files table for file tracking
-- Referenced by query materialized views in V4_0_1+

CREATE TABLE IF NOT EXISTS files (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    filename        VARCHAR(500) NOT NULL,
    size_bytes      BIGINT NOT NULL DEFAULT 0,
    mime_type       VARCHAR(100),
    scan_status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    upload_purpose  VARCHAR(50),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_files_org_id ON files (org_id);
CREATE INDEX IF NOT EXISTS idx_files_user_id ON files (user_id);
CREATE INDEX IF NOT EXISTS idx_files_created_at ON files (created_at DESC);

-- Enable RLS
ALTER TABLE files ENABLE ROW LEVEL SECURITY;

CREATE POLICY files_org_policy ON files
    FOR ALL
    USING (org_id::text = current_setting('app.current_org_id', true));
