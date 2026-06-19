package com.xiaoding.javaai.helpdesk.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ToolExecutionLedger {

    private final List<ToolExecutionRecord> records = new ArrayList<>();

    public synchronized void append(String toolName,
                                    OperatorContext operator,
                                    boolean approved,
                                    ToolResult result,
                                    Map<String, Object> arguments) {
        records.add(new ToolExecutionRecord(
                toolName,
                operator.userId(),
                operator.tenantId(),
                approved,
                result.success(),
                arguments,
                result.message(),
                Instant.now()
        ));
    }

    public synchronized List<ToolExecutionRecord> records() {
        return List.copyOf(records);
    }
}
