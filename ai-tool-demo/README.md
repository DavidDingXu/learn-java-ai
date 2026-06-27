# ai-tool-demo

Agent Tool API 工程边界 demo。

本模块演示 Agent 不能直接调用业务 Service，而要通过 Tool API 完成白名单、参数校验、人工确认和审计。

查询类 Tool 当前覆盖工单查询和订单查询。HTTP demo 暴露工单查询；订单查询在单元测试里验证最小返回、敏感字段过滤和审计参数记录。

参数校验集中在 `ToolParameterValidator`，用于保护所有调用来源，不只保护 HTTP Controller。

Tool 审计集中在 `ToolExecutionLedger`。当前是内存账本，记录 `traceId`、`toolName`、`tenantId`、参数摘要、执行结果和耗时，并支持按 trace、租户、工具名查询。生产环境应替换为数据库或审计平台。

`AsyncToolExecutor` 演示高频只读 Tool 的执行边界：异步调度、只读缓存、超时失败和大结果裁剪。它不替代 `TicketToolFacade` 的权限和审计，只是在 Tool API 外面增加执行层保护。

`SpringAiToolCallbackBridge` 把已经治理过的 `TicketToolFacade` 暴露成 Spring AI 原生 `ToolCallback`。模型侧仍然使用 Spring AI Tool Calling，项目侧继续负责参数校验、权限边界、人工确认、幂等和审计，不把 Tool 压成普通字符串调用。

## 启动

```bash
mvn -pl ai-tool-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8087/
```

页面会演示查询类 Tool、写操作人工确认、确认 token 和审计 ledger。

## 查询工单

```bash
curl -X POST http://localhost:8087/api/tools/ticket/lookup \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "operator": {
      "userId": "u1001",
      "tenantId": "tenant-a",
      "department": "support"
    }
  }'
```

## 写操作未确认

```bash
curl -X POST http://localhost:8087/api/tools/ticket/close \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "humanApproved": false,
    "operator": {
      "userId": "u1001",
      "tenantId": "tenant-a",
      "department": "support"
    }
  }'
```

## 写操作已确认

```bash
curl -X POST http://localhost:8087/api/tools/ticket/close \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "humanApproved": true,
    "confirmationToken": "confirm-001",
    "operator": {
      "userId": "u1001",
      "tenantId": "tenant-a",
      "department": "support"
    }
  }'
```

## 查看审计

```bash
curl http://localhost:8087/api/tools/ledger
```

## 测试

```bash
mvn -pl ai-tool-demo test
```

测试覆盖：

```text
lookupTicket：查询工单并写入审计。
lookupOrder：查询订单的最小返回和审计参数。
closeTicket：写操作未人工确认时拒绝执行，已确认时使用 confirmationToken 做幂等。
blank identifier：空参数在进入业务查询前被拒绝。
malformed identifier：工单号、订单号、confirmationToken 格式错误时拒绝执行。
ledger query：按 traceId、tenantId、toolName 复盘 Tool 调用路径。
async executor：只读结果缓存、慢 Tool 超时、大结果裁剪后返回。
SpringAiToolCallbackBridgeTest：验证 `ticket.lookup`、`ticket.close` 以 Spring AI `ToolCallback` 暴露，并且写操作不能绕过人工确认和审计。
```

## Spring AI Tool Calling 接入点

生产接入真实模型时，不要让 Agent 直接调用业务 Service，也不要绕过 Spring AI 的 Tool Calling。推荐边界是：

```text
ChatClient / Agent
  -> Spring AI ToolCallback(ticket.lookup, ticket.close)
  -> TicketToolFacade
  -> 参数校验、权限、人工确认、幂等、审计
  -> 业务系统
```

`SpringAiToolCallbackBridge#toolCallbackProvider()` 可以交给 `ChatClient` 的工具配置使用。这样模型看到的是带 schema 的工具，Java 项目保留确定性治理。
