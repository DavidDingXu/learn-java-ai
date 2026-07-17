ALTER TABLE document_version
    ADD COLUMN published_by VARCHAR(160),
    ADD COLUMN published_at TIMESTAMPTZ;
