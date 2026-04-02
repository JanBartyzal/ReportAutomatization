-- Add period_id column to link batches to reporting periods (FS08 + FS20)
-- period_id references periods table in engine-reporting DB (cross-service, no FK constraint)
ALTER TABLE batches ADD COLUMN IF NOT EXISTS period_id UUID;
