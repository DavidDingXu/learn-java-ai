# ai-tool-demo

Agent Tool API 工程边界 demo。

本模块演示 Agent 不能直接调用业务 Service，而要通过 Tool API 完成白名单、参数校验、人工确认和审计。

查询类 Tool 当前覆盖工单查询和订单查询。HTTP demo 暴露工单查询；订单查询在单元测试里验证最小返回、敏感字段过滤和审计参数记录。

参数校验集中在 `ToolParameterValidator`，用于保护所有调用来源，不只保护 HTTP Controller。

Tool 审计集中在 `ToolExecutionLedger`。当前是内存账本，记录 `traceId`、`toolName`、`tenantId`、参数摘要、执行结果和耗时，并支持按 trace、租户、工具名查询。生产环境应替换为数据库或审计平台。

`AsyncToolExecutor` 演示高频只读 Tool 的执行边界：异步调度、只读缓存、超时失败和大结果裁剪。它不替代 `TicketToolFacade` 的权限和审计，只是在 Tool API 外面增加执行层保护。

## 启动

```bash
mvn -pl ai-tool-demo spring-boot:run
```

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
```
