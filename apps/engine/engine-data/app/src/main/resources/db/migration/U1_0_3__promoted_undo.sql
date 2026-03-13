-- U1_0_3__promoted_undo.sql
-- Reverses V1_0_3__sinktbl_create_promoted_tables_registry.sql
-- WARNING: This will delete all data in the affected tables

-- Drop indexes
DROP INDEX IF EXISTS idx_promoted_mapping;
DROP INDEX IF EXISTS idx_promoted_status;

-- Drop table
DROP TABLE IF EXISTS promoted_tables_registry CASCADE;
