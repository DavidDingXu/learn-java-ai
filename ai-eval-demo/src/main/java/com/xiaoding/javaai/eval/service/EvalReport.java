package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record EvalReport(int total, int passed, BigDecimal passRate, List<EvalResult> results) {
    public EvalReport {
        passRate = passRate == null ? BigDecimal.ZERO : passRate;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static EvalReport from(List<EvalResult> results) {
        List<EvalResult> safeResults = results == null ? List.of() : List.copyOf(results);
        int total = safeResults.size();
        int passed = (int) safeResults.stream().filter(EvalResult::passed).count();
        BigDecimal passRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new EvalReport(total, passed, passRate, safeResults);
    }
}
