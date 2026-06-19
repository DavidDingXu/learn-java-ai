package com.xiaoding.javaai.helpdesk.agent;

import java.util.ArrayList;
import java.util.List;

class PolicyKnowledgeBase {

    private final List<PolicyDocument> documents = new ArrayList<>();

    void add(PolicyDocument document) {
        documents.add(document);
    }

    List<Citation> search(String question, OperatorContext operator) {
        String normalized = normalize(question);
        return documents.stream()
                .filter(document -> document.accessibleBy(operator))
                .filter(document -> matches(normalized, document.content()))
                .map(document -> new Citation(document.documentId(), document.content()))
                .toList();
    }

    private boolean matches(String normalizedQuestion, String content) {
        if (normalizedQuestion.isBlank()) {
            return false;
        }
        if (content.contains("退款") && normalizedQuestion.contains("退款")) {
            return true;
        }
        if (content.contains("薪资") && normalizedQuestion.contains("薪资")) {
            return true;
        }
        return false;
    }

    private String normalize(String question) {
        return question == null ? "" : question.replaceAll("\\s+", "");
    }
}
