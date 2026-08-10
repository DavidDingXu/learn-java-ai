# 运行配置验证

## 已验证

- Knowledge Service、Ticket Agent Service 和 Customer BFF 的主资源目录各自只有一个 `application.yml`。
- 默认运行使用真实 Chat Provider、固定身份和跨服务 HTTP；会话、Agent 状态、审计和写 Tool 使用进程内实现。
- 仓库保留带占位 Key 的 `config/application-default.yml`，Knowledge Service 和 Ticket Agent Service 会自动读取。使用默认 OpenAI 配置时只需填写 API Key，运行后不能提交真实值。
- 将 `java-ai.knowledge.mode` 改为 `postgres-rag` 后，Knowledge Service 会启用 PostgreSQL/pgvector、Embedding、索引与检索；固定本地身份不要求 HTTP 请求携带 Token。
- PostgreSQL、JWT、Token Exchange 和下游地址都保留明确配置键，由对应 `mode` 选择适配器。密钥占位值必须由部署平台或密钥系统覆盖。
- Knowledge Service 和 Ticket Agent Service 只保留直接控制模型、数据库、JWT 和 Tool 的配置，不使用没有调用方的外部集成总开关。
- `test` Profile 只装配自动化测试配置；确定性模型不会进入默认运行路径，也不会伪造外部调用成功。
- 读者只需填写项目级配置，再从 IDEA 启动对应的 `main` 方法；不需要先执行构建或测试命令。

## 读者启动路径

- 模型问答：填写 `config/application-default.yml`，启动 `KnowledgeServiceApplication.main()`，调用文章给出的 HTTP 接口。
- 受控 Agent：保持同一份模型配置，启动 `TicketAgentServiceApplication.main()`，创建任务后观察计划、确认与执行状态。
- 客服聚合：再启动 `CustomerBffApplication.main()`，从统一入口观察知识回答与工单状态。
- PostgreSQL RAG：将 `java-ai.knowledge.mode` 改为 `postgres-rag`，填写同一配置文件中的数据库与 Embedding 参数，再启动 Knowledge Service。

这些入口在 macOS 与 Windows 上使用相同的 YAML 配置键。读者不需要维护 Shell、PowerShell 或环境变量两套启动方式。

## 外部验证边界

日常代码回归不访问模型和外部基础设施。仓库中的模型配置只保留不可用的占位 Key，生产密钥由部署平台覆盖。数据库、IdP、对象存储、业务 Tool、Windows 运行和容量结论仍需要目标环境测试；统一配置路径不会把本地结果扩大成生产验收。
