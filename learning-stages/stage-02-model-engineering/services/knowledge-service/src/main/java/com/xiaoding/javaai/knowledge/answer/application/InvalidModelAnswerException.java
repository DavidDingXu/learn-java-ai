package com.xiaoding.javaai.knowledge.answer.application;

public final class InvalidModelAnswerException extends RuntimeException {

    public InvalidModelAnswerException(String message) {
        super(message);
    }

    public InvalidModelAnswerException(String message, Throwable cause) {
        super(message, cause);
    }
}
