-- V1: Versioning tables for MS-VER
-- Stores immutable version snapshots and cached diffs

-- Versions table: stores full JSONB snapshots of each entity version
CREATE TABLE IF NOT EXISTS versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       UUID         NOT NULL,
    version_number  INTEGER      NOT NULL,
    org_id          UUID         NOT NULL,
    snapshot_data   JSONB        NOT NULL,
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by      VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reason          TEXT,

    CONSTRAINT uq_version_entity UNIQUE (entity_type, entity_id, version_number)
);

-- Indexes for common query patterns
CREATE INDEX idx_versions_entity ON versions (entity_type, entity_id);
CREATE INDEX idx_versions_org_id ON versions (org_id);
CREATE INDEX idx_versions_created_at ON versions (created_at);
CREATE INDEX idx_versions_entity_latest ON versions (entity_type, entity_id, version_number DESC);

-- Version diffs table: caches computed diffs between version pairs
CREATE TABLE IF NOT EXISTS version_diffs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID        NOT NULL,
    from_version    INTEGER     NOT NULL,
    to_version      INTEGER     NOT NULL,
    diff_data       JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_version_diff UNIQUE (entity_type, entity_id, from_version, to_version)
);

CREATE INDEX idx_version_diffs_entity ON version_diffs (entity_type, entity_id);

-- Row-Level Security
ALTER TABLE versions ENABLE ROW LEVEL SECURITY;
CREATE POLICY versions_org_isolation ON versions
    USING (org_id = rls.get_current_org_id());

ALTER TABLE version_diffs ENABLE ROW LEVEL SECURITY;
CREATE POLICY version_diffs_org_isolation ON version_diffs
    USING (
        EXISTS (
            SELECT 1 FROM versions v
            WHERE v.entity_type = version_diffs.entity_type
              AND v.entity_id = version_diffs.entity_id
              AND v.org_id = rls.get_current_org_id()
            LIMIT 1
        )
    );
