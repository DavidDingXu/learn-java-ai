package com.xiaoding.javaai.knowledge.document.application;

public final class DocumentTooLargeException extends RuntimeException {
    DocumentTooLargeException(int actual, int maximum) {
        super("document size " + actual + " exceeds limit " + maximum);
    }
}
