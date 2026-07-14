# Java AI Engineering Practice 代码约束

## 项目定位

本目录是可独立 clone、构建、测试和二次开发的代码项目。只放源代码、测试、合同、数据集、ADR、Runbook 和运行说明，不放专栏正文、文章排期、运营文案或作者审稿记录。

当前阶段完成付费专栏发布基线：在既有模型问答、企业 RAG、C 端咨询、受控工单 Agent 和上线前工程能力基础上，四个隔离 lab 分别实现 Spring AI Alibaba Provider/检索/Graph、LangChain4j AI Services/RAG/Tool/共存、AgentScope Tool 权限与本地协作边界，以及 MCP/A2A 官方 SDK 互操作。Knowledge Service 与 Ticket Agent 的 shared-dev 均使用 PostgreSQL/Flyway；真实 IdP、共享会话存储、分布式租户限流、外部 JDK8 业务系统、持久 UNKNOWN 对账、生产 SSE 链路和容量仍需公司环境验证。不得把 lab 测试、进程内存储、健康检查或单次真实模型烟测扩写成生产链路已经验收。

## 构建边界

主 reactor 只包含：

- `services/knowledge-service`
- `services/ticket-agent-service`
- `apps/customer-bff`
- `quality/eval-runner`

其他产品保持独立：

- `integrations/jdk8-client` 使用独立 Java 8 POM，不继承 Java 21 父 POM。
- `apps/customer-web` 使用独立 Node 构建，不进入 Maven reactor。
- `labs/spring-ai-alibaba-lab`、`labs/langchain4j-lab`、`labs/agentscope-lab` 由独立 labs reactor 管理。
- `contracts`、`datasets` 和 `deploy` 是跨模块交付物目录，不伪装成 Java 模块。

服务不共享领域 JAR。需要复用的是 HTTP/OpenAPI 合同、JSON Schema、错误码和测试 fixture，不是另一服务的内部类。

## 服务所有权

- Knowledge Service 拥有知识文档、版本、权限检索、引用和知识问答。
- Ticket Agent Service 拥有工单 AI 协同、Tool 编排、确认任务、幂等和审计。
- Customer BFF 只负责 C 端身份委托、短期会话、协议聚合和流式代理，不拥有知识或工单业务事实。
- JDK8 CRM 或工单系统保留业务写操作、人工确认、最终状态和操作审计。
- Eval Runner 读取合同和数据集，通过版本化 HTTP 合同执行评测，不依赖服务内部代码；内部评测端点必须使用独立权限。

第一版服务间协作使用 HTTP/OpenAPI。任何服务都不能直接读写另一个服务的数据库，也不能为了“复用”引入对方实现模块。

## AI 代码边界

- 主线框架使用 Spring AI；Provider SDK 只能出现在基础设施适配器中。
- 不建设跨 Chat、RAG、Tool 和 Agent 的万能模型网关，也不把业务能力压成 `String -> String`。
- 应用层端口按业务语义定义，例如知识回答模型或工单规划模型；Spring AI 的 `ChatClient`、`ChatResponse`、Advisor、Tool Callback 和 VectorStore 保留在基础设施层。
- Controller 只处理协议、身份和校验，不直接调用模型、向量库、Tool 执行器或其他服务数据库。
- Prompt 使用资源文件和明确版本，不散落为不可追踪的长字符串。
- 默认测试不得访问真实模型。确定性模型协议合同、固定响应和真实模型验证必须明确区分。
- Prompt 必须按版本存放并区分系统规则、可信上下文和不可信用户输入。
- 结构化转换之后必须继续做业务校验；未知引用、越权动作和缺失拒答原因不能进入公开响应。
- SSE 事件名属于公开合同；开始输出后不得自动重试。
- Observation 和指标只允许固定低基数字段，不能把 Prompt、问题正文、用户或租户放进标签。

## RAG 与 Agent 边界

- RAG 必须在向量 TopK 前执行租户、部门、文档版本和有效期过滤；检索后 Java 过滤不能作为生产权限方案。
- 回答需要返回可追溯引用，失败样例和拒答进入评测集。
- Agent 只能通过受控 Tool API 访问业务能力，不能直接调用老系统 Service 或数据库。
- 写操作 Tool 必须验证身份、权限、状态、规范化参数、幂等键和确认凭证。
- 高风险确认必须由具备客服或审批角色的业务终端完成，Agent 不能自我确认。
- 无条件重试只允许无副作用读操作；写操作重试必须依赖业务幂等合同。

## Java 8 接入

- 老系统不升级运行时，也不引入 Spring AI。
- Java 8 客户端只依赖稳定 HTTP/OpenAPI 合同，不能依赖 Java 21 字节码或服务内部 DTO。
- 认证、超时、错误码、幂等键和任务查询必须进入合同测试。
- Java 8 构建必须使用包含 `java` 和 `javac` 的完整 JDK 8；浏览器 JRE 不算验证环境。

## 开发方式

- 新行为和缺陷修复按 TDD：先写失败测试，再写最小实现，最后重构。
- 不为未来能力预建空抽象、空模块、消息主题、数据库表或配置项。
- 所有密钥和私有地址使用环境变量，不提交真实值。
- 数据库结构以 Flyway 为唯一来源；引入数据库前先有真实用例和独立集成验证路径。
- 每次新增依赖都要说明归属、替换条件和是否影响无 Docker 验证。
- README 只描述当前已实现能力。计划中的能力放在 ADR 或交付路线中，并明确状态。

## 验证

macOS/Linux：

```bash
JAVA_AI_MAIN_JAVA_HOME=/path/to/jdk-21-or-newer \
JAVA_AI_JDK8_HOME=/path/to/full-jdk8 \
scripts/verify-unit.sh
```

Windows PowerShell：

```powershell
$env:JAVA_AI_MAIN_JAVA_HOME = "C:\\Java\\jdk-21"
$env:JAVA_AI_JDK8_HOME = "C:\\Java\\jdk8"
.\scripts\verify-unit.ps1
```

外部环境健康检查使用 `JAVA_AI_EXTERNAL_BASE_URL` 和 `verify-integration`。该脚本只检查 `/actuator/health`，不能据此声称模型、数据库、向量库、对象存储或端到端链路已经验证。

提交前运行与改动范围对应的 Node 测试、Maven `verify` 和 `git diff --check`。Windows 脚本在 macOS 上只能做静态合同检查，正式发布前仍需要真实 Windows 运行证据。
