# Java AI Engineering Practice

这是一个面向 Java 后端工程师的 AI 应用工程实践项目，默认读者已经具备 Spring Boot 和 HTTP API 开发经验。项目围绕企业知识服务、C 端自助咨询、工单 AI 协同和 JDK8 老系统接入持续演进。

## 当前状态

当前为可运行、可测试的工程基线，已经完成：

- 三个可启动的 Java 21 HTTP 应用和健康检查。
- 一个依赖轻量的 Eval Runner 入口。
- 一个由 JDK 8 独立编译和测试的老系统客户端工程。
- Spring AI Alibaba、LangChain4j、AgentScope 和协议互操作四个有实现、有测试的隔离实验构建。
- macOS/Linux 与 Windows 两套验证脚本。
- 不依赖外部数据库、模型密钥或业务网络的日常测试路径。
- Knowledge Service 的 Spring AI、WebFlux、可靠性和可观测依赖边界。
- Customer BFF 的客户 JWT、RFC 8693 Token Exchange、下游客户端认证与 audience/scope 隔离。
- 测试替身与正式运行配置隔离，以及 JDK 21/JDK 8 CI 校验。
- 第一条固定政策上下文问答、Spring AI 业务适配器、确定性模型协议回归测试和模型接口 Smoke 入口。
- 版本化 Prompt、信任分区、结构化输出、业务校验和事件格式稳定的 SSE。
- 面向知识回答用例的超时、并发、断路器和安全读重试。
- 独立 HTTP Eval Runner、5 条 Golden Set、接口回归评测和模型评测入口。
- Micrometer Observation、Spring AI 原生观测和 HTTP Trace 关联。
- 受 `knowledge:write` 保护的知识文档上传与发布接口、JWT 租户隔离、JDBC 持久化、版本冲突、重复内容校验、本地文件对象存储和确定性政策切分。
- PostgreSQL/pgvector 结构、Embedding 模型隔离、TopK 前 ACL 与有效期过滤。
- 向量与 trigram 混合检索、RRF 融合、可配置候选预算和受控 Rerank 边界。
- JWT 中租户、主体和部门范围到检索 SQL 的完整传递。
- Flyway V1-V4 数据库结构、发布事务内的 ACL 与索引任务创建、任务租约、原子领取、对象正文回读和 pgvector 分块写入。
- 业务发布版本与检索版本分离；新索引写完前继续读取上一版，写入时通过 `leaseAttempt` fencing 拒绝失去租约的 Worker。
- 版本化检索 Golden Set、受 `knowledge:eval` 保护的评测接口、Recall@K、HitRate@K、MRR、重复率、p95 延迟和阈值检查。
- C 端完整回答与 SSE 接口、会话/尝试标识，以及明确的取消和异常状态。
- 带 TTL、消息窗口、Token 估算、裁剪和事实隔离摘要的短时会话。
- 绑定回答尝试的反馈、可追踪重试和不可变工单升级快照。
- Ticket Agent Service 的委托 JWT 校验、幂等键、请求指纹、重复返回与冲突拒绝。
- Ticket Agent Service 的 PostgreSQL/Flyway 任务、确认执行状态和审计持久化，以及不在数据库事务中调用远程 Tool 的两阶段执行边界。
- 受限步数 Agent 状态机、Spring AI 2.0 结构化规划、模型元数据和模型接口烟测。
- 服务端 Tool 目录、参数白名单、只读知识查询、写操作风险分级和人工确认单。
- 确认幂等、乐观版本、确定性拒绝与未知执行结果分流、低敏审计事件。
- Knowledge Service 与 Java 8 Legacy Tool 的 HTTP 适配器，以及开发联调用委托 JWT 签发器。
- 独立 Java 8 任务查询与确认客户端，认证、连接池、超时、错误映射和未知结果异常。
- Agent Golden Set、三令牌公开 HTTP 评测器和 JSON/Markdown 报告入口。
- Injection 绕过确认、合成 PII 审计泄露和身份字段污染的安全回归数据集与跨平台入口。
- Ticket Agent 规划次数、Token 分布、Tool 结果与耗时的 Micrometer 指标，并暴露受管理网络保护的 Prometheus 端点。
- Agent Run 使用公平信号量限制并发，并提供稳定的 429 错误码与响应格式，以及异常路径的许可释放测试。
- 每个服务一份运行配置、统一 `.env` 参数入口、测试隔离和跨平台验证命令。
- 跨平台 release gate：全构建、接口与规则回归、Java 8、敏感信息扫描和可选外部健康检查。
- DashScope Provider 适配、国内 Embedding/Rerank 同集评测和 Spring AI Alibaba 人工确认 Graph。
- LangChain4j AI Services、租户受限 RAG、Tool 循环、结构化输出和按业务能力共存策略。
- AgentScope 2.0 Tool 权限复用、人工确认事件、MCP 外部 Tool 注册和 A2A 任务状态边界。
- MCP Java SDK 2.0.0 的 Streamable HTTP 初始化、工具发现和只读调用互操作。
- A2A Java SDK 1.1.0.Final 的 Agent Card 发现、Skill 准入、JSON-RPC 消息发送与 Task 映射。
- 50 条检索、30 条 Agent 和 30 条安全合成回归数据；公司落地时需要按业务分布替换或扩展。
- Customer BFF、Knowledge Service、Ticket Agent Service 和 JDK8 Legacy Tool 四份 OpenAPI 文档。

