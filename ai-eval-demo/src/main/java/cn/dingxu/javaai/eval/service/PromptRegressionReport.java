package cn.dingxu.javaai.eval.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record PromptRegressionReport(
        int total,
        int regressions,
        BigDecimal passRate,
        List<PromptRegressionResult> results
) {
    public PromptRegressionReport {
        passRate = passRate == null ? BigDecimal.ZERO : passRate;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static PromptRegressionReport from(List<PromptRegressionResult> results) {
        List<PromptRegressionResult> safeResults = results == null ? List.of() : List.copyOf(results);
        int total = safeResults.size();
        int regressions = (int) safeResults.stream().filter(result -> !result.passed()).count();
        int passed = total - regressions;
        BigDecimal passRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new PromptRegressionReport(total, regressions, passRate, safeResults);
    }
}
