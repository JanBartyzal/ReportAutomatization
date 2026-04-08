-- V10_0_1: FS25 – Sink Browser, Manual Corrections & Extraction Learning
-- Creates three new tables: sink_corrections, sink_selections, extraction_learning_log

-- =============================================================================
-- 1. SINK_CORRECTIONS – overlay corrections on immutable parsed_tables
-- =============================================================================

CREATE TABLE sink_corrections (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parsed_table_id UUID NOT NULL REFERENCES parsed_tables(id) ON DELETE CASCADE,
    org_id          VARCHAR(255) NOT NULL,
    row_index       INT,              -- NULL = header correction
    col_index       INT,
    original_value  TEXT,
    corrected_value TEXT,
    correction_type VARCHAR(50) NOT NULL,  -- CELL_VALUE, HEADER_RENAME, ROW_DELETE, ROW_ADD, COLUMN_TYPE
    corrected_by    VARCHAR(255) NOT NULL,
    corrected_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata        JSONB DEFAULT '{}'
);

CREATE INDEX idx_sink_corrections_parsed_table ON sink_corrections(parsed_table_id);
CREATE INDEX idx_sink_corrections_org ON sink_corrections(org_id);
CREATE INDEX idx_sink_corrections_corrected_at ON sink_corrections(corrected_at DESC);

COMMENT ON TABLE sink_corrections IS 'Overlay corrections on immutable parsed_tables records (FS25)';
COMMENT ON COLUMN sink_corrections.row_index IS 'NULL indicates a header-level correction';
COMMENT ON COLUMN sink_corrections.correction_type IS 'CELL_VALUE | HEADER_RENAME | ROW_DELETE | ROW_ADD | COLUMN_TYPE';

-- =============================================================================
-- 2. SINK_SELECTIONS – which sinks are selected for report/dashboard output
-- =============================================================================

CREATE TABLE sink_selections (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parsed_table_id UUID NOT NULL REFERENCES parsed_tables(id) ON DELETE CASCADE,
    org_id          VARCHAR(255) NOT NULL,
    period_id       VARCHAR(255),
    report_type     VARCHAR(100),
    selected        BOOLEAN NOT NULL DEFAULT true,
    priority        INT NOT NULL DEFAULT 0,
    selected_by     VARCHAR(255),
    selected_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    note            TEXT,

    CONSTRAINT uk_sink_selection_table_period_type UNIQUE (parsed_table_id, period_id, report_type)
);

CREATE INDEX idx_sink_selections_org ON sink_selections(org_id);
CREATE INDEX idx_sink_selections_period ON sink_selections(period_id);
CREATE INDEX idx_sink_selections_selected ON sink_selections(org_id, period_id, selected) WHERE selected = true;

COMMENT ON TABLE sink_selections IS 'Tracks which parsed tables are selected for final report/dashboard output (FS25)';

-- =============================================================================
-- 3. EXTRACTION_LEARNING_LOG – feedback loop for AI extraction improvement
-- =============================================================================

CREATE TABLE extraction_learning_log (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id           VARCHAR(255),
    parsed_table_id   UUID REFERENCES parsed_tables(id) ON DELETE SET NULL,
    org_id            VARCHAR(255) NOT NULL,
    source_type       VARCHAR(50),       -- PPTX, EXCEL, PDF, CSV
    slide_index       INT,
    error_category    VARCHAR(50),       -- MERGED_CELLS, WRONG_HEADER, MISSING_ROW, VALUE_FORMAT, SPLIT_TABLE
    original_snippet  JSONB,
    corrected_snippet JSONB,
    confidence_score  FLOAT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    applied           BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_learning_log_org ON extraction_learning_log(org_id);
CREATE INDEX idx_learning_log_source_type ON extraction_learning_log(source_type);
CREATE INDEX idx_learning_log_error_category ON extraction_learning_log(error_category);
CREATE INDEX idx_learning_log_not_applied ON extraction_learning_log(applied) WHERE applied = false;
CREATE INDEX idx_learning_log_file ON extraction_learning_log(file_id);

COMMENT ON TABLE extraction_learning_log IS 'Stores extraction error feedback for AI learning pipeline (FS25)';
COMMENT ON COLUMN extraction_learning_log.error_category IS 'MERGED_CELLS | WRONG_HEADER | MISSING_ROW | VALUE_FORMAT | SPLIT_TABLE';
COMMENT ON COLUMN extraction_learning_log.applied IS 'Whether this correction has been used in retraining/rules';

-- =============================================================================
-- 4. RLS POLICIES
-- =============================================================================

-- sink_corrections RLS
ALTER TABLE sink_corrections ENABLE ROW LEVEL SECURITY;
ALTER TABLE sink_corrections FORCE ROW LEVEL SECURITY;

CREATE POLICY sink_corrections_org_isolation ON sink_corrections
    FOR SELECT
    USING (org_id = current_setting('app.current_org_id', true));

CREATE POLICY sink_corrections_insert ON sink_corrections
    FOR INSERT
    WITH CHECK (true);

CREATE POLICY sink_corrections_update ON sink_corrections
    FOR UPDATE
    USING (org_id = current_setting('app.current_org_id', true));

CREATE POLICY sink_corrections_delete ON sink_corrections
    FOR DELETE
    USING (org_id = current_setting('app.current_org_id', true));

-- sink_selections RLS
ALTER TABLE sink_selections ENABLE ROW LEVEL SECURITY;
ALTER TABLE sink_selections FORCE ROW LEVEL SECURITY;

CREATE POLICY sink_selections_org_isolation ON sink_selections
    FOR SELECT
    USING (org_id = current_setting('app.current_org_id', true));

CREATE POLICY sink_selections_insert ON sink_selections
    FOR INSERT
    WITH CHECK (true);

CREATE POLICY sink_selections_update ON sink_selections
    FOR UPDATE
    USING (org_id = current_setting('app.current_org_id', true));

CREATE POLICY sink_selections_delete ON sink_selections
    FOR DELETE
    USING (org_id = current_setting('app.current_org_id', true));

-- extraction_learning_log RLS
ALTER TABLE extraction_learning_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE extraction_learning_log FORCE ROW LEVEL SECURITY;

CREATE POLICY learning_log_org_isolation ON extraction_learning_log
    FOR SELECT
    USING (org_id = current_setting('app.current_org_id', true));

CREATE POLICY learning_log_insert ON extraction_learning_log
    FOR INSERT
    WITH CHECK (true);

-- =============================================================================
-- 5. GRANTS
-- =============================================================================

GRANT SELECT, INSERT, UPDATE, DELETE ON sink_corrections TO engine_data_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON sink_selections TO engine_data_user;
GRANT SELECT, INSERT ON extraction_learning_log TO engine_data_user;
