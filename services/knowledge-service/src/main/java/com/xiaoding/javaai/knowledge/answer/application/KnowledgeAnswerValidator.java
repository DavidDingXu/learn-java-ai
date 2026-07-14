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
        if (draft.usage() == null) {
            throw new InvalidModelAnswerException("model usage is missing");
        }
        Set<String> availableSections = new HashSet<>();
        contexts.forEach(context -> availableSections.add(context.sectionId()));
        for (String citedSectionId : draft.citedSectionIds()) {
            if (!availableSections.contains(citedSectionId)) {
                throw new InvalidModelAnswerException("unknown citation section: " + citedSectionId);
            }
        }
        if (!draft.refused() && draft.citedSectionIds().isEmpty()) {
            throw new InvalidModelAnswerException("a grounded answer must contain at least one citation");
        }
        if (draft.refused() && (draft.refusalReason() == null || draft.refusalReason().isBlank())) {
            throw new InvalidModelAnswerException("a refused answer must contain a refusal reason");
        }
        if (UNSUPPORTED_ACTION_CLAIMS.stream().anyMatch(draft.answer()::contains)) {
            throw new InvalidModelAnswerException("answer claims an unsupported business action");
        }
        return draft;
    }
}
