package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.service.JudgeCalibrationCase;
import com.xiaoding.javaai.eval.service.JudgeCalibrationReport;
import com.xiaoding.javaai.eval.service.JudgeCalibrationRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeCalibrationRunnerTest {

    private final JudgeCalibrationRunner runner = new JudgeCalibrationRunner();

    @Test
    void reportsAgreementWhenJudgeMatchesHumanLabel() {
        JudgeCalibrationCase evalCase = new JudgeCalibrationCase(
                "judge-1",
                "退款建议是否引用了正确制度",
                true,
                true,
                0.86,
                "引用了 refund-policy-001-c1"
        );

        JudgeCalibrationReport report = runner.run(List.of(evalCase));

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.agreed()).isEqualTo(1);
        assertThat(report.lowConfidence()).isZero();
        assertThat(report.results().getFirst().passed()).isTrue();
        assertThat(report.results().getFirst().reason()).isEqualTo("agreed");
    }

    @Test
    void flagsDisagreementBetweenJudgeAndHumanLabel() {
        JudgeCalibrationCase evalCase = new JudgeCalibrationCase(
                "judge-2",
                "高风险退款是否需要人工确认",
                false,
                true,
                0.91,
                "模型评委忽略了人工确认"
        );

        JudgeCalibrationReport report = runner.run(List.of(evalCase));

        assertThat(report.agreed()).isZero();
        assertThat(report.results().getFirst().passed()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("judge_disagreed");
    }

    @Test
    void flagsLowConfidenceEvenWhenLabelsMatch() {
        JudgeCalibrationCase evalCase = new JudgeCalibrationCase(
                "judge-3",
                "回答是否完整",
                true,
                true,
                0.54,
                "判断依据不足"
        );

        JudgeCalibrationReport report = runner.run(List.of(evalCase));

        assertThat(report.agreed()).isEqualTo(1);
        assertThat(report.lowConfidence()).isEqualTo(1);
        assertThat(report.results().getFirst().passed()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("low_confidence");
    }
}
