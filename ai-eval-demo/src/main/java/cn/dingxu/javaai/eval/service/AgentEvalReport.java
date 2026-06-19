package cn.dingxu.javaai.eval.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record AgentEvalReport(
        int total,
        int pathPassed,
        int approvalPassed,
        int riskPassed,
        BigDecimal passRate,
        List<AgentEvalResult> results
) {
    public AgentEvalReport {
        passRate = passRate == null ? BigDecimal.ZERO : passRate;
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static AgentEvalReport from(List<AgentEvalResult> results) {
        List<AgentEvalResult> safeResults = results == null ? List.of() : List.copyOf(results);
        int total = safeResults.size();
        int pathPassed = (int) safeResults.stream().filter(AgentEvalResult::pathPassed).count();
        int approvalPassed = (int) safeResults.stream().filter(AgentEvalResult::approvalPassed).count();
        int riskPassed = (int) safeResults.stream().filter(AgentEvalResult::riskPassed).count();
        int passed = (int) safeResults.stream().filter(AgentEvalResult::passed).count();
        BigDecimal passRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(passed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
        return new AgentEvalReport(total, pathPassed, approvalPassed, riskPassed, passRate, safeResults);
    }
}
