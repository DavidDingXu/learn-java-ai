package cn.dingxu.javaai.agent.service.context;

import java.util.Map;

public record ContextSlice(
        ContextSliceType type,
        String source,
        String content,
        int estimatedTokens,
        Map<String, Object> attributes
) {
    public ContextSlice {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        content = content == null ? "" : content;
        estimatedTokens = Math.max(1, estimatedTokens);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
