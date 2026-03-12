-- V1: Create search tables for full-text and vector search
-- This table stores searchable content with FTS and vector embeddings

-- Create search_index view/table that aggregates searchable content
-- Note: This is a base table; actual data comes from document and table sinks

CREATE TABLE IF NOT EXISTS search_index (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    
    -- Full-text search content
    title TEXT,
    content TEXT,
    search_vector tsvector,
    
    -- Vector search embedding (pgvector)
    embedding vector(1536),
    
    -- Metadata
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT uq_search_entity UNIQUE (entity_type, entity_id)
);

-- Create GIN index for full-text search
CREATE INDEX idx_search_fts_gin ON search_index USING GIN(search_vector);

-- Create index for org-based filtering
CREATE INDEX idx_search_org ON search_index(org_id);

-- Create index for entity type filtering
CREATE INDEX idx_search_entity_type ON search_index(entity_type);

-- Create index for vector similarity search (cosine)
CREATE INDEX idx_search_vector_cosine ON search_index USING ivfflat (embedding vector_cosine_ops);

-- Enable Row-Level Security
ALTER TABLE search_index ENABLE ROW LEVEL SECURITY;

-- Create RLS policy
CREATE POLICY "search_rls_policy" ON search_index
    FOR SELECT
    USING (org_id::text = current_setting('app.current_org_id', true));

COMMENT ON TABLE search_index IS 'Unified search index with FTS and vector embeddings';
COMMENT ON INDEX idx_search_fts_gin IS 'GIN index for full-text search using tsvector';
COMMENT ON INDEX idx_search_vector_cosine IS 'IVFFlat index for approximate nearest neighbor vector search';

-- Function to update search_vector automatically
CREATE OR REPLACE FUNCTION update_search_vector() RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := 
        setweight(to_tsvector('coalesce', NEW.title), 'A') ||
        setweight(to_tsvector('coalesce', NEW.content), 'B');
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to keep search_vector updated
CREATE TRIGGER trigger_update_search_vector
    BEFORE INSERT OR UPDATE ON search_index
    FOR EACH ROW
    EXECUTE FUNCTION update_search_vector();
