package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.answer.application.InvalidModelAnswerException;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamChunk;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamDecision;

import java.util.List;

final class StreamingAnswerHeaderParser {

    static final String HEADER_START = "<answer-decision>";
    static final String ANSWER_START = "</answer-decision><answer-text>";
    private static final int MAX_HEADER_LENGTH = 4096;

    private final ObjectMapper objectMapper;
    private final StringBuilder pendingHeader = new StringBuilder();
    private boolean answerStarted;

    StreamingAnswerHeaderParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ModelStreamChunk parse(ModelStreamChunk chunk) {
        if (answerStarted || chunk.delta() == null || chunk.delta().isEmpty()) {
            return chunk;
        }

        pendingHeader.append(chunk.delta());
        String buffered = pendingHeader.toString();
        validateHeaderPrefix(buffered);
        int answerStart = buffered.indexOf(ANSWER_START, HEADER_START.length());
        if (answerStart < 0) {
            if (pendingHeader.length() > MAX_HEADER_LENGTH) {
                throw new InvalidModelAnswerException("model stream decision header is too large");
            }
            return metadataOnly(chunk);
        }
        if (answerStart > MAX_HEADER_LENGTH) {
            throw new InvalidModelAnswerException("model stream decision header is too large");
        }

        String decisionJson = buffered.substring(HEADER_START.length(), answerStart);
        DecisionPayload payload = readDecision(decisionJson);
        answerStarted = true;
        pendingHeader.setLength(0);
        String answer = buffered.substring(answerStart + ANSWER_START.length());
        return new ModelStreamChunk(
                answer.isEmpty() ? null : answer,
                new ModelStreamDecision(
                        payload.citedSectionIds(), payload.refused(), payload.refusalReason()
                ),
                chunk.model(),
                chunk.usage(),
                chunk.finishReason()
        );
    }

    private DecisionPayload readDecision(String json) {
        try {
            DecisionPayload payload = objectMapper.readValue(json, DecisionPayload.class);
            if (payload == null || payload.citedSectionIds() == null || payload.refused() == null) {
                throw new InvalidModelAnswerException("model stream decision header is incomplete");
            }
            return payload;
        } catch (JsonProcessingException error) {
            throw new InvalidModelAnswerException("model stream decision header is invalid");
        }
    }

    private static void validateHeaderPrefix(String buffered) {
        int comparableLength = Math.min(buffered.length(), HEADER_START.length());
        if (!HEADER_START.regionMatches(0, buffered, 0, comparableLength)) {
            throw new InvalidModelAnswerException("model stream must start with a decision header");
        }
    }

    private static ModelStreamChunk metadataOnly(ModelStreamChunk chunk) {
        return new ModelStreamChunk(
                null, null, chunk.model(), chunk.usage(), chunk.finishReason()
        );
    }

    private record DecisionPayload(
            List<String> citedSectionIds,
            Boolean refused,
            String refusalReason
    ) {
    }
}
