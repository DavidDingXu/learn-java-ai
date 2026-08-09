package com.xiaoding.javaai.stages.support;

import com.fasterxml.jackson.databind.JsonNode;

public final class StageOutput {

    private StageOutput() {
    }

    public static void heading(String text) {
        System.out.println();
        System.out.println("=== " + text + " ===");
    }

    public static void value(String label, Object value) {
        System.out.printf("%-18s %s%n", label + ":", value);
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "-" : value.asText();
    }
}
