-- V11_0_3: Add BLOB backend support to storage_routing_config
-- Expands the backend CHECK constraint to allow 'BLOB' in addition to 'POSTGRES' and 'SPARK'.
-- The BLOB path serialises table data as JSON and uploads directly to Azure Blob Storage.

-- Drop existing CHECK constraint and recreate with BLOB included
ALTER TABLE storage_routing_config
    DROP CONSTRAINT IF EXISTS storage_routing_config_backend_check;

ALTER TABLE storage_routing_config
    ADD CONSTRAINT storage_routing_config_backend_check
        CHECK (backend IN ('POSTGRES', 'SPARK', 'BLOB'));

-- Expand the storage_backend column on parsed_tables to allow 'BLOB'
-- (rows with BLOB backend will never be written to parsed_tables, but the column
--  is kept uniform for audit queries and future migrations)
ALTER TABLE parsed_tables
    DROP CONSTRAINT IF EXISTS parsed_tables_storage_backend_check;

COMMENT ON COLUMN storage_routing_config.backend IS
    'Target backend: POSTGRES (JSONB sink) | SPARK (Delta Lake via external pipeline) | BLOB (raw JSON in Azure Blob Storage)';
