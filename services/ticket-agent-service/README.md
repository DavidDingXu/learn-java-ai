# Ticket Agent Service

工单 AI 协同与受控 Tool 执行服务。该模块拥有 Agent 任务、规划步骤、确认决定、请求指纹、幂等和审计时间线；最终工单状态和业务写入仍由下游 JDK8 CRM/工单系统掌握。

## 主要边界

- `POST /api/v1/agent/tasks`：从不可变的咨询升级快照创建任务。
- `POST /api/v1/agent/tasks/{taskId}/runs`：在有限步数和服务端 Tool Catalog 内生成建议。
- `PUT /api/v1/agent/tasks/{taskId}/confirmation`：按任务版本和幂等键提交人工决定。
- `GET /api/v1/agent/tasks/{taskId}/audit`：读取低敏审计时间线。

接口契约见 [`contracts/openapi/agent-task-v1.yaml`](../../contracts/openapi/agent-task-v1.yaml)。模型只能提出 Tool 意图，权限、参数、风险、确认、幂等与审计都由应用层处理。

## 运行与测试

```bash
./mvnw -pl services/ticket-agent-service test
./mvnw -pl services/ticket-agent-service spring-boot:run
```

默认 `demo` Profile 使用内存任务库并关闭模型与远程 Tool，用于检查组装和 health，不会伪造成功执行。评测与安全数据集的运行方式见[模型、检索与 Agent 评测](../../docs/runbooks/model-interaction-eval.md)和 [AI 安全回归](../../docs/runbooks/security-regression.md)。

生产环境必须使用持久化任务、幂等和审计数据，并补齐下游查询、未知结果对账、积压告警与故障恢复。
