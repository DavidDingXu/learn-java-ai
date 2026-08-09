package com.xiaoding.javaai.ticket.agent.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public final class ToolActionFingerprint {

    private ToolActionFingerprint() {
    }

    public static String calculate(String toolName, Map<String, String> arguments) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, requireText(toolName, "toolName"));
        java.util.Objects.requireNonNull(arguments, "arguments must not be null")
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    append(canonical, requireText(entry.getKey(), "argument name"));
                    append(canonical, java.util.Objects.requireNonNull(
                            entry.getValue(), "argument value must not be null"));
                });
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    static boolean matches(String expected, String toolName, Map<String, String> arguments) {
        if (expected == null || expected.isBlank()) return false;
        return MessageDigest.isEqual(
                expected.trim().getBytes(StandardCharsets.US_ASCII),
                calculate(toolName, arguments).getBytes(StandardCharsets.US_ASCII));
    }

    private static void append(StringBuilder canonical, String value) {
        canonical.append(value.length()).append(':').append(value).append(';');
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
