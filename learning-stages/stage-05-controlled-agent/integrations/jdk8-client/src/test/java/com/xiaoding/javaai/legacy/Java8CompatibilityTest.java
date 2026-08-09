package com.xiaoding.javaai.legacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Java8CompatibilityTest {

    @Test
    void requiresJava8Runtime() {
        assertEquals("8", ClientRuntime.requiredJavaVersion());
    }
}
