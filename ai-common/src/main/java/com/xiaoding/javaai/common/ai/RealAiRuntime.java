package com.xiaoding.javaai.common.ai;

import java.util.Locale;

public final class RealAiRuntime {

    private RealAiRuntime() {
    }

    public static boolean isConfigured(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        String normalized = apiKey.trim().toLowerCase(Locale.ROOT);
        return !normalized.equals("demo-key")
                && !normalized.equals("replace-with-your-api-key")
                && !normalized.equals("your-api-key")
                && !normalized.equals("todo")
                && !normalized.equals("changeme");
    }

    public static void requireConfigured(String apiKey, String feature) {
        if (!isConfigured(apiKey)) {
            throw new IllegalStateException(feature + " requires real AI configuration: set AI_API_KEY, AI_BASE_URL and AI_CHAT_MODEL");
        }
    }
}
