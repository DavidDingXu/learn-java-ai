package com.xiaoding.javaai.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EvalRunnerBuildTest {

    @Test
    void reportsPhaseFBuildVersion() {
        assertEquals("phase-f", EvalRunner.version());
    }
}
