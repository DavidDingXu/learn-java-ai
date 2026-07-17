package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.application.port.DocumentContentParser;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.domain.ContentHash;
import com.xiaoding.javaai.knowledge.document.domain.DocumentVersion;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

public final class DocumentUploadService {

    static final int MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_MEDIA_TYPES = Set.of("text/markdown", "text/plain");

    private final KnowledgeDocumentRepository repository;
    private final DocumentObjectStore objectStore;
    private final DocumentContentParser parser;

    public DocumentUploadService(
            KnowledgeDocumentRepository repository,
            DocumentObjectStore objectStore,
            DocumentContentParser parser
    ) {
        this.repository = repository;
        this.objectStore = objectStore;
        this.parser = parser;
    }

    public UploadedDocument upload(UploadDocumentCommand command) {
        byte[] content = command.content();
        if (content.length > MAX_DOCUMENT_BYTES) {
            throw new DocumentTooLargeException(content.length, MAX_DOCUMENT_BYTES);
        }
        if (!ALLOWED_MEDIA_TYPES.contains(command.mediaType())) {
            throw new UnsupportedDocumentMediaTypeException(command.mediaType());
        }

        parser.parse(command.mediaType(), content);
        ContentHash contentHash = sha256(content);
        KnowledgeDocument document = repository.find(command.tenantId(), command.documentId())
                .orElseGet(() -> KnowledgeDocument.create(
                        command.documentId(), command.tenantId(), command.title(), command.actorId(), command.uploadedAt()
                ));
        ObjectKey objectKey = objectKey(command, contentHash);
        DocumentVersion version = document.addVersion(
                command.expectedRevision(),
                contentHash,
                objectKey,
                command.mediaType(),
                command.actorId(),
                command.uploadedAt()
        );
        objectStore.put(objectKey, command.mediaType(), content);
        repository.save(document);
        return new UploadedDocument(
                document.id(), document.tenantId(), version.number(), document.revision(), contentHash, objectKey
        );
    }

    private static ObjectKey objectKey(UploadDocumentCommand command, ContentHash contentHash) {
        String suffix = command.mediaType().equals("text/markdown") ? ".md" : ".txt";
        String hash = contentHash.value().substring("sha256:".length());
        return new ObjectKey(command.tenantId().value()
                + "/knowledge/"
                + command.documentId().value()
                + "/"
                + hash
                + suffix);
    }

    private static ContentHash sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return new ContentHash("sha256:" + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}
