package com.xiaoding.javaai.eval.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JudgeCalibrationRunner {

    private static final double MIN_CONFIDENCE = 0.70;

    public JudgeCalibrationReport run(List<JudgeCalibrationCase> cases) {
        List<JudgeCalibrationResult> results = (cases == null ? List.<JudgeCalibrationCase>of() : cases).stream()
                .map(this::evaluate)
                .toList();
        return JudgeCalibrationReport.from(results);
    }

    private JudgeCalibrationResult evaluate(JudgeCalibrationCase evalCase) {
        boolean agreed = evalCase.humanPassed() == evalCase.judgePassed();
        boolean lowConfidence = evalCase.judgeConfidence() < MIN_CONFIDENCE;
        boolean passed = agreed && !lowConfidence;
        return new JudgeCalibrationResult(evalCase.caseId(), agreed, lowConfidence, passed,
                reason(agreed, lowConfidence));
    }

    private String reason(boolean agreed, boolean lowConfidence) {
        if (!agreed) {
            return "judge_disagreed";
        }
        if (lowConfidence) {
            return "low_confidence";
        }
        return "agreed";
    }
}
