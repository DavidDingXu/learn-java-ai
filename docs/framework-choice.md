# 框架选型说明

## 结论

本项目采用“一主两辅一扩展”：

| 定位 | 框架 | 用法 |
|---|---|---|
| 主线框架 | Spring AI | 主项目底座，贯穿模型调用、RAG、Tool、MCP、Observability |
| 国内生态补充 | Spring AI Alibaba | DashScope、中文 RAG、Graph、Agent Framework、国内模型接入 |
| 对照框架 | LangChain4j | 讲 AI Service、Tool、Memory、RAG 的另一套 Java 写法 |
| 高阶扩展 | AgentScope Java | 多 Agent、A2A、复杂 Agent 编排 |

## 为什么主线选 Spring AI

这个项目的主题是 Java + AI 工程化落地，而不是框架测评。主线需要满足几个条件：

1. 和 Spring Boot 集成自然。
2. 能覆盖模型调用、RAG、Tool、MCP、可观测性。
3. 适合写成 Maven 多模块工程。
4. 方便和 Redis、MySQL、pgvector、Micrometer、OpenTelemetry、WebFlux 这些 Java 工程组件集成。

Spring AI 更适合作为主线。Spring AI Alibaba 用来补齐国内模型和 Alibaba 生态。LangChain4j 和 AgentScope 不作为主项目底座，避免整套课程变成框架拼盘。

## 学习顺序

```text
Spring AI 主线能力
  -> Spring AI Alibaba 国内生态
  -> LangChain4j 对照理解
  -> AgentScope Java 高阶 Agent 编排
```

## 项目约束

- 生产级项目默认使用 Spring AI。
- 国内模型调用能力可以通过 Spring AI Alibaba 或 OpenAI-compatible endpoint 接入。
- 所有模型密钥通过环境变量配置。
- 不复用参考教程的业务域、类名、Prompt 和测试数据。
