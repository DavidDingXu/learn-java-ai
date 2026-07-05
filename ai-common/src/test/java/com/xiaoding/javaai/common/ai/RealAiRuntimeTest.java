package com.xiaoding.javaai.common.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealAiRuntimeTest {

    @Test
    void rejectsPlaceholderApiKeys() {
        assertThat(RealAiRuntime.isConfigured("")).isFalse();
        assertThat(RealAiRuntime.isConfigured("demo-key")).isFalse();
        assertThat(RealAiRuntime.isConfigured("replace-with-your-api-key")).isFalse();
        assertThat(RealAiRuntime.isConfigured("sk-real-key")).isTrue();
    }

    @Test
    void throwsClearErrorWhenLiveAiIsNotConfigured() {
        assertThatThrownBy(() -> RealAiRuntime.requireConfigured("demo-key", "ai-output-demo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires real AI configuration")
                .hasMessageContaining("AI_API_KEY");
    }
}
