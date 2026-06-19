package cn.dingxu.javaai.eval.service;

public record EvalResult(String caseId, boolean passed, String reason) {
}
