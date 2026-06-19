package cn.dingxu.javaai.eval;

import cn.dingxu.javaai.eval.service.HarnessExperimentCase;
import cn.dingxu.javaai.eval.service.HarnessExperimentRunner;
import cn.dingxu.javaai.eval.service.HarnessStrategyObservation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessExperimentRunnerTest {

    private final HarnessExperimentRunner runner = new HarnessExperimentRunner();

    @Test
    void selectsCandidateOnlyWhenQualityImprovesWithoutCostOrLatencyRegression() {
        HarnessExperimentCase evalCase = new HarnessExperimentCase(
                "harness-1",
                "ticket-advice",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.20),
                BigDecimal.valueOf(0.25)
        );
        HarnessStrategyObservation baseline = new HarnessStrategyObservation(
                "harness-1", "baseline", BigDecimal.valueOf(0.82), 900, 1200);
        HarnessStrategyObservation candidate = new HarnessStrategyObservation(
                "harness-1", "candidate", BigDecimal.valueOf(0.87), 980, 1300);

        var report = runner.run(List.of(evalCase), List.of(baseline, candidate));

        assertThat(report.results()).hasSize(1);
        assertThat(report.results().getFirst().winner()).isEqualTo("candidate");
        assertThat(report.results().getFirst().promotable()).isTrue();
        assertThat(report.promotableCandidates()).isEqualTo(1);
    }

    @Test
    void rejectsCandidateWhenCostIncreaseExceedsLimitEvenIfQualityImproves() {
        HarnessExperimentCase evalCase = new HarnessExperimentCase(
                "harness-2",
                "policy-answer",
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.10),
                BigDecimal.valueOf(0.25)
        );
        HarnessStrategyObservation baseline = new HarnessStrategyObservation(
                "harness-2", "baseline", BigDecimal.valueOf(0.80), 800, 1000);
        HarnessStrategyObservation candidate = new HarnessStrategyObservation(
                "harness-2", "candidate", BigDecimal.valueOf(0.86), 1100, 1200);

        var report = runner.run(List.of(evalCase), List.of(baseline, candidate));

        assertThat(report.results().getFirst().winner()).isEqualTo("baseline");
        assertThat(report.results().getFirst().promotable()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("cost");
    }
}
