-- V1_0_2: Add macro_removed column to files table
-- Tracks whether macros were removed during security scanning

ALTER TABLE files ADD COLUMN IF NOT EXISTS macro_removed BOOLEAN NOT NULL DEFAULT false;
