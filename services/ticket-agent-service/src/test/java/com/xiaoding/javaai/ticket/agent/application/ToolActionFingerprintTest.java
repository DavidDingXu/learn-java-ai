package com.xiaoding.javaai.ticket.agent.application;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolActionFingerprintTest {

    @Test
    void produces_the_same_fingerprint_regardless_of_argument_iteration_order() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("amountMinor", "10000");
        first.put("currency", "CNY");
        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("currency", "CNY");
        reversed.put("amountMinor", "10000");

        assertThat(ToolActionFingerprint.calculate("ISSUE_REFUND", first))
                .isEqualTo(ToolActionFingerprint.calculate("ISSUE_REFUND", reversed));
    }

    @Test
    void length_prefixes_keep_delimiter_like_values_from_colliding() {
        Map<String, String> oneArgument = Map.of("a", "b;c=d");
        Map<String, String> twoArguments = Map.of("a", "b", "c", "d");

        assertThat(ToolActionFingerprint.calculate("TEST_TOOL", oneArgument))
                .isNotEqualTo(ToolActionFingerprint.calculate("TEST_TOOL", twoArguments));
    }
}
