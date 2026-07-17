package com.xiaoding.javaai.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvalRunnerBuildTest {

    @Test
    void reportsPhaseFBuildVersion() {
        assertEquals("0.1.0", EvalRunner.version());
    }
}
