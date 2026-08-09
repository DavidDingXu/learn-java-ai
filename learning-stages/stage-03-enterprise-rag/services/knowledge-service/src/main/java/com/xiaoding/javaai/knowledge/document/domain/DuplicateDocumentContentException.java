package com.xiaoding.javaai.knowledge.document.domain;

public final class DuplicateDocumentContentException extends RuntimeException {
    DuplicateDocumentContentException(ContentHash contentHash) {
        super("document already contains content " + contentHash.value());
    }
}
