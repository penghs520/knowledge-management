-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create vector_store table for Spring AI PgVectorStore
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT,
    metadata JSONB,
    embedding vector(768)
);

-- Create index for vector similarity search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- Initialize default tenant
INSERT INTO tenants (id, tenant_id, name, description, active, created_at, updated_at)
VALUES (1, 'default', 'Default Tenant', 'Default tenant for the system', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Initialize permissions
INSERT INTO permissions (id, name, description, resource, action, created_at, updated_at)
VALUES
    (1, 'DOCUMENT_READ', 'Read documents', 'document', 'read', NOW(), NOW()),
    (2, 'DOCUMENT_WRITE', 'Write/Upload documents', 'document', 'write', NOW(), NOW()),
    (3, 'DOCUMENT_DELETE', 'Delete documents', 'document', 'delete', NOW(), NOW()),
    (4, 'KNOWLEDGE_QUERY', 'Query knowledge base', 'knowledge', 'query', NOW(), NOW()),
    (5, 'USER_MANAGE', 'Manage users', 'user', 'manage', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Initialize admin role for default tenant
INSERT INTO roles (id, name, description, tenant_id, created_at, updated_at)
VALUES (1, 'ADMIN', 'Administrator', 1, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Assign all permissions to admin role
INSERT INTO role_permissions (role_id, permission_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5)
ON CONFLICT DO NOTHING;

-- Create default admin user (password: admin123)
-- BCrypt hash for "admin123"
INSERT INTO users (id, username, email, password, full_name, tenant_id, active, created_at, updated_at)
VALUES (1, 'admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Administrator', 1, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Assign admin role to admin user
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1)
ON CONFLICT DO NOTHING;
