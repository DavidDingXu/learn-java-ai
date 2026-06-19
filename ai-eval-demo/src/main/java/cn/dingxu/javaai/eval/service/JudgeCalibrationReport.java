package cn.dingxu.javaai.eval.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record JudgeCalibrationReport(
        int total,
        int agreed,
        int lowConfidence,
        BigDecimal agreementRate,
        List<JudgeCalibrationResult> results
) {
    public JudgeCalibrationReport {
        agreementRate = agreementRate == null ? BigDecimal.ZERO : agreementRate;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static JudgeCalibrationReport from(List<JudgeCalibrationResult> results) {
        List<JudgeCalibrationResult> safeResults = results == null ? List.of() : List.copyOf(results);
        int total = safeResults.size();
        int agreed = (int) safeResults.stream().filter(JudgeCalibrationResult::agreed).count();
        int lowConfidence = (int) safeResults.stream().filter(JudgeCalibrationResult::lowConfidence).count();
        BigDecimal agreementRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(agreed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new JudgeCalibrationReport(total, agreed, lowConfidence, agreementRate, safeResults);
    }
}
