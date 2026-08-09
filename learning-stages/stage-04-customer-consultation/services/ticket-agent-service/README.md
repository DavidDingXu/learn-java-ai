# Ticket Agent Service

工单 AI 协同与受控 Tool 执行服务。该模块拥有 Agent 任务、规划步骤、确认决定、请求指纹、幂等和审计时间线；最终工单状态和业务写入仍由下游 JDK8 CRM/工单系统掌握。

## 主要边界

- `POST /api/v1/agent/tasks`：从不可变的咨询升级快照创建任务。
- `POST /api/v1/agent/tasks/{taskId}/runs`：在有限步数和服务端 Tool Catalog 内生成建议。
- `PUT /api/v1/agent/tasks/{taskId}/confirmation`：按任务版本和幂等键提交人工决定。
- `GET /api/v1/agent/tasks/{taskId}/audit`：读取低敏审计时间线。

接口契约见 [`contracts/openapi/agent-task-v1.yaml`](../../contracts/openapi/agent-task-v1.yaml)。模型只能提出 Tool 意图，权限、参数、风险、确认、幂等与审计都由应用层处理。

## 直接启动

先在根目录 `config/application.yml` 填好模型 API Key 并运行 `KnowledgeServiceApplication`，再用 IDE 运行 `TicketAgentServiceApplication`。启动后访问 `http://localhost:8082/actuator/health`。

默认调用真实 Chat Provider 和 Knowledge Service，任务、确认与审计保存在内存中，写 Tool 使用带幂等检查的本地实现。这样可以直接验证 Agent 主链路，但不能证明重启恢复或真实 Legacy Tool 已经完成联调。评测与安全数据集的运行方式见[模型、检索与 Agent 评测](../../docs/runbooks/model-interaction-eval.md)和 [AI 安全回归](../../docs/runbooks/security-regression.md)。

需要持久化时，将 `java-ai.persistence.mode` 改为 `jdbc`；接入公司身份时，将 `java-ai.security.mode` 改为 `jwt`；有真实 Legacy Tool 后，再把写 Tool 改为 HTTP。完整清单见[运行配置](../../docs/runbooks/runtime-configuration.md)。

生产环境必须使用持久化任务、幂等和审计数据，并补齐下游查询、未知结果对账、积压告警与故障恢复。
