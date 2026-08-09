package com.xiaoding.javaai.knowledge.document.infrastructure;

public final class InvalidObjectKeyException extends RuntimeException {
    InvalidObjectKeyException(String key) {
        super("object key escapes configured root: " + key);
    }
}
