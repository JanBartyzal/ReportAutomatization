-- V2: Create promotion candidates table for smart persistence detection
-- Tracks mapping templates that are candidates for promotion to dedicated tables

CREATE TABLE IF NOT EXISTS promotion_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mapping_template_id UUID NOT NULL,
    org_id UUID,
    status VARCHAR(30) NOT NULL DEFAULT 'CANDIDATE'
        CHECK (status IN ('CANDIDATE', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'CREATED', 'MIGRATING', 'ACTIVE')),
    usage_count BIGINT NOT NULL,
    proposed_table_name VARCHAR(255) NOT NULL,
    proposed_ddl TEXT NOT NULL,
    proposed_indexes TEXT,
    column_type_analysis JSONB,
    reviewed_by VARCHAR(255),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    final_ddl TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_promo_status ON promotion_candidates(status);
CREATE INDEX idx_promo_mapping ON promotion_candidates(mapping_template_id);

-- Comments
COMMENT ON TABLE promotion_candidates IS 'Candidates for promotion from JSONB to dedicated tables based on usage patterns';
COMMENT ON COLUMN promotion_candidates.status IS 'Lifecycle: CANDIDATE -> PENDING_REVIEW -> APPROVED -> CREATED -> MIGRATING -> ACTIVE (or REJECTED)';
COMMENT ON COLUMN promotion_candidates.proposed_ddl IS 'Auto-generated DDL proposal based on JSONB column type analysis';
COMMENT ON COLUMN promotion_candidates.column_type_analysis IS 'JSON analysis of detected column types from sample data';
