package com.xiaoding.javaai.labs.alibaba;

import java.util.List;

public record ConfirmationResult(ConfirmationStatus status, List<String> visitedNodes) {
    public ConfirmationResult {
        visitedNodes = List.copyOf(visitedNodes);
    }
}
