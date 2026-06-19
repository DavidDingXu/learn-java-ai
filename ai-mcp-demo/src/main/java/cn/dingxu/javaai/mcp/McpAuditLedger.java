package cn.dingxu.javaai.mcp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class McpAuditLedger {

    private final List<McpAuditRecord> records = new ArrayList<>();

    public synchronized void append(String toolName,
                                    OperatorContext operator,
                                    boolean allowed,
                                    McpToolResult result,
                                    Map<String, Object> arguments) {
        append(toolName, operator, allowed, result, arguments, "");
    }

    public synchronized void append(String toolName,
                                    OperatorContext operator,
                                    boolean allowed,
                                    McpToolResult result,
                                    Map<String, Object> arguments,
                                    String denyReason) {
        records.add(new McpAuditRecord(
                toolName,
                operator.userId(),
                operator.tenantId(),
                allowed,
                result.success(),
                result.message(),
                denyReason,
                arguments,
                Instant.now()
        ));
    }

    public synchronized List<McpAuditRecord> records() {
        return List.copyOf(records);
    }
}
