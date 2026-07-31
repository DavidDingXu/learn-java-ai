# Java AI Engineering Practice

这是一个面向 Java 后端工程师的 AI 应用工程实践项目，默认读者已经具备 Spring Boot 和 HTTP API 开发经验。项目围绕企业知识服务、C 端自助咨询、工单 AI 协同和 JDK8 老系统接入持续演进。

## 当前状态

当前为可运行、可测试的工程基线，已经完成：

- **可直接运行。** 三个 Java 21 HTTP 应用、独立 Eval Runner 和 Java 8 客户端均有单独构建入口；默认 `demo` 不连接数据库、模型或公司网络，未启用的能力会明确返回不可用。
- **企业 RAG。** Knowledge Service 覆盖文档上传与发布、版本冲突、租户和部门 ACL、增量索引、pgvector、混合检索、引用校验，以及 50 条检索样例和 Recall@K、HitRate@K、MRR、重复率、p95 指标。
- **C 端咨询与受控 Agent。** Customer Web 与 BFF 覆盖 SSE、会话、反馈、重试和工单升级；Ticket Agent 覆盖结构化规划、Tool 参数校验、风险分级、人工确认、幂等、未知结果和审计；Java 8 客户端按 OpenAPI 查询任务并提交员工决定。
- **质量与安全。** 项目提供模型、检索、Agent 和安全评测入口，30 条 Agent 路径样例、30 条安全合成样例，以及基于 Micrometer 的 Trace 与 Metric、低敏日志、并发限制、敏感信息扫描和跨平台发布门禁。
- **框架与协议实验。** 独立 labs 分别验证 DashScope 请求与响应适配边界、Spring AI Alibaba 条件路由、LangChain4j 业务端口迁移、AgentScope Tool 注册与权限裁决，以及 MCP/A2A 官方 SDK 的本地互操作。
- **生产接入边界。** `production` Profile 提供 PostgreSQL/Flyway、JWT 与下游 HTTP 适配器；公司 IdP、对象存储、共享会话与限流、真实 Legacy Tool、容量、告警和回滚仍需在目标环境完成配置与验收。

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

仓库只保留不含真实密钥的 `config/application.example.yml`。首次运行时复制为本地配置：

```bash
cp config/application.example.yml config/application.yml
```

Windows PowerShell 使用 `Copy-Item config/application.example.yml config/application.yml`。`config/application.yml` 已被 Git 忽略。使用 OpenAI 时只需在其中填写 `spring.ai.openai.api-key`，然后直接运行 Java 集成测试：

```bash
./mvnw \
  -pl services/knowledge-service \
  -Dtest=LiveModelSmokeIT \
  -Dspring.config.additional-location=file:config/application.yml \
  -Djava-ai.smoke.report-path=target/live-model-smoke.md \
  test
```

该测试检查模型连接、响应映射和业务校验，不会启动完整服务。Windows 使用相同的 Maven 参数，把入口换成 `mvnw.cmd` 即可。

本地 YAML 为了方便测试和文章演示，不是生产密钥方案。生产环境必须通过密钥管理系统或部署平台 Secret 覆盖同一配置键。普通单元测试不会读取真实密钥，也不会访问模型接口。

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

本地演示使用 Git 忽略的 `config/application.yml`。生产数据库密码、API Key、JWT 验签材料和客户端密钥必须由公司密钥系统或部署平台覆盖，不能进入 Git、镜像层或测试报告。

启用 `production` 只代表应用改用真实适配器。是否具备上线条件，还需要在目标环境验证数据库迁移、JWT 链路、向量检索、下游回执、并发容量、告警与回滚，不能从本机 health 或单次模型请求外推。

## 验证结果

架构和框架边界复核见 [Architecture Review](docs/reports/lesson-02-architecture-review.md) 与 [Framework Decision](docs/reports/lesson-03-framework-decision.md)。模型调用和 Golden Set 见 [Model Engineering](docs/reports/milestone-12.md)，各业务增量的测试范围与未覆盖项位于 `docs/reports/`。
