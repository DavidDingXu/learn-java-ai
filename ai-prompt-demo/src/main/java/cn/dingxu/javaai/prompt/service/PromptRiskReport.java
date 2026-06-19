package cn.dingxu.javaai.prompt.service;

import java.util.List;

public record PromptRiskReport(
        boolean safe,
        List<String> risks
) {
}
