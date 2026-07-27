package com.xiaoding.javaai.knowledge.answer.application;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class KnowledgeAnswerValidator {

    private static final int MAX_ANSWER_LENGTH = 1200;
    private static final List<String> UNSUPPORTED_ACTION_CLAIMS = List.of(
            "已经为你退款", "已为您退款", "已经创建工单", "已创建工单"
    );

    ModelAnswerDraft validate(ModelAnswerDraft draft, List<PolicyContext> contexts) {
        if (draft.answer() == null || draft.answer().isBlank()) {
            throw new InvalidModelAnswerException("model answer must not be blank");
        }
        if (draft.answer().length() > MAX_ANSWER_LENGTH) {
            throw new InvalidModelAnswerException("model answer exceeds 1200 characters");
        }
        if (draft.model() == null || draft.model().isBlank()) {
            throw new InvalidModelAnswerException("model metadata is missing");
        }
        if (draft.usage() == null || isEmpty(draft.usage())) {
            throw new InvalidModelAnswerException("model usage is missing");
        }
        validateDecision(
                draft.citedSectionIds(), draft.refused(), draft.refusalReason(), contexts
        );
        if (UNSUPPORTED_ACTION_CLAIMS.stream().anyMatch(draft.answer()::contains)) {
            throw new InvalidModelAnswerException("answer claims an unsupported business action");
        }
        return draft;
    }

    void validateDecision(ModelStreamDecision decision, List<PolicyContext> contexts) {
        if (decision == null) {
            throw new InvalidModelAnswerException("model stream decision is missing");
        }
        validateDecision(
                decision.citedSectionIds(), decision.refused(), decision.refusalReason(), contexts
        );
    }

    private void validateDecision(
            List<String> citedSectionIds,
            boolean refused,
            String refusalReason,
            List<PolicyContext> contexts
    ) {
        Set<String> availableSections = new HashSet<>();
        contexts.forEach(context -> availableSections.add(context.sectionId()));
        for (String citedSectionId : citedSectionIds) {
            if (!availableSections.contains(citedSectionId)) {
                throw new InvalidModelAnswerException("unknown citation section: " + citedSectionId);
            }
        }
        if (!refused && citedSectionIds.isEmpty()) {
            throw new InvalidModelAnswerException("a grounded answer must contain at least one citation");
        }
        if (refused && (refusalReason == null || refusalReason.isBlank())) {
            throw new InvalidModelAnswerException("a refused answer must contain a refusal reason");
        }
    }

    private static boolean isEmpty(ModelUsage usage) {
        return usage.promptTokens() == 0
                && usage.completionTokens() == 0
                && usage.totalTokens() == 0;
    }
}
