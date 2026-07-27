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
- 单元测试启用固定返回结果的假模型和假服务，正式运行不启用这些测试实现，并通过 JDK 21/JDK 8 CI 校验。
- 第一条固定政策上下文问答、Spring AI 业务适配器、固定模型响应的协议回归测试和真实模型接口 Smoke 入口。
- 版本化 Prompt，将系统规则、授权知识和不可信用户输入分开组装，配套结构化输出、业务校验和事件格式稳定的 SSE。
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
- 每个服务一份运行配置、统一模型演示配置、测试隔离和跨平台验证命令。
- 跨平台 release gate：全构建、接口与规则回归、Java 8、敏感信息扫描和可选外部健康检查。
- Pull Request 依赖变更审查，新增高危漏洞时阻止合并。
- DashScope Provider 适配、国内 Embedding/Rerank 同集评测和 Spring AI Alibaba 人工确认 Graph。
- LangChain4j AI Services、租户受限 RAG、Tool 循环、结构化输出和按业务能力共存策略。
- AgentScope 2.0 Tool 权限复用、人工确认事件、MCP 外部 Tool 注册和 A2A 任务状态边界。
- MCP Java SDK 2.0.0 的 Streamable HTTP 初始化、工具发现和只读调用互操作。
- A2A Java SDK 1.1.0.Final 的 Agent Card 发现、Skill 准入、JSON-RPC 消息发送与 Task 映射。
- 50 条检索、30 条 Agent 和 30 条安全合成回归数据；公司落地时需要按业务分布替换或扩展。
- Customer BFF、Knowledge Service、Ticket Agent Service 和 JDK8 Legacy Tool 四份 OpenAPI 文档。

当前代码覆盖模型调用、企业 RAG、C 端会话与交互页面、幂等工单升级、受控 Agent、Java 8 客户端、安全回归、标准指标、跨平台发布检查，以及框架和协议隔离实验。业务接口要求受信身份，不从请求体读取租户和客户身份。Knowledge Service 使用 PostgreSQL/pgvector 保存文档元数据、ACL、索引任务、分块和检索版本指针；上传原文默认写入本地文件目录。多实例部署前，应把原文适配器换成 S3 兼容对象存储，并在目标数据库上完成迁移、并发、备份恢复和容量测试。Customer BFF 的进程内会话与限流也需要换成共享实现。目标 IdP、外部 JDK8 Tool、生产网关、UNKNOWN 对账和端到端容量测试仍需在公司环境接入。

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

## 快速开始

启动主服务或运行日常测试，只需本机已安装一个包含 `java` 和 `javac` 的 JDK 21 或更高版本。项目自带 Maven Wrapper，不需要另行安装 Maven，也不需要配置项目专用的 Java 环境变量。

运行主构建：

```bash
./mvnw verify
```

Windows 使用：

```powershell
.\mvnw.cmd verify
```

这条命令覆盖 Knowledge Service、Ticket Agent Service、Customer BFF 和 Eval Runner，不访问真实模型、数据库或外部业务网络。

## 运行真实模型专项测试

项目根目录的 `config/application.yml` 已提供 OpenAI API 地址、Chat 模型和 Embedding 模型默认值。使用 OpenAI 时只需填写 `spring.ai.openai.api-key`，然后直接运行 Java 集成测试：

```bash
./mvnw \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Dspring.config.additional-location=file:config/application.yml \
  test
```

该测试检查模型连接、响应映射和业务校验，不会启动完整服务。Windows 使用相同的 Maven 参数，把入口换成 `mvnw.cmd` 即可。

这份配置是为了方便本地测试和文章演示。不要提交真实 API Key；生产环境必须通过密钥管理系统或部署平台 Secret 覆盖该配置。普通单元测试不会读取真实密钥，也不会访问模型接口。

## 完整仓库验证

框架实验使用独立构建：

```bash
./mvnw -f labs/pom.xml verify
```

Java 8 客户端需要完整 JDK 8，仓库契约测试需要 Node.js 24 或更高版本。只有执行全仓库回归时才需要这两项工具。

仓库提供 `scripts/verify-unit.sh` 和对应的 PowerShell 脚本，用于一次运行 Node 契约、主构建、labs 和 Java 8 客户端。脚本会自动寻找本机已安装的完整 JDK；它是聚合验证入口，不是启动普通 Java 服务的前置条件。

## 直接启动三个服务

三个应用默认使用 `demo` Profile，不连接数据库、身份平台、模型或下游服务。Knowledge Service 关闭索引与模型能力，Ticket Agent 使用内存任务库并关闭远程 Tool，Customer BFF 关闭令牌交换与下游调用。这条路径用于启动应用、查看结构和检查 health；访问被关闭的外部能力时会明确返回不可用，不会伪造成功结果。

从项目根目录分别启动：

```bash
./mvnw -pl services/knowledge-service spring-boot:run
./mvnw -pl services/ticket-agent-service spring-boot:run
./mvnw -pl apps/customer-bff spring-boot:run
```

默认端口分别为 8081、8082 和 8080，无需先设置业务环境变量。验证示例：

```bash
curl http://localhost:8081/actuator/health
```

期望返回包含 `"status":"UP"` 的 JSON。`/actuator/env` 不对外暴露。

知识文档上传、发布和索引见 [Knowledge Ingestion](docs/runbooks/knowledge-ingestion.md)。模型接口测试见 [Live Model Smoke](docs/runbooks/live-model-smoke.md)，模型、检索和 Agent 评测见 [模型及检索评测](docs/runbooks/model-interaction-eval.md)。安全回归见 [AI Security Regression](docs/runbooks/security-regression.md)，统一参数和启动方式见 [Runtime Configuration](docs/runbooks/runtime-configuration.md)。

## 接入生产环境

每个服务的 `application.yml` 都包含一个 `production` 文档，列出真实数据库、模型、身份系统和下游服务所需的配置路径。仓库中的域名、账号和密钥都是不可用的占位值。

本地演示为了减少步骤，允许在 `config/application.yml` 中临时填写模型 API Key，但填入真实值后不能提交。生产数据库密码、API Key、JWT 验签材料和客户端密钥必须由公司密钥系统或部署平台覆盖，不能进入 Git、镜像层或测试报告。

启用 `production` 只代表应用改用真实适配器。是否具备上线条件，还需要在目标环境验证数据库迁移、JWT 链路、向量检索、下游回执、并发容量、告警与回滚，不能从本机 health 或单次模型请求外推。

## 验证结果

架构和框架边界复核见 [Architecture Review](docs/reports/lesson-02-architecture-review.md) 与 [Framework Decision](docs/reports/lesson-03-framework-decision.md)。模型调用和 Golden Set 见 [Model Engineering](docs/reports/milestone-12.md)，各业务增量的测试范围与未覆盖项位于 `docs/reports/`。
