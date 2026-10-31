-- Enable the pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop table if resetting environment
DROP TABLE IF EXISTS financial_document_embeddings;

-- Create the vector store table matching Spring AI's schema requirements (3072 dimensions)
CREATE TABLE financial_document_embeddings (
    id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(3072)
);

-- Optional: Create a GIN index on metadata for high-performance tenant/application filtering
CREATE INDEX IF NOT EXISTS idx_financial_docs_metadata_app_id
ON financial_document_embeddings USING gin ((metadata->'applicationId'));
