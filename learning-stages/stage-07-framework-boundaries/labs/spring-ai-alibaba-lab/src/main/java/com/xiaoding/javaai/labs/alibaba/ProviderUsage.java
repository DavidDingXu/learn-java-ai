package com.xiaoding.javaai.labs.alibaba;

public record ProviderUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {

    public ProviderUsage {
        requireNonNegative(inputTokens, "inputTokens");
        requireNonNegative(outputTokens, "outputTokens");
        requireNonNegative(totalTokens, "totalTokens");
    }

    public static ProviderUsage unavailable() {
        return new ProviderUsage(null, null, null);
    }

    public boolean available() {
        return totalTokens != null;
    }

    private static void requireNonNegative(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
