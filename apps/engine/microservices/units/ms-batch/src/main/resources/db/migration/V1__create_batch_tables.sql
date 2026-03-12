-- V1__create_batch_tables.sql
-- Batch service database schema

-- Batches table for organizing file uploads by period/quarter
CREATE TABLE IF NOT EXISTS batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    period VARCHAR(50) NOT NULL,
    description TEXT,
    holding_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'COLLECTING', 'CLOSED')),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_batch_period_holding UNIQUE (period, holding_id)
);

CREATE INDEX idx_batches_holding ON batches(holding_id);
CREATE INDEX idx_batches_status ON batches(status);
CREATE INDEX idx_batches_period ON batches(period);

-- Batch files association table (for files assigned to batch)
CREATE TABLE IF NOT EXISTS batch_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    file_id UUID NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    added_by VARCHAR(255) NOT NULL,
    CONSTRAINT uk_batch_file UNIQUE (batch_id, file_id)
);

CREATE INDEX idx_batch_files_batch ON batch_files(batch_id);
CREATE INDEX idx_batch_files_file ON batch_files(file_id);

-- Period to batch mapping for OPEX reporting
CREATE TABLE IF NOT EXISTS periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('QUARTER', 'MONTH', 'YEAR')),
    holding_id UUID NOT NULL,
    batch_id UUID REFERENCES batches(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_periods_holding ON periods(holding_id);
CREATE INDEX idx_periods_code ON periods(code);
CREATE INDEX idx_periods_batch ON periods(batch_id);

-- Enable RLS on all tables (Row-Level Security)
-- Note: RLS policies will be managed at the database level
ALTER TABLE batches ENABLE ROW LEVEL SECURITY;
ALTER TABLE batch_files ENABLE ROW LEVEL SECURITY;
ALTER TABLE periods ENABLE ROW LEVEL SECURITY;
