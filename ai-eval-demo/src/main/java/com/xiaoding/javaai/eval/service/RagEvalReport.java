package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record RagEvalReport(
        int total,
        int retrievalPassed,
        int citationPassed,
        int noEvidencePassed,
        BigDecimal passRate,
        List<RagEvalResult> results
) {
    public RagEvalReport {
        passRate = passRate == null ? BigDecimal.ZERO : passRate;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static RagEvalReport from(List<RagEvalResult> results) {
        List<RagEvalResult> safeResults = results == null ? List.of() : List.copyOf(results);
        int total = safeResults.size();
        int retrievalPassed = (int) safeResults.stream().filter(RagEvalResult::retrievalPassed).count();
        int citationPassed = (int) safeResults.stream().filter(RagEvalResult::citationPassed).count();
        int noEvidencePassed = (int) safeResults.stream().filter(RagEvalResult::noEvidencePassed).count();
        int passed = (int) safeResults.stream().filter(RagEvalResult::passed).count();
        BigDecimal passRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new RagEvalReport(total, retrievalPassed, citationPassed, noEvidencePassed, passRate, safeResults);
    }
}
