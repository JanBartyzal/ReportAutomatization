-- V1: Create sink tables for document storage
-- This migration creates tables for documents with pgVector support for embeddings

-- Enable UUID and vector extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- documents: stores unstructured document content
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id VARCHAR(255) NOT NULL,
    org_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,  -- SLIDE_TEXT, PDF_PAGE, NOTES
    content JSONB NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    CONSTRAINT uk_documents_file_id UNIQUE (file_id, document_type)
);

-- document_embeddings: stores vector embeddings for semantic search
CREATE TABLE document_embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    embedding vector(1536) NOT NULL,  -- OpenAI ada-002 dimension
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for efficient queries
CREATE INDEX idx_documents_org_id ON documents(org_id);
CREATE INDEX idx_documents_file_id ON documents(file_id);
CREATE INDEX idx_document_embeddings_document_id ON document_embeddings(document_id);

-- Index for vector similarity search (cosine distance)
CREATE INDEX idx_document_embeddings_cosine 
    ON document_embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- Grant permissions
GRANT SELECT, INSERT, UPDATE, DELETE ON documents TO ms_sink_doc;
GRANT SELECT, INSERT ON document_embeddings TO ms_sink_doc;

-- Comments
COMMENT ON TABLE documents IS 'Stores unstructured document content extracted from files';
COMMENT ON TABLE document_embeddings IS 'Stores vector embeddings for semantic search';
COMMENT ON COLUMN documents.content IS 'JSON content with text extracted from document';
