package com.xiaoding.javaai.knowledge.document.application;

public final class UnsupportedDocumentMediaTypeException extends RuntimeException {
    UnsupportedDocumentMediaTypeException(String mediaType) {
        super("unsupported document media type " + mediaType);
    }
}
