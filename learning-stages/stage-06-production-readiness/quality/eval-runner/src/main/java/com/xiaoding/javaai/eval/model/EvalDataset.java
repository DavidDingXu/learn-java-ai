package com.xiaoding.javaai.eval.model;

import java.util.List;

public record EvalDataset(String version, List<EvalCase> cases) {

    public EvalDataset {
        cases = List.copyOf(cases);
    }
}
