package com.xiaoding.javaai.knowledge.document.domain;

public record ActorId(String value) {
    public ActorId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("actorId must not be blank");
    }
}
