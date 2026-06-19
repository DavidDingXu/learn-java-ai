package com.xiaoding.javaai.enterprise.rag;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/enterprise-rag")
public class EnterpriseRagController {

    private final EnterpriseRagApplicationService ragService;

    public EnterpriseRagController(EnterpriseRagApplicationService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/documents")
    public IndexTask uploadDocument(@RequestBody DocumentUploadHttpRequest request) {
        return ragService.uploadAndIndex(new PolicyDocumentUpload(
                request.documentId(),
                request.tenantId(),
                request.departments(),
                request.type(),
                request.content()
        ));
    }

    @PostMapping("/answers")
    public RagAnswer answer(@RequestBody AnswerHttpRequest request) {
        return ragService.answer(
                request.question(),
                new OperatorScope(request.tenantId(), request.department())
        );
    }

    @PostMapping("/eval")
    public EvalReport evaluate(@RequestBody List<EvalHttpRequest> requests) {
        List<EvalCase> cases = requests.stream()
                .map(request -> new EvalCase(
                        request.caseId(),
                        request.question(),
                        new OperatorScope(request.tenantId(), request.department()),
                        request.expectedDocumentId()
                ))
                .toList();
        return ragService.evaluate(cases);
    }

    public record DocumentUploadHttpRequest(
            String documentId,
            String tenantId,
            Set<String> departments,
            DocumentType type,
            String content
    ) {
    }

    public record AnswerHttpRequest(
            String question,
            String tenantId,
            String department
    ) {
    }

    public record EvalHttpRequest(
            String caseId,
            String question,
            String tenantId,
            String department,
            String expectedDocumentId
    ) {
    }
}