当前代码覆盖模型调用、企业 RAG、C 端会话与信任交互、幂等工单升级、受控 Agent、Java 8 客户端、安全回归、标准指标、跨平台发布检查，以及框架和协议隔离实验。业务接口要求受信身份，不从请求体读取租户和客户身份。Knowledge Service 使用 PostgreSQL/pgvector 保存文档元数据、ACL、索引任务、分块和检索版本指针；上传原文默认写入本地文件目录。多实例部署前，应把原文适配器换成 S3 兼容对象存储，并在目标数据库上完成迁移、并发、备份恢复和容量测试。Customer BFF 的进程内会话与限流也需要换成共享实现。目标 IdP、外部 JDK8 Tool、Customer Web、UNKNOWN 对账和端到端容量测试仍需在公司环境接入。

## 模块边界

```text
services/knowledge-service       企业知识与知识问答边界
services/ticket-agent-service    工单 AI 协同与受控 Tool 边界
apps/customer-bff                C 端身份委托、会话和协议聚合边界
quality/eval-runner              独立评测入口

integrations/jdk8-client         独立 Java 8 构建
apps/customer-web                独立前端构建边界
labs/spring-ai-alibaba-lab       Spring AI Alibaba 迁移实验
labs/langchain4j-lab             LangChain4j 迁移实验
labs/agentscope-lab              AgentScope 扩展实验
labs/protocol-interop-lab         MCP/A2A 官方 SDK 互操作实验
contracts                        OpenAPI 与 Schema
datasets                         Golden Set 与坏案例数据
deploy                           部署清单和环境说明
```

根 Maven reactor 只聚合三个服务和 Eval Runner。labs、JDK8 客户端和 Customer Web 独立构建，避免框架与协议实验依赖、Java 8 字节码和前端工具链污染主项目。

服务之间通过版本化 HTTP/OpenAPI 接口协作，不共享领域 JAR，也不跨服务访问数据库。详细决策见：

- [系统上下文](docs/architecture/system-context.md)
- [客户咨询完整时序](docs/architecture/customer-consultation-flow.md)
- [服务与数据所有权](docs/architecture/service-ownership.md)
- [为什么以 Spring AI 为主线](docs/adr/0001-spring-ai-mainline.md)
- [为什么拆分构建边界](docs/adr/0002-build-boundaries.md)
- [框架实验为什么保持隔离](docs/adr/0003-framework-lab-isolation.md)
- [框架选型矩阵](docs/decisions/framework-selection-matrix.md)

## 技术基线

- 主 reactor：Java 21 字节码、Spring Boot 4.1.0、Spring AI BOM 2.0.0。
- labs reactor：Spring AI Alibaba 1.1.2.3、LangChain4j 1.18.0、AgentScope 2.0.0、MCP Java SDK 2.0.0、A2A Java SDK 1.1.0.Final。
- JDK8 客户端：Java 8、独立 Maven 构建。
- Maven Wrapper：3.9.14。

完整锁定见 [版本基线](docs/version-baseline.md)。这些是项目选择，不等同于“所有公司项目都必须使用的最新版本”。

## 环境要求

至少准备：

