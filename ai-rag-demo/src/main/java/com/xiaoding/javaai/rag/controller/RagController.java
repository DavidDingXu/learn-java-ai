package com.xiaoding.javaai.rag.controller;

import com.xiaoding.javaai.common.ai.RealAiRuntime;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.RagAnswer;
import com.xiaoding.javaai.rag.service.RagLabService;
import com.xiaoding.javaai.rag.service.RagRetrievalService;
import com.xiaoding.javaai.rag.service.IndexTaskResult;
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

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagRetrievalService ragRetrievalService;
    private final RagLabService ragLabService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final String apiKey;
    private final String modelName;
    private final String embeddingModelName;

    public RagController(RagRetrievalService ragRetrievalService,
                         RagLabService ragLabService,
                         ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                         ObjectProvider<EmbeddingModel> embeddingModelProvider,
                         @Value("${spring.ai.openai.api-key:}") String apiKey,
                         @Value("${java-ai.rag.model-name:gpt-4o-mini}") String modelName,
                         @Value("${java-ai.rag.embedding-model-name:text-embedding-3-small}") String embeddingModelName) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragLabService = ragLabService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.embeddingModelName = embeddingModelName;
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

    @PostMapping("/live-answer")
    public LiveRagAnswer liveAnswer(@RequestBody RagQuestionRequest request) {
        RealAiRuntime.requireConfigured(apiKey, "ai-rag-demo");
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (embeddingModel == null || builder == null) {
            throw new IllegalStateException("ai-rag-demo requires Spring AI EmbeddingModel and ChatClient.Builder beans");
        }
        RagAnswer evidence = ragRetrievalService.answer(
                request.query(),
                new OperatorScope(request.tenantId(), request.department())
        );
        if (evidence.citations().isEmpty()) {
            return new LiveRagAnswer("no-evidence", modelName, embeddingModelName, 0, evidence.content(), evidence);
        }
        float[] queryVector = embeddingModel.embed(request.query());
        String modelAnswer = builder.build()
                .prompt()
                .system("你是企业制度知识库助手。只能基于给定依据回答，必须保留引用 documentId/chunkId。")
                .user("""
                        问题：%s
                        可用依据：%s
                        请输出处理建议，并逐条列出引用。
                        """.formatted(request.query(), evidence.citations()))
                .call()
                .content();
        return new LiveRagAnswer(
                "model:" + modelName,
                modelName,
                embeddingModelName,
                queryVector.length,
                modelAnswer,
                evidence
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record RagQuestionRequest(String query, String tenantId, String department) {
    }

    public record LiveRagAnswer(
            String mode,
            String model,
            String embeddingModel,
            int queryVectorDimensions,
            String answer,
            RagAnswer evidence
    ) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
