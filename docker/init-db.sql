-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_documents_tenant ON documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_document ON document_chunks(document_id);

-- Create vector index for similarity search
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
ON document_chunks USING hnsw (embedding vector_cosine_ops);
