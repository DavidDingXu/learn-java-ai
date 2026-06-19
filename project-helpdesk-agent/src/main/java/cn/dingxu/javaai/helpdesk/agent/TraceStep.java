package cn.dingxu.javaai.helpdesk.agent;

import java.util.Map;

public record TraceStep(String name, Map<String, Object> attributes) {
    public TraceStep {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("trace step name must not be blank");
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
