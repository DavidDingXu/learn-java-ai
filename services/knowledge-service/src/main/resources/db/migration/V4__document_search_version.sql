CREATE TABLE document_search_version (
    tenant_id VARCHAR(128) NOT NULL,
    document_id VARCHAR(160) NOT NULL,
    version_number INTEGER NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, document_id),
    FOREIGN KEY (tenant_id, document_id, version_number)
        REFERENCES document_version (tenant_id, document_id, version_number)
        ON DELETE CASCADE
);

INSERT INTO document_search_version (tenant_id, document_id, version_number, activated_at)
SELECT dv.tenant_id, dv.document_id, dv.version_number, CURRENT_TIMESTAMP
  FROM document_version dv
 WHERE dv.status = 'PUBLISHED'
   AND EXISTS (
        SELECT 1
          FROM document_chunk dc
         WHERE dc.tenant_id = dv.tenant_id
           AND dc.document_id = dv.document_id
           AND dc.version_number = dv.version_number
   );
