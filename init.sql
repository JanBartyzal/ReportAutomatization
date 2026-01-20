-- 1. Povolit vektory
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Tabulka pro Knowledge Base
CREATE TABLE document_chunks (
    id SERIAL PRIMARY KEY,
    content TEXT,                -- Zde bude tabulka v Markdownu: "| Col1 | Col2 |..."
    metadata JSONB,              -- { "filename": "report.pptx", "slide": 5, "user_id": "..." }
    embedding VECTOR(768)        -- 768 dimenzí (pro nomic-embed-text). OpenAI má 1536.
);

-- 3. Index pro rychlé hledání (HNSW)
CREATE INDEX ON document_chunks USING hnsw (embedding vector_cosine_ops);