package com.xiaoding.javaai.rag.controller;

import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.RagAnswer;
import com.xiaoding.javaai.rag.service.RagLabService;
import com.xiaoding.javaai.rag.service.RagRetrievalService;
import com.xiaoding.javaai.rag.service.IndexTaskResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagRetrievalService ragRetrievalService;
    private final RagLabService ragLabService;

    public RagController(RagRetrievalService ragRetrievalService, RagLabService ragLabService) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragLabService = ragLabService;
    }

    @PostMapping("/answer")
    public RagAnswer answer(@RequestBody RagQuestionRequest request) {
        return ragRetrievalService.answer(
                request.query(),
                new OperatorScope(request.tenantId(), request.department())
        );
    }

    @PostMapping("/lab/pipeline")
    public RagLabService.PipelineLabResult pipeline(@RequestBody RagLabService.PipelineLabRequest request) {
        return ragLabService.pipeline(request);
    }

    @PostMapping("/lab/access")
    public RagLabService.AccessLabResult access(@RequestBody RagLabService.AccessLabRequest request) {
        return ragLabService.access(request);
    }

    @PostMapping("/lab/retrieval")
    public RagLabService.RetrievalLabResult retrieval(@RequestBody RagLabService.RetrievalLabRequest request) {
        return ragLabService.retrieval(request);
    }

    @PostMapping("/lab/rewrite")
    public RagLabService.RewriteLabResult rewrite(@RequestBody RagLabService.RewriteLabRequest request) {
        return ragLabService.rewrite(request);
    }

    @PostMapping("/lab/index")
    public IndexTaskResult index(@RequestBody RagLabService.IndexLabRequest request) {
        return ragLabService.index(request);
    }

    public record RagQuestionRequest(String query, String tenantId, String department) {
    }
}
