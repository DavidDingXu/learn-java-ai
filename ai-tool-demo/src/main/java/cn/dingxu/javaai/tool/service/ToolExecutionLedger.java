package cn.dingxu.javaai.tool.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ToolExecutionLedger {

    private final List<ToolExecutionRecord> records = new ArrayList<>();

    public synchronized void append(String toolName,
                                    OperatorContext operator,
                                    boolean approved,
                                    ToolResult result,
                                    Map<String, Object> arguments) {
        append("trace-" + UUID.randomUUID(), toolName, operator, approved, result, arguments, 0);
    }

    public synchronized ToolExecutionRecord append(String traceId,
                                                   String toolName,
                                                   OperatorContext operator,
                                                   boolean approved,
                                                   ToolResult result,
                                                   Map<String, Object> arguments,
                                                   long durationMs) {
        ToolExecutionRecord record = new ToolExecutionRecord(
                traceId,
                toolName,
                operator.userId(),
                operator.tenantId(),
                approved,
                result.success(),
                arguments,
                result.message(),
                durationMs,
                Instant.now()
        );
        records.add(record);
        return record;
    }

    public synchronized List<ToolExecutionRecord> records() {
        return List.copyOf(records);
    }

    public synchronized List<ToolExecutionRecord> recordsByTraceId(String traceId) {
        return records.stream()
                .filter(record -> record.traceId().equals(traceId))
                .toList();
    }

    public synchronized List<ToolExecutionRecord> recordsByTenant(String tenantId) {
        return records.stream()
                .filter(record -> record.tenantId().equals(tenantId))
                .toList();
    }

    public synchronized List<ToolExecutionRecord> recordsByToolName(String toolName) {
        return records.stream()
                .filter(record -> record.toolName().equals(toolName))
                .toList();
    }
}
