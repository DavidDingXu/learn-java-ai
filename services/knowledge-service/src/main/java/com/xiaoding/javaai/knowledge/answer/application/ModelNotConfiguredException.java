package com.xiaoding.javaai.knowledge.answer.application;

public final class ModelNotConfiguredException extends RuntimeException {

    public ModelNotConfiguredException() {
        super("No chat model is configured for the active runtime profile");
    }
}
