package com.xiaoding.javaai.eval.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public EvalController(EvalRunner evalRunner,
                          RagEvalRunner ragEvalRunner,
                          AgentEvalRunner agentEvalRunner,
                          JudgeCalibrationRunner judgeCalibrationRunner,
                          PromptRegressionRunner promptRegressionRunner,
                          HarnessExperimentRunner harnessExperimentRunner) {
        this.evalRunner = evalRunner;
        this.ragEvalRunner = ragEvalRunner;
        this.agentEvalRunner = agentEvalRunner;
        this.judgeCalibrationRunner = judgeCalibrationRunner;
        this.promptRegressionRunner = promptRegressionRunner;
        this.harnessExperimentRunner = harnessExperimentRunner;
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
}
