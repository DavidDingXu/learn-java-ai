package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.event.RequireUserConfirmEvent;

import java.util.List;

public final class CollaborationPolicy {

    public CollaborationDecision decide(List<WorkUnit> workUnits) {
        if (workUnits == null || workUnits.isEmpty()) {
            throw new IllegalArgumentException("workUnits must not be empty");
        }
        if (workUnits.stream().anyMatch(WorkUnit::sideEffect)) {
            RequireUserConfirmEvent event = new RequireUserConfirmEvent(
                    "confirm-" + Integer.toUnsignedString(workUnits.hashCode()), List.of());
            return new CollaborationDecision(
                    CollaborationMode.HUMAN_REQUIRED,
                    "side-effect work must wait for a human decision",
                    event);
        }
        boolean independent = workUnits.size() > 1 && workUnits.stream().allMatch(WorkUnit::independent);
        if (independent) {
            return new CollaborationDecision(
                    CollaborationMode.MULTI_AGENT,
                    "independent read-only work can be delegated in parallel",
                    null);
        }
        return new CollaborationDecision(
                CollaborationMode.SINGLE_AGENT,
                "dependent work shares one ordered state",
                null);
    }
}
