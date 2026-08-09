package com.xiaoding.javaai.labs.alibaba;

import java.util.ArrayList;
import java.util.List;

public record FrameworkCompatibilityDecision(
        boolean inPlaceCompatible,
        MigrationBoundary boundary,
        List<String> reasons) {

    public FrameworkCompatibilityDecision {
        reasons = List.copyOf(reasons);
    }

    public static FrameworkCompatibilityDecision compare(FrameworkBaseline mainline, FrameworkBaseline candidate) {
        List<String> reasons = new ArrayList<>();
        if (!major(mainline.version()).equals(major(candidate.version()))) {
            reasons.add("Framework major line differs: " + mainline.version() + " vs " + candidate.version());
        }
        if (!major(mainline.springBootLine()).equals(major(candidate.springBootLine()))) {
            reasons.add("Spring Boot major line differs: " + mainline.springBootLine() + " vs " + candidate.springBootLine());
        }
        boolean compatible = reasons.isEmpty();
        return new FrameworkCompatibilityDecision(
                compatible,
                compatible ? MigrationBoundary.IN_PLACE : MigrationBoundary.ISOLATED_SERVICE_OR_LAB,
                reasons);
    }

    private static String major(String version) {
        int separator = version.indexOf('.');
        return separator < 0 ? version : version.substring(0, separator);
    }
}
