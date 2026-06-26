-- M8A officially supports text-embedding-3-small (1536 dimensions only).
-- To support a different embedding model in the future, create a new migration.

CREATE EXTENSION IF NOT EXISTS vector;
-- NOTE: If this fails due to insufficient permissions on managed PostgreSQL
-- (AWS RDS, Supabase, etc.), stop immediately and report the error.
-- Do not proceed without vector extension successfully installed.

CREATE TABLE semantic_cache_entries (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    model               VARCHAR(255)    NOT NULL,
    embedding_model     VARCHAR(255)    NOT NULL,
    normalized_prompt   TEXT            NOT NULL,
    prompt_hash         VARCHAR(64)     NOT NULL,
    embedding           vector(1536)    NOT NULL,
    response_json       TEXT            NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP       NOT NULL,
    hit_count           INT             NOT NULL DEFAULT 0
);

-- Unique constraint: prevent duplicate entries for same prompt per tenant+model
ALTER TABLE semantic_cache_entries
    ADD CONSTRAINT uq_semantic_cache_prompt
    UNIQUE (tenant_id, model, embedding_model, prompt_hash);

-- Relational filter index
CREATE INDEX idx_semantic_cache_tenant_model
    ON semantic_cache_entries (tenant_id, model, embedding_model);

-- TTL eviction index
CREATE INDEX idx_semantic_cache_expires_at
    ON semantic_cache_entries (expires_at);

-- HNSW vector index
CREATE INDEX idx_semantic_cache_embedding
    ON semantic_cache_entries
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
