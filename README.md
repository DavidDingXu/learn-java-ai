# Java AI 应用开发实战

沿一条客户服务链路，学习模型调用、企业 RAG、受控 Agent 与 Java 系统接入。从第一条真实模型调用，逐步走到评测、安全、发布与回滚。

**[在线阅读](https://daviddingxu.github.io/learn-java-ai/) · [完整目录](docs/course/README.md) · [分阶段源码](learning-stages/README.md) · [运行指南](docs/getting-started.md)**

**课程已完结 · 52 篇完整文章 · 7 个学习模块 · 7 个可运行阶段**

## 适合谁学

适合熟悉 Java、Spring Boot、数据库与 HTTP API，希望系统学习 AI 应用开发的后端工程师。课程从业务问题解释模型、RAG 和 Agent，再进入接口、实现、测试与工程取舍。

仓库包含完整正文、配图、分阶段源码、前端页面、接口契约与评测数据。Spring AI 作为主线，Spring AI Alibaba、LangChain4j、AgentScope、MCP 和 A2A 在独立实验中对照学习。

## 学完能做什么

- 实现有权限过滤和可核对引用的企业知识问答，处理文档、检索、索引与评测。
- 将咨询升级为工单，让 Agent 在身份、工具权限、人工确认、幂等和审计约束下执行。
- 通过 HTTP/OpenAPI 接入 JDK8 老系统，评估模型与框架替换，并完成安全、观测和发布检查。

## 课程路线

整套课程围绕同一条业务链逐步展开：**企业知识问答 → 客户咨询 → 工单接管 → 受控 Agent → JDK8 系统执行 → 评测与上线治理**。

| 模块 | 文章 | 学习重点 |
| --- | --- | --- |
| [系统边界与第一条调用](docs/course/README.md#module-01) | 01–06 | 服务职责、Spring AI、模型调用、身份与 JDK8 边界 |
| [模型调用工程化](docs/course/README.md#module-02) | 07–12 | Prompt、结构化输出、SSE、超时、重试与模型评测 |
| [企业 RAG](docs/course/README.md#module-03) | 13–21 | 文档、pgvector、ACL、引用、索引任务与检索评测 |
| [客户咨询与工单升级](docs/course/README.md#module-04) | 22–25 | BFF、会话、证据展示、反馈与转人工 |
| [受控 Agent 与老系统接入](docs/course/README.md#module-05) | 26–34 | Tool、风险分级、人工确认、幂等与 JDK8 客户端 |
| [评测、安全与上线治理](docs/course/README.md#module-06) | 35–39 | 安全回归、观测、配置、发布与回滚 |
| [框架与协议迁移](docs/course/README.md#module-07) | 40–52 | 框架替换、MCP/A2A 互操作与课程收官 |

## 动手实践

初次学习可按 [分阶段源码](learning-stages/README.md) 的说明，用 IDEA 打开对应阶段的 `pom.xml`，观察每个阶段新增的能力；希望体验完整链路时，从主工程的 [运行指南](docs/getting-started.md) 开始。

| 实践入口 | 关注什么 |
| --- | --- |
| [Knowledge Service](services/knowledge-service/README.md) | 模型调用、文档、检索、RAG 与引用 |
| [客户咨询页面](apps/customer-web/README.md) | 会话、流式回答、证据展示与工单升级 |
| [工单 Agent](services/ticket-agent-service/README.md) | 规划、Tool、人工确认与审计 |
| [JDK8 客户端](integrations/jdk8-client/README.md) | 跨版本系统的接口契约与调用边界 |
| [评测 Runner](quality/eval-runner/README.md) | 模型、检索、Agent 和安全评测 |
| [框架与协议实验](labs/README.md) | 在相同业务契约下比较不同实现 |

## 开始学习

1. 从 [第一篇文章](docs/course/01-column-introduction.md) 了解业务主线和学习顺序。
2. 运行主工程时准备 JDK 21+，复制 [模型配置示例](config/application-default.example.yml) 为 `config/application-default.yml`，填写模型连接信息。
3. 按 [运行指南](docs/getting-started.md) 启动 `KnowledgeServiceApplication`，先观察第一条模型回答，再逐步接入 RAG、工单和前端。完整 RAG 的数据库准备见 [本地准备指南](docs/runbooks/rag-prerequisites.md)。

## 交流与许可

欢迎通过 [Issue](https://github.com/DavidDingXu/learn-java-ai/issues) 反馈文章、运行或迁移问题，提交改进前请阅读 [贡献指南](CONTRIBUTING.md)。

- 文章、图示与课程文档：[CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)，署名、非商业使用、相同方式共享。
- 示例代码、脚本与网站代码：[PolyForm Noncommercial 1.0.0](LICENSE)。

商业使用需另行授权；具体范围与第三方内容说明见 [许可说明](LICENSING.md)。
