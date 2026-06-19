package com.xiaoding.javaai.eval.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvalRunner {

    public EvalReport run(List<EvalCase> cases) {
        List<EvalResult> results = cases.stream()
                .map(this::evaluate)
                .toList();
        return EvalReport.from(results);
    }

    private EvalResult evaluate(EvalCase evalCase) {
        boolean passed = evalCase.actualAnswer().contains(evalCase.expectedKeyword());
        String reason = passed
                ? "命中期望关键依据：" + evalCase.expectedKeyword()
                : "未命中期望关键依据：" + evalCase.expectedKeyword();
        return new EvalResult(evalCase.caseId(), passed, reason);
    }
}
