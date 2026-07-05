package com.xiaoding.javaai.eval.controller;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import com.xiaoding.javaai.eval.service.EvalCase;
import com.xiaoding.javaai.eval.service.EvalReport;
import com.xiaoding.javaai.eval.service.EvalRunner;
import com.xiaoding.javaai.eval.service.AgentEvalCase;
import com.xiaoding.javaai.eval.service.AgentEvalObservation;
import com.xiaoding.javaai.eval.service.AgentEvalReport;
import com.xiaoding.javaai.eval.service.AgentEvalRunner;
import com.xiaoding.javaai.eval.service.JudgeCalibrationCase;
import com.xiaoding.javaai.eval.service.JudgeCalibrationReport;
import com.xiaoding.javaai.eval.service.JudgeCalibrationRunner;
import com.xiaoding.javaai.eval.service.HarnessExperimentCase;
import com.xiaoding.javaai.eval.service.HarnessExperimentReport;
import com.xiaoding.javaai.eval.service.HarnessExperimentRunner;
import com.xiaoding.javaai.eval.service.HarnessStrategyObservation;
import com.xiaoding.javaai.eval.service.PromptRegressionCase;
import com.xiaoding.javaai.eval.service.PromptRegressionReport;
import com.xiaoding.javaai.eval.service.PromptRegressionRunner;
import com.xiaoding.javaai.eval.service.RagEvalCase;
import com.xiaoding.javaai.eval.service.RagEvalObservation;
import com.xiaoding.javaai.eval.service.RagEvalReport;
import com.xiaoding.javaai.eval.service.RagEvalRunner;
import org.springframework.ai.chat.client.ChatClient;
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

@RestController
@RequestMapping("/api/eval")
public class EvalController {

    private final EvalRunner evalRunner;
    private final RagEvalRunner ragEvalRunner;
    private final AgentEvalRunner agentEvalRunner;
    private final JudgeCalibrationRunner judgeCalibrationRunner;
    private final PromptRegressionRunner promptRegressionRunner;
    private final HarnessExperimentRunner harnessExperimentRunner;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String judgeModelName;

    public EvalController(EvalRunner evalRunner,
                          RagEvalRunner ragEvalRunner,
                          AgentEvalRunner agentEvalRunner,
                          JudgeCalibrationRunner judgeCalibrationRunner,
                          PromptRegressionRunner promptRegressionRunner,
                          HarnessExperimentRunner harnessExperimentRunner,
                          ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                          @Value("${spring.ai.openai.api-key:}") String apiKey,
                          @Value("${java-ai.eval.judge-model-name:gpt-4o-mini}") String judgeModelName) {
        this.evalRunner = evalRunner;
        this.ragEvalRunner = ragEvalRunner;
        this.agentEvalRunner = agentEvalRunner;
        this.judgeCalibrationRunner = judgeCalibrationRunner;
        this.promptRegressionRunner = promptRegressionRunner;
        this.harnessExperimentRunner = harnessExperimentRunner;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.judgeModelName = judgeModelName;
    }

    @PostMapping("/run")
    public EvalReport run(@RequestBody List<EvalCase> cases) {
        return evalRunner.run(cases);
    }

    @PostMapping("/rag/run")
    public RagEvalReport runRag(@RequestBody RagEvalRequest request) {
        return ragEvalRunner.run(request.cases(), request.observations());
    }

    @PostMapping("/agent/run")
    public AgentEvalReport runAgent(@RequestBody AgentEvalRequest request) {
        return agentEvalRunner.run(request.cases(), request.observations());
    }

    @PostMapping("/judge/calibrate")
    public JudgeCalibrationReport runJudgeCalibration(@RequestBody List<JudgeCalibrationCase> cases) {
        return judgeCalibrationRunner.run(cases);
    }

    @PostMapping("/prompt/regression")
    public PromptRegressionReport runPromptRegression(@RequestBody List<PromptRegressionCase> cases) {
        return promptRegressionRunner.run(cases);
    }

    @PostMapping("/harness/run")
    public HarnessExperimentReport runHarnessExperiment(@RequestBody HarnessExperimentRequest request) {
        return harnessExperimentRunner.run(request.cases(), request.observations());
    }

    @PostMapping("/judge/live")
    public LiveJudgeResponse runLiveJudge(@RequestBody LiveJudgeRequest request) {
        String prompt = """
                请作为 AI 评测裁判，判断候选回答是否满足期望依据。
                只输出三行：pass=true/false、score=0-1、reason=中文理由。
                问题：%s
                期望依据：%s
                候选回答：%s
                """.formatted(request.question(), request.expectedEvidence(), request.answer());
        LiveAiResult result = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                judgeModelName,
                "ai-eval-demo"
        ).call("你是严谨的 AI 质量评测裁判，不要替业务系统补不存在的依据。", prompt);
        return new LiveJudgeResponse(result.mode(), result.model(), prompt, result.content());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    public record RagEvalRequest(
            List<RagEvalCase> cases,
            List<RagEvalObservation> observations
    ) {
    }

    public record AgentEvalRequest(
            List<AgentEvalCase> cases,
            List<AgentEvalObservation> observations
    ) {
    }

    public record HarnessExperimentRequest(
            List<HarnessExperimentCase> cases,
            List<HarnessStrategyObservation> observations
    ) {
    }

    public record LiveJudgeRequest(String question, String expectedEvidence, String answer) {
    }

    public record LiveJudgeResponse(String mode, String model, String prompt, String judgeOutput) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
