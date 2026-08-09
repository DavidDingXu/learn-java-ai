package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.answer.application.InvalidModelAnswerException;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamChunk;
import com.xiaoding.javaai.knowledge.answer.application.ModelUsage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StreamingAnswerHeaderParserTest {

    private final StreamingAnswerHeaderParser parser =
            new StreamingAnswerHeaderParser(new ObjectMapper());

    @Test
    void parsesADecisionHeaderSplitAcrossProviderChunks() {
        ModelStreamChunk first = parser.parse(chunk(
                "<answer-deci", null, null
        ));
        ModelStreamChunk second = parser.parse(chunk(
                "sion>{\"citedSectionIds\":[\"arrival-time\"],\"refused\":false,"
                        + "\"refusalReason\":null}</answer-decision><answer-te",
                null, null
        ));
        ModelStreamChunk third = parser.parse(chunk(
                "xt>退款通常在 1 到 5 个工作日到账。", new ModelUsage(8, 8, 16), "stop"
        ));

        assertThat(first.delta()).isNull();
        assertThat(second.delta()).isNull();
        assertThat(third.decision().citedSectionIds()).containsExactly("arrival-time");
        assertThat(third.decision().refused()).isFalse();
        assertThat(third.delta()).isEqualTo("退款通常在 1 到 5 个工作日到账。");
        assertThat(third.usage()).isEqualTo(new ModelUsage(8, 8, 16));
    }

    @Test
    void rejectsTextBeforeTheDecisionHeader() {
        assertThatThrownBy(() -> parser.parse(chunk("先给答案", null, null)))
                .isInstanceOf(InvalidModelAnswerException.class)
                .hasMessage("model stream must start with a decision header");
    }

    @Test
    void rejectsANullDecisionPayload() {
        assertThatThrownBy(() -> parser.parse(chunk(
                "<answer-decision>null</answer-decision><answer-text>无法确认。",
                null,
                null
        )))
                .isInstanceOf(InvalidModelAnswerException.class)
                .hasMessage("model stream decision header is incomplete");
    }

    @Test
    void rejectsAnOversizedDecisionHeaderEvenWhenItArrivesInOneChunk() {
        String oversizedReason = "x".repeat(4100);

        assertThatThrownBy(() -> parser.parse(chunk(
                "<answer-decision>{\"citedSectionIds\":[],\"refused\":true,"
                        + "\"refusalReason\":\"" + oversizedReason
                        + "\"}</answer-decision><answer-text>无法确认。",
                null,
                null
        )))
                .isInstanceOf(InvalidModelAnswerException.class)
                .hasMessage("model stream decision header is too large");
    }

    private static ModelStreamChunk chunk(
            String delta,
            ModelUsage usage,
            String finishReason
    ) {
        return new ModelStreamChunk(delta, "fixture-model", usage, finishReason);
    }
}
