package com.xiaoding.javaai.enterprise.rag;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.RealAiRuntime;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/enterprise-rag")
public class EnterpriseRagController {

    private final EnterpriseRagApplicationService ragService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final String apiKey;
    private final String modelName;
    private final String embeddingModelName;

    public EnterpriseRagController(EnterpriseRagApplicationService ragService,
                                   ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                   ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                   @Value("${spring.ai.openai.api-key:}") String apiKey,
                                   @Value("${java-ai.enterprise-rag.model-name:gpt-4o-mini}") String modelName,
                                   @Value("${java-ai.enterprise-rag.embedding-model-name:text-embedding-3-small}") String embeddingModelName) {
        this.ragService = ragService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.embeddingModelName = embeddingModelName;
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

    @PostMapping("/answers/live")
    public LiveEnterpriseRagAnswer liveAnswer(@RequestBody AnswerHttpRequest request) {
        RealAiRuntime.requireConfigured(apiKey, "project-enterprise-rag");
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("project-enterprise-rag requires a Spring AI EmbeddingModel bean");
        }
        float[] queryVector = embeddingModel.embed(request.question());
        RagAnswer evidence = ragService.answer(
                request.question(),
                new OperatorScope(request.tenantId(), request.department())
        );
        LiveAiResult modelAnswer = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "project-enterprise-rag"
        ).call(
                "你是企业制度知识库助手。只能基于检索证据回答；证据为空或冲突时必须拒绝给确定结论，并说明需要人工复核。",
                """
                        用户问题：%s
                        租户/部门：%s/%s
                        检索答案草稿：%s
                        引用证据：%s
                        Trace：%s
                        请生成一段可展示给客服的制度解释，并保留引用 documentId/chunkId。
                        """.formatted(
                        request.question(),
                        request.tenantId(),
                        request.department(),
                        evidence.content(),
                        evidence.citations(),
                        evidence.trace()
                )
        );
        return new LiveEnterpriseRagAnswer(
                modelName,
                embeddingModelName,
                queryVector.length,
                evidence,
                modelAnswer
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

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
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

    public record LiveEnterpriseRagAnswer(
            String model,
            String embeddingModel,
            int queryVectorDimensions,
            RagAnswer evidence,
            LiveAiResult answer
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
