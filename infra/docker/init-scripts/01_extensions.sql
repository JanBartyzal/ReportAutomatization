-- PostgreSQL Initialization Script
-- Creates extensions and base configuration for ReportAutomatization
-- Note: Schemas, users, and RLS functions are created in 09_p8_consolidated_users.sql

-- Enable pgVector extension for document embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable hstore for key-value storage
CREATE EXTENSION IF NOT EXISTS hstore;

-- Set timezone
SET timezone = 'UTC';

-- Log successful initialization
DO $$ BEGIN RAISE NOTICE 'PostgreSQL extensions initialized successfully'; END $$;
