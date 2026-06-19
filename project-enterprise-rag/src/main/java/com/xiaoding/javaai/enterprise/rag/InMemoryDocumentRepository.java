package com.xiaoding.javaai.enterprise.rag;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryDocumentRepository {

    private final Map<String, PolicyDocument> documents = new LinkedHashMap<>();

    public synchronized PolicyDocument saveNextVersion(PolicyDocumentUpload upload) {
        int nextVersion = documents.containsKey(upload.documentId())
                ? documents.get(upload.documentId()).version() + 1
                : 1;
        PolicyDocument document = new PolicyDocument(
                upload.documentId(),
                upload.tenantId(),
                upload.departments(),
                upload.type(),
                nextVersion,
                upload.content(),
                null
        );
        documents.put(document.documentId(), document);
        return document;
    }

    public synchronized PolicyDocument get(String documentId) {
        PolicyDocument document = documents.get(documentId);
        if (document == null) {
            throw new IllegalArgumentException("document not found: " + documentId);
        }
        return document;
    }
}
