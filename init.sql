-- 1. Povolit vektory
CREATE EXTENSION IF NOT EXISTS vector;

-- 3. Index pro rychlé hledání (HNSW)
CREATE INDEX ON document_chunks USING hnsw (embedding vector_cosine_ops);