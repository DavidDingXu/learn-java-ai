package com.xiaoding.javaai.prompt.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PromptRiskDetector {

    public PromptRiskReport detect(String userInput) {
        String normalized = normalize(userInput);
        List<String> risks = new ArrayList<>();

        if (containsAny(normalized, "忽略以上", "忽略之前", "忽略所有", "ignore previous", "ignore all")) {
            risks.add("instruction_override");
        }
        if (containsAny(normalized, "系统提示词", "system prompt", "api key", "apikey", "secret", "密钥")) {
            risks.add("secret_extraction");
        }
        if (containsAny(normalized, "直接调用工具", "绕过审批", "不需要人工确认", "bypass approval")) {
            risks.add("tool_policy_bypass");
        }

        return new PromptRiskReport(risks.isEmpty(), List.copyOf(risks));
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String input, String... needles) {
        for (String needle : needles) {
            if (input.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
