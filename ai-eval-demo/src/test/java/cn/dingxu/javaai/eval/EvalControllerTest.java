package cn.dingxu.javaai.eval;

import cn.dingxu.javaai.eval.controller.EvalController;
import cn.dingxu.javaai.eval.service.AgentEvalRunner;
import cn.dingxu.javaai.eval.service.EvalRunner;
import cn.dingxu.javaai.eval.service.HarnessExperimentCase;
import cn.dingxu.javaai.eval.service.HarnessExperimentRunner;
import cn.dingxu.javaai.eval.service.HarnessStrategyObservation;
import cn.dingxu.javaai.eval.service.JudgeCalibrationCase;
import cn.dingxu.javaai.eval.service.JudgeCalibrationRunner;
import cn.dingxu.javaai.eval.service.PromptRegressionCase;
import cn.dingxu.javaai.eval.service.PromptRegressionRunner;
import cn.dingxu.javaai.eval.service.RagEvalRunner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvalControllerTest {

    private final EvalController controller = new EvalController(
            new EvalRunner(),
            new RagEvalRunner(),
            new AgentEvalRunner(),
            new JudgeCalibrationRunner(),
            new PromptRegressionRunner(),
            new HarnessExperimentRunner()
    );

    @Test
    void exposesJudgeCalibrationEndpoint() {
        var request = List.of(new JudgeCalibrationCase(
                "judge-api-1",
                "回答是否引用正确制度",
                true,
                true,
                0.9,
                "人工和评委一致"
        ));

        var report = controller.runJudgeCalibration(request);

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.agreed()).isEqualTo(1);
    }

    @Test
    void exposesPromptRegressionEndpoint() {
        var request = List.of(new PromptRegressionCase(
                "prompt-api-1",
                "ticket-advice",
                "v1",
                "v2",
                BigDecimal.valueOf(0.82),
                BigDecimal.valueOf(0.84),
                BigDecimal.valueOf(0.02)
        ));

        var report = controller.runPromptRegression(request);

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.regressions()).isZero();
    }

    @Test
    void exposesHarnessExperimentEndpoint() {
        var request = new EvalController.HarnessExperimentRequest(
                List.of(new HarnessExperimentCase(
                        "harness-api-1",
                        "ticket-advice",
                        BigDecimal.valueOf(0.03),
                        BigDecimal.valueOf(0.20),
                        BigDecimal.valueOf(0.25)
                )),
                List.of(
                        new HarnessStrategyObservation("harness-api-1", "baseline", BigDecimal.valueOf(0.82), 900, 1200),
                        new HarnessStrategyObservation("harness-api-1", "candidate", BigDecimal.valueOf(0.87), 980, 1300)
                )
        );

        var report = controller.runHarnessExperiment(request);

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.promotableCandidates()).isEqualTo(1);
    }
}
