CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_document (
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    revision BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, document_id),
    CHECK (revision >= 0)
);

CREATE TABLE document_version (
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    version_number INTEGER NOT NULL,
    content_hash VARCHAR(80) NOT NULL,
    object_key VARCHAR(900) NOT NULL,
    media_type VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    effective_from TIMESTAMPTZ,
    effective_until TIMESTAMPTZ,
    created_by VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, document_id, version_number),
    UNIQUE (tenant_id, document_id, version_number),
    UNIQUE (tenant_id, document_id, content_hash),
    FOREIGN KEY (tenant_id, document_id)
        REFERENCES knowledge_document (tenant_id, document_id),
    CHECK (version_number > 0),
    CHECK (effective_until IS NULL OR effective_from IS NULL OR effective_until > effective_from)
);

CREATE UNIQUE INDEX ux_document_version_single_published
    ON document_version (tenant_id, document_id)
    WHERE status = 'PUBLISHED';

CREATE TABLE document_chunk (
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    version_number INTEGER NOT NULL,
    chunk_id VARCHAR(80) NOT NULL,
    chunk_policy_version VARCHAR(80) NOT NULL,
    ordinal INTEGER NOT NULL,
    heading_path JSONB NOT NULL DEFAULT '[]'::jsonb,
    clause VARCHAR(160),
    content TEXT NOT NULL,
    embedding VECTOR(1536),
    embedding_model VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, chunk_id),
    UNIQUE (tenant_id, document_id, version_number, ordinal),
    FOREIGN KEY (tenant_id, document_id, version_number)
        REFERENCES document_version (tenant_id, document_id, version_number)
        ON DELETE CASCADE,
    CHECK (ordinal > 0),
    CHECK (char_length(content) > 0)
);

CREATE INDEX ix_document_chunk_embedding_hnsw
    ON document_chunk USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE TABLE document_acl (
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_id VARCHAR(160) NOT NULL,
    permission VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, document_id, subject_type, subject_id, permission),
    FOREIGN KEY (tenant_id, document_id)
        REFERENCES knowledge_document (tenant_id, document_id)
        ON DELETE CASCADE
);

CREATE INDEX ix_document_acl_lookup
    ON document_acl (tenant_id, subject_type, subject_id, document_id);

CREATE TABLE index_task (
    task_id UUID PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    version_number INTEGER NOT NULL,
    task_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    lease_owner VARCHAR(160),
    lease_until TIMESTAMPTZ,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    error_code VARCHAR(80),
    error_message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (tenant_id, document_id, version_number, task_type),
    FOREIGN KEY (tenant_id, document_id, version_number)
        REFERENCES document_version (tenant_id, document_id, version_number)
        ON DELETE CASCADE,
    CHECK (attempts >= 0)
);

CREATE INDEX ix_index_task_claim
    ON index_task (status, next_attempt_at, lease_until, created_at);

CREATE TABLE rag_feedback (
    feedback_id UUID PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    question_hash VARCHAR(80) NOT NULL,
    answer_hash VARCHAR(80) NOT NULL,
    rating SMALLINT NOT NULL,
    reason_code VARCHAR(80),
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (rating BETWEEN -1 AND 1)
);

CREATE INDEX ix_rag_feedback_trace
    ON rag_feedback (tenant_id, trace_id, created_at DESC);
