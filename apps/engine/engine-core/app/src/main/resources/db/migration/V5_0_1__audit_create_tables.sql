-- V1: Audit tables for MS-AUDIT
-- All tables are append-only (immutable audit trail)

-- General audit log: who did what, when, where
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID,
    details         JSONB,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_org_id ON audit_logs (org_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_org_created ON audit_logs (org_id, created_at);

-- Read access log: tracks who viewed sensitive documents/reports
CREATE TABLE IF NOT EXISTS read_access_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    document_id     UUID         NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_read_access_org_id ON read_access_logs (org_id);
CREATE INDEX idx_read_access_user_id ON read_access_logs (user_id);
CREATE INDEX idx_read_access_document ON read_access_logs (document_id);
CREATE INDEX idx_read_access_created_at ON read_access_logs (created_at);

-- AI audit log: tracks all AI prompt/response interactions (prepared for FS12)
CREATE TABLE IF NOT EXISTS ai_audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id          UUID         NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    prompt_text     TEXT         NOT NULL,
    response_text   TEXT,
    model           VARCHAR(100),
    tokens_used     INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_ai_audit_org_id ON ai_audit_logs (org_id);
CREATE INDEX idx_ai_audit_user_id ON ai_audit_logs (user_id);
CREATE INDEX idx_ai_audit_created_at ON ai_audit_logs (created_at);

-- Row-Level Security
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY audit_logs_org_isolation ON audit_logs
    USING (org_id = rls.get_current_org_id());

ALTER TABLE read_access_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY read_access_logs_org_isolation ON read_access_logs
    USING (org_id = rls.get_current_org_id());

ALTER TABLE ai_audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY ai_audit_logs_org_isolation ON ai_audit_logs
    USING (org_id = rls.get_current_org_id());
