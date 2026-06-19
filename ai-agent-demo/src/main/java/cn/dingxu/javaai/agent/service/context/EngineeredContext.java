package cn.dingxu.javaai.agent.service.context;

import java.util.List;

public record EngineeredContext(
        List<ContextSlice> slices,
        int totalEstimatedTokens,
        List<String> sources
) {
    public EngineeredContext {
        slices = slices == null ? List.of() : List.copyOf(slices);
        totalEstimatedTokens = Math.max(0, totalEstimatedTokens);
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
