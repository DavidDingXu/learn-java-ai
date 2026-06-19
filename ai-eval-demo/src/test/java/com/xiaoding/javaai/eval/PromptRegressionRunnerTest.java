package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.service.PromptRegressionCase;
import com.xiaoding.javaai.eval.service.PromptRegressionReport;
import com.xiaoding.javaai.eval.service.PromptRegressionRunner;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptRegressionRunnerTest {

    private final PromptRegressionRunner runner = new PromptRegressionRunner();

    @Test
    void passesWhenCandidateKeepsBaselineQuality() {
        PromptRegressionCase evalCase = new PromptRegressionCase(
                "prompt-1",
                "ticket-advice",
                "v1",
                "v2",
                BigDecimal.valueOf(0.82),
                BigDecimal.valueOf(0.85),
                BigDecimal.valueOf(0.02)
        );

        PromptRegressionReport report = runner.run(List.of(evalCase));

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.regressions()).isZero();
        assertThat(report.results().getFirst().passed()).isTrue();
        assertThat(report.results().getFirst().delta()).isEqualByComparingTo("0.03");
    }

    @Test
    void failsWhenCandidateDropsBeyondTolerance() {
        PromptRegressionCase evalCase = new PromptRegressionCase(
                "prompt-2",
                "ticket-advice",
                "v1",
                "v3",
                BigDecimal.valueOf(0.86),
                BigDecimal.valueOf(0.78),
                BigDecimal.valueOf(0.03)
        );

        PromptRegressionReport report = runner.run(List.of(evalCase));

        assertThat(report.regressions()).isEqualTo(1);
        assertThat(report.results().getFirst().passed()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("regression");
    }

    @Test
    void passesSmallDropWithinTolerance() {
        PromptRegressionCase evalCase = new PromptRegressionCase(
                "prompt-3",
                "policy-answer",
                "v4",
                "v5",
                BigDecimal.valueOf(0.91),
                BigDecimal.valueOf(0.89),
                BigDecimal.valueOf(0.03)
        );

        PromptRegressionReport report = runner.run(List.of(evalCase));

        assertThat(report.regressions()).isZero();
        assertThat(report.results().getFirst().passed()).isTrue();
        assertThat(report.results().getFirst().reason()).isEqualTo("within_tolerance");
    }
}
