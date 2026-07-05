package com.xiaoding.javaai.prompt.controller;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import com.xiaoding.javaai.prompt.service.PromptTemplate;
import com.xiaoding.javaai.prompt.service.PromptTemplateService;
import com.xiaoding.javaai.prompt.service.PromptRiskDetector;
import com.xiaoding.javaai.prompt.service.PromptRiskReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;
    private final PromptRiskDetector promptRiskDetector;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public PromptTemplateController(PromptTemplateService promptTemplateService,
                                    PromptRiskDetector promptRiskDetector,
                                    ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                    @Value("${spring.ai.openai.api-key:}") String apiKey,
                                    @Value("${java-ai.prompt.model-name:gpt-4o-mini}") String modelName) {
        this.promptTemplateService = promptTemplateService;
        this.promptRiskDetector = promptRiskDetector;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @PostMapping
    public PromptTemplate save(@RequestBody SavePromptRequest request) {
        return promptTemplateService.save(request.code(), request.version(), request.content());
    }

    @GetMapping("/render")
    public String render(@RequestParam("code") String code, @RequestParam Map<String, String> variables) {
        variables.remove("code");
        return promptTemplateService.render(code, variables);
    }

    @PostMapping("/rollback")
    public PromptTemplate rollback(@RequestBody RollbackPromptRequest request) {
        return promptTemplateService.rollback(request.code(), request.version());
    }

    @PostMapping("/risk/detect")
    public PromptRiskReport detectRisk(@RequestBody DetectRiskRequest request) {
        return promptRiskDetector.detect(request.userInput());
    }

    @PostMapping("/live-preview")
    public LiveAiResult livePreview(@RequestBody LivePromptRequest request) {
        String renderedPrompt = promptTemplateService.render(request.code(), request.variables());
        return new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-prompt-demo"
        ).call("你是企业工单系统里的 AI 助手。严格按已经渲染好的业务 Prompt 回答。", renderedPrompt);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record SavePromptRequest(String code, String version, String content) {
    }

    public record RollbackPromptRequest(String code, String version) {
    }

    public record DetectRiskRequest(String userInput) {
    }

    public record LivePromptRequest(String code, Map<String, String> variables) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
