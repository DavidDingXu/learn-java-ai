package com.xiaoding.javaai.labs.agentscope;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record A2aTaskRequest(
        String tenantId,
        String idempotencyKey,
        String remoteAgent,
        String taskType,
        String businessObjectId,
        String dataScope,
        String instruction,
        String inputVersion) {

    public A2aTaskRequest {
        tenantId = normalizeRequired(tenantId);
        idempotencyKey = normalizeRequired(idempotencyKey);
        remoteAgent = normalizeRequired(remoteAgent);
        taskType = normalizeRequired(taskType);
        businessObjectId = normalizeRequired(businessObjectId);
        dataScope = normalizeRequired(dataScope);
        instruction = normalizeRequired(instruction).replaceAll("\\s+", " ");
        inputVersion = normalizeRequired(inputVersion);
    }

    public String requestFingerprint() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{
                    tenantId, remoteAgent, taskType, businessObjectId, dataScope, instruction, inputVersion}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update((byte) (bytes.length >>> 24));
                digest.update((byte) (bytes.length >>> 16));
                digest.update((byte) (bytes.length >>> 8));
                digest.update((byte) bytes.length);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A2A request fields must not be blank");
        }
        return value.strip();
    }
}
