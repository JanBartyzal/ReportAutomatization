-- V11_0_1: Add storage_backend column to parsed_tables
-- Tracks which persistence backend was used to store each row.
-- Enables coexistence of POSTGRES and SPARK backends during migration.

ALTER TABLE parsed_tables
    ADD COLUMN IF NOT EXISTS storage_backend VARCHAR(20) NOT NULL DEFAULT 'POSTGRES';

COMMENT ON COLUMN parsed_tables.storage_backend
    IS 'Storage backend that persisted this row: POSTGRES (default) or SPARK';

CREATE INDEX IF NOT EXISTS idx_parsed_tables_storage_backend
    ON parsed_tables (storage_backend);
