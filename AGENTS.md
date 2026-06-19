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
- 业务代码依赖 `AiCallGateway`，不依赖具体厂商 SDK。
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
