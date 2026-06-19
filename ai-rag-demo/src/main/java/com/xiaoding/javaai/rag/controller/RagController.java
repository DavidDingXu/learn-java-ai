package com.xiaoding.javaai.rag.controller;

import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.RagAnswer;
import com.xiaoding.javaai.rag.service.RagRetrievalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagRetrievalService ragRetrievalService;

    public RagController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @PostMapping("/answer")
    public RagAnswer answer(@RequestBody RagQuestionRequest request) {
        return ragRetrievalService.answer(
                request.query(),
                new OperatorScope(request.tenantId(), request.department())
        );
    }

    public record RagQuestionRequest(String query, String tenantId, String department) {
    }
}
