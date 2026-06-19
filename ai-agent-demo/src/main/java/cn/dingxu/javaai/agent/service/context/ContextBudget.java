package cn.dingxu.javaai.agent.service.context;

public record ContextBudget(
        int maxEstimatedTokens,
        int maxConversationMessages
) {
    public ContextBudget {
        if (maxEstimatedTokens < 1) {
            throw new IllegalArgumentException("maxEstimatedTokens must be positive");
        }
        if (maxConversationMessages < 0) {
            throw new IllegalArgumentException("maxConversationMessages must not be negative");
        }
    }
}
