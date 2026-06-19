package cn.dingxu.javaai.enterprise.rag;

public record RagTraceStep(String name, String detail) {
    public RagTraceStep {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("trace step name must not be blank");
        }
        detail = detail == null ? "" : detail;
    }
}
