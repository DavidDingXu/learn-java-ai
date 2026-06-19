package cn.dingxu.javaai.legacy.legacy;

import cn.dingxu.javaai.legacy.legacy.model.LegacyAuditRecord;
import cn.dingxu.javaai.legacy.legacy.model.OperatorContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryLegacyAuditLedger {

    private final List<LegacyAuditRecord> records = new ArrayList<LegacyAuditRecord>();

    public void record(String action, String ticketId, OperatorContext operatorContext) {
        records.add(new LegacyAuditRecord(
                action,
                ticketId,
                operatorContext.getOperatorId(),
                operatorContext.getTenantId(),
                operatorContext.getDepartments(),
                operatorContext.getPermissions(),
                Instant.now()
        ));
    }

    public List<LegacyAuditRecord> records() {
        return Collections.unmodifiableList(records);
    }
}
