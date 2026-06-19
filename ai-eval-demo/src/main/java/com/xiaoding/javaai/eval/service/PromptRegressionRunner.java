package com.xiaoding.javaai.eval.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PromptRegressionRunner {

    public PromptRegressionReport run(List<PromptRegressionCase> cases) {
        List<PromptRegressionResult> results = (cases == null ? List.<PromptRegressionCase>of() : cases).stream()
                .map(this::evaluate)
                .toList();
        return PromptRegressionReport.from(results);
    }

    private PromptRegressionResult evaluate(PromptRegressionCase evalCase) {
        BigDecimal delta = evalCase.candidatePassRate().subtract(evalCase.baselinePassRate()).stripTrailingZeros();
        boolean passed = delta.compareTo(evalCase.tolerance().negate()) >= 0;
        String reason = passed ? "within_tolerance" : "regression delta=" + delta;
        return new PromptRegressionResult(evalCase.caseId(), evalCase.promptKey(), delta, passed, reason);
    }
}
