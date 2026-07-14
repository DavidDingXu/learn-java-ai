package com.xiaoding.javaai.labs.agentscope;

public record WorkUnit(String name, boolean independent, boolean sideEffect) {
    public WorkUnit {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
