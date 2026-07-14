CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX ix_document_chunk_content_trgm
    ON document_chunk USING gin (content gin_trgm_ops);
