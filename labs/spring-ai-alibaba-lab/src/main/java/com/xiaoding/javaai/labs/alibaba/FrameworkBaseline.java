package com.xiaoding.javaai.labs.alibaba;

public record FrameworkBaseline(String framework, String version, String springBootLine) {
    public FrameworkBaseline {
        if (framework == null || framework.isBlank() || version == null || version.isBlank()
                || springBootLine == null || springBootLine.isBlank()) {
            throw new IllegalArgumentException("framework baseline fields must not be blank");
        }
    }
}
