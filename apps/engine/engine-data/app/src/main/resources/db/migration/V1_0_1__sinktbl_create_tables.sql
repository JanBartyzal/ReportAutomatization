-- V1: Create sink tables for table data storage
-- This migration creates tables for parsed tables and form responses

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- parsed_tables: stores structured table data extracted from files
CREATE TABLE parsed_tables (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id VARCHAR(255) NOT NULL,
    org_id VARCHAR(255) NOT NULL,
    source_sheet VARCHAR(255),
    headers JSONB NOT NULL DEFAULT '[]',
    rows JSONB NOT NULL DEFAULT '[]',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uk_parsed_tables_file_id UNIQUE (file_id, source_sheet)
);

-- form_responses: stores form submission data
CREATE TABLE form_responses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    org_id VARCHAR(255) NOT NULL,
    period_id VARCHAR(255) NOT NULL,
    form_version_id VARCHAR(255) NOT NULL,
    field_id VARCHAR(255) NOT NULL,
    value TEXT,
    data_type VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for form_responses queries
CREATE INDEX idx_form_responses_org_period_form 
    ON form_responses(org_id, period_id, form_version_id);

-- Index for file_id lookups
CREATE INDEX idx_parsed_tables_file_id ON parsed_tables(file_id);

-- Index for org_id queries
CREATE INDEX idx_parsed_tables_org_id ON parsed_tables(org_id);

-- Row-Level Security (RLS) - enabled per tenant
-- Note: RLS policies will be managed by the application based on org_id

-- Grant appropriate permissions
-- The application will use a specific user with appropriate RLS policies
GRANT SELECT, INSERT, UPDATE, DELETE ON parsed_tables TO engine_data_user;
GRANT SELECT, INSERT ON form_responses TO engine_data_user;

-- Comments for documentation
COMMENT ON TABLE parsed_tables IS 'Stores structured table data extracted from uploaded files (Excel, PPTX)';
COMMENT ON TABLE form_responses IS 'Stores form submission data for periodic reporting';
COMMENT ON COLUMN parsed_tables.headers IS 'JSON array of column header names';
COMMENT ON COLUMN parsed_tables.rows IS 'JSON array of row data, each row is an array of cell values';
COMMENT ON COLUMN parsed_tables.metadata IS 'Additional metadata about the table (e.g., table index, detected type)';
