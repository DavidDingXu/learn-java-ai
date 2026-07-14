package com.xiaoding.javaai.legacy.contract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolActionCommand {

    private final String actionId;
    private final String ticketId;
    private final String actionType;
    private final Map<String, String> arguments;

    public ToolActionCommand(
            String actionId,
            String ticketId,
            String actionType,
            Map<String, String> arguments
    ) {
        this.actionId = requireText(actionId, "actionId");
        this.ticketId = requireText(ticketId, "ticketId");
        this.actionType = requireText(actionType, "actionType");
        this.arguments = arguments == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(arguments));
    }

    public String getActionId() {
        return actionId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getActionType() {
        return actionType;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
