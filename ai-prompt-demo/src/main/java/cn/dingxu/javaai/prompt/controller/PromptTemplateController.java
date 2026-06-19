package cn.dingxu.javaai.prompt.controller;

import cn.dingxu.javaai.prompt.service.PromptTemplate;
import cn.dingxu.javaai.prompt.service.PromptTemplateService;
import cn.dingxu.javaai.prompt.service.PromptRiskDetector;
import cn.dingxu.javaai.prompt.service.PromptRiskReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;
    private final PromptRiskDetector promptRiskDetector;

    public PromptTemplateController(PromptTemplateService promptTemplateService, PromptRiskDetector promptRiskDetector) {
        this.promptTemplateService = promptTemplateService;
        this.promptRiskDetector = promptRiskDetector;
    }

    @PostMapping
    public PromptTemplate save(@RequestBody SavePromptRequest request) {
        return promptTemplateService.save(request.code(), request.version(), request.content());
    }

    @GetMapping("/render")
    public String render(@RequestParam String code, @RequestParam Map<String, String> variables) {
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

    public record SavePromptRequest(String code, String version, String content) {
    }

    public record RollbackPromptRequest(String code, String version) {
    }

    public record DetectRiskRequest(String userInput) {
    }
}
