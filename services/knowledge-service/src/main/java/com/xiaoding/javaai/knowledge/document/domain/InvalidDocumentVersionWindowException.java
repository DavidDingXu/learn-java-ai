package com.xiaoding.javaai.knowledge.document.domain;

public final class InvalidDocumentVersionWindowException extends RuntimeException {
    InvalidDocumentVersionWindowException() {
        super("effectiveUntil must be after effectiveFrom");
    }
}
