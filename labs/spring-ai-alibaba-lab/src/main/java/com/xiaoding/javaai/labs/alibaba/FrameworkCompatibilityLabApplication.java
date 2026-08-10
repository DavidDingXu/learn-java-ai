package com.xiaoding.javaai.labs.alibaba;

import java.util.Properties;

public final class FrameworkCompatibilityLabApplication {

    private FrameworkCompatibilityLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = SpringAiAlibabaLabApplication.loadConfig();
        FrameworkCompatibilityDecision decision = FrameworkCompatibilityDecision.compare(
                new FrameworkBaseline("Spring AI", "2.0.0", "4.1.0"),
                new FrameworkBaseline(
                        "Spring AI Alibaba",
                        SpringAiAlibabaLabApplication.required(config, "lab.compatibility.candidate-version"),
                        SpringAiAlibabaLabApplication.required(config, "lab.compatibility.candidate-boot-line")));
        System.out.printf("inPlace=%s boundary=%s reasons=%s%n",
                decision.inPlaceCompatible(), decision.boundary(), decision.reasons());
    }
}
