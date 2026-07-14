package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public final class TicketBusinessTools {

    @Tool(name = "query_ticket", description = "按编号查询工单低敏摘要", readOnly = true, strict = true)
    public String queryTicket(
            @ToolParam(name = "ticket_id", description = "工单编号") String ticketId) {
        return "{\"ticketId\":\"" + ticketId + "\",\"status\":\"OPEN\"}";
    }

    @Tool(name = "update_ticket", description = "更新工单状态，需要人工确认", strict = true, concurrencySafe = false)
    public String updateTicket(
            @ToolParam(name = "ticket_id", description = "工单编号") String ticketId,
            @ToolParam(name = "status", description = "目标状态") String status) {
        return "{\"ticketId\":\"" + ticketId + "\",\"status\":\"" + status + "\"}";
    }

    @Tool(name = "export_all_customers", description = "导出全量客户数据", readOnly = true)
    public String exportAllCustomers() {
        return "disabled";
    }
}