- Node.js 24 或更高版本，用于仓库契约测试和本地联调脚本。
- 一个包含 `java` 和 `javac` 的 JDK 21 或更高版本。使用高于 21 的 JDK 时只能验证 `--release 21` 编译兼容性，CI 仍需在 JDK 21 上运行。
- 一个包含 `java` 和 `javac` 的完整 JDK 8，用于独立客户端构建。

本地可使用高于 21 的完整 JDK 编译 Java 21 字节码，但 CI 仍需在 JDK 21 上运行；老系统客户端必须使用独立完整 JDK 8。具体配置见 [本地工具链手册](docs/runbooks/local-toolchain.md)。

## 一次跑完

macOS/Linux：

```bash
export JAVA_AI_MAIN_JAVA_HOME=/path/to/jdk-21-or-newer
export JAVA_AI_JDK8_HOME=/path/to/full-jdk8
scripts/verify-unit.sh
```

Windows PowerShell：

```powershell
$env:JAVA_AI_MAIN_JAVA_HOME = "C:\\Java\\jdk-21"
$env:JAVA_AI_JDK8_HOME = "C:\\Java\\jdk8"
.\scripts\verify-unit.ps1
```

脚本会运行项目 Node 契约、主 reactor、labs reactor 和 Java 8 客户端。

## 分别构建

主 reactor：

```bash
./mvnw verify
```

框架实验：

```bash
./mvnw -f labs/pom.xml verify
```

Java 8 客户端：

```bash
JAVA_HOME=/path/to/full-jdk8 PATH="$JAVA_HOME/bin:$PATH" \
  ./mvnw -f integrations/jdk8-client/pom.xml verify
```

Windows 可使用 `mvnw.cmd` 和对应 JDK 环境变量执行相同 POM。

## 启动服务骨架

三个应用都提供 Actuator health。Knowledge Service 提供知识回答和 SSE，Customer BFF 提供 C 端回答、SSE、反馈、重试和工单升级，Ticket Agent Service 提供任务接收、运行、查询、确认与审计：

先从示例生成本地参数文件并填写连接信息：

```bash
cp .env.example .env
```

三个服务都会通过 `spring.config.import` 读取项目根目录的 `.env`，无需切换 Spring Profile。然后分别启动：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

默认端口分别为 8081、8082 和 8080。验证示例：

```bash
curl http://localhost:8081/actuator/health
```

期望返回包含 `"status":"UP"` 的 JSON。`/actuator/env` 不对外暴露。

知识文档上传、发布和索引见 [Knowledge Ingestion](docs/runbooks/knowledge-ingestion.md)。模型接口 Smoke 见 [Live Model Smoke](docs/runbooks/live-model-smoke.md)，模型、检索和 Agent 评测见 [模型及检索评测](docs/runbooks/model-interaction-eval.md)。安全回归见 [AI Security Regression](docs/runbooks/security-regression.md)，统一参数和启动方式见 [Runtime Configuration](docs/runbooks/runtime-configuration.md)，发布入口见 [Release Checklist](docs/runbooks/release-checklist.md)。Ticket Agent 的模型接口烟测使用 `scripts/run-agent-live-model-smoke.sh` 或对应 PowerShell 脚本。检索和 Agent 评测通过显式 URL 与凭证连接目标测试环境，本机或 CI 均运行同一个 Eval Runner。

## 外部环境检查

当前外部脚本只检查一个已部署服务的健康端点：

```bash
JAVA_AI_EXTERNAL_BASE_URL=https://test.example.com scripts/verify-integration.sh
```

Windows 使用：

```powershell
$env:JAVA_AI_EXTERNAL_BASE_URL = "https://test.example.com"
.\scripts\verify-integration.ps1
```

该脚本只检查健康端点，不覆盖数据库、向量检索、对象存储、模型或端到端业务。`external-integration` Maven profile 提供 PostgreSQL/pgvector 集成测试入口；是否通过以目标测试库上的具体运行结果为准，不能从健康检查推断。

## 环境变量示例

`.env.example` 只列出当前代码和验证脚本实际消费的变量。模型、Embedding、数据库和委托 JWT 均通过环境变量注入，不写入源码或默认配置。

## 验证结果

架构和框架边界复核见 [Architecture Review](docs/reports/lesson-02-architecture-review.md) 与 [Framework Decision](docs/reports/lesson-03-framework-decision.md)。模型调用和 Golden Set 见 [Model Engineering](docs/reports/milestone-12.md)，各业务增量的测试范围与未覆盖项位于 `docs/reports/`。
