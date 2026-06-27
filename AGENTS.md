# Java AI Engineering Practice 代码约束

## Scope

本目录是面向读者运行和二次修改的开源代码项目。

这里专注提供可运行 Java 代码、模块 README、启动说明、测试和本地依赖配置。

## 技术主线

- Java 21 + Spring Boot 3.x。
- 主线框架使用 Spring AI。
- Spring AI Alibaba 作为国内生态补充。
- LangChain4j 只用于对照 demo。
- AgentScope Java 只用于高阶 Agent / A2A / 多 Agent 专题。

## 代码边界

- Controller 不直接调用模型 SDK。
- `AiCallGateway` 只承接普通 Chat 调用里的路由、超时、重试、降级、审计、成本和 trace，不是 Spring AI 的上位替代。
- 结构化输出、Tool Calling、Advisor、Memory、Embedding、RAG、MCP 和模型响应元数据优先使用 Spring AI 原生 API 和类型，不把它们压扁成 `String prompt -> String content`。
- 业务代码不依赖具体厂商 SDK；应用服务可以组合 `ChatClient`、`PromptTemplate`、`BeanOutputConverter`、Advisor、Tool Callback、Memory、VectorStore 等 Spring AI 能力，并在外层补治理、观测、权限、审计和测试合同。
- Agent 不直接调用业务 Service，必须通过 Tool API / Facade。
- 写操作 Tool 必须有权限、幂等、状态检查和人工确认。
- RAG 必须考虑权限、引用和增量更新。
- Prompt 不散落在 Java 字符串里，应该通过模板、变量、版本演进。
- 模型调用、RAG 检索、Tool 调用、Agent 步骤都要能进入 trace。

## 可运行标准

- 所有密钥和私有地址走环境变量。
- 提供 `.env.example`。
- 每个 demo 有 README、启动命令、curl 示例。
- 每个核心模块有最小 JUnit 测试。
- 新增模块后同步更新根 `pom.xml` 和 `README.md`。

## 验证

默认验证命令：

```bash
mvn test
```
