# Java + AI 工程化落地实战

这个仓库是 **Java + AI 工程化落地实战** 的开源代码项目，面向有 Spring Boot 经验的 Java 后端工程师。代码从普通 Chat 调用治理、Prompt、RAG、Tool API、Agent、Eval 到全链路追踪逐步展开，目标是把 AI 能力接进可运行、可测试、可替换的后端工程。

读者 clone 后，准备 JDK 21+ 和 Maven 就能先跑测试；需要真实模型调用或基础设施联调时，再配置模型地址、API Key、Redis、MySQL、pgvector、MinIO。

这个项目优先使用 Spring AI / Spring AI Alibaba / LangChain4j 已经提供的底层能力。demo 和主项目重点补的是 Java 工程治理：权限、审计、Trace、Eval、成本、限流、灰度、回滚、坏 case 复盘和生产替换点。内存实现或规则实现只用于表达边界，不代表要重写框架能力。

## 技术主线

- 主线框架：Spring AI
- 国内生态补充：Spring AI Alibaba
- 对照框架：LangChain4j
- 高阶扩展：AgentScope Java
- 工程底座：Java 21、Spring Boot 3.x、Maven 多模块

## 当前模块

```text
ai-common          // 通用 DTO、trace、异常模型
ai-gateway-demo    // 普通 Chat 调用治理 demo
ai-output-demo     // Spring AI 结构化输出 demo
ai-streaming-demo  // SSE 流式输出 demo
ai-prompt-demo     // Prompt 模板、变量、版本 demo
ai-rag-demo        // 企业 RAG 权限过滤和引用 demo
ai-tool-demo       // Agent Tool API、人工确认、审计 demo
ai-agent-demo      // 受控工作流 Agent demo
ai-legacy-demo     // 老 JDK8 系统接入外部 Agent 服务 demo
ai-mcp-demo        // MCP Host / Client / Server 边界 demo
ai-a2a-demo        // A2A Agent Card、Skill、Task 状态 demo
ai-eval-demo       // Golden Set 评测 demo
ai-observability-demo // AI Trace、Span、成本追踪 demo
project-enterprise-rag // 企业制度 RAG 知识库主项目
project-helpdesk-agent // 企业工单 AI 助手主项目
docs/              // 路线图、框架选型、启动说明
docs/delivery/     // AI 项目上线前交付文档样例
```

## 两个主项目

这个仓库用两个主项目承接完整业务链路：一个解决企业知识库问题，一个解决业务 Agent 执行问题。

| 主项目 | 目标 | 先看什么 |
|---|---|---|
| `project-enterprise-rag` | 企业制度知识库，回答要有权限、有引用、可评测、可追踪 | `EnterpriseRagProjectTest`、`EnterpriseRagControllerTest` |
| `project-helpdesk-agent` | 企业工单 AI 助手，Agent 调工具要受控、可确认、可审计 | `HelpdeskAgentProjectTest`、`HelpdeskAgentControllerTest` |

demo 模块讲单点机制，两个主项目串完整链路。建议按 [docs/main-project-roadmap.md](docs/main-project-roadmap.md) 学习：先跑内存版边界，再把普通 Chat 治理、Spring AI 结构化输出、RAG、Tool、Eval、Trace 对应回主项目，最后替换基础设施。

## 快速运行

完整说明见 [docs/quick-start.md](docs/quick-start.md)。

1. 准备 JDK 21。
2. 复制 `.env.example`，按自己的模型服务填写 `AI_BASE_URL`、`AI_API_KEY`、`AI_CHAT_MODEL`，并在当前终端执行 `set -a && source .env && set +a`。
3. 在项目根目录先安装一次本地多模块依赖：

```bash
mvn install -DskipTests
```

4. 启动网关 demo：

```bash
mvn -pl ai-gateway-demo spring-boot:run
```

5. 验证接口：

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1001","message":"帮我总结一下：客户申请退款，但订单已经发货。"}'
```

## 学习方式

这个项目按专题组织 demo 和主项目。建议先进入对应 demo 模块运行测试，看清单点机制；再进入两个主项目，观察同一个能力在完整业务链路里的位置。

## 前端 demo 入口

有接口的模块都提供了对应的静态前端页面，页面放在各自 module 的 `src/main/resources/static/index.html`，启动服务后直接打开模块根路径即可。页面不是简单展示 JSON，而是按业务动作展示流式输出、证据引用、人工确认、Trace 时间线和评测结果。

第一次只启动单个模块前，先在项目根执行一次 `mvn install -DskipTests`，让 `ai-common` 等内部模块进入本地 Maven 仓库。

| 模块 | 启动命令 | 页面 |
|---|---|---|
| `ai-gateway-demo` | `mvn -pl ai-gateway-demo spring-boot:run` | `http://localhost:8081/` |
| `ai-output-demo` | `mvn -pl ai-output-demo spring-boot:run` | `http://localhost:8082/` |
| `ai-streaming-demo` | `mvn -pl ai-streaming-demo spring-boot:run` | `http://localhost:8083/` |
| `ai-prompt-demo` | `mvn -pl ai-prompt-demo spring-boot:run` | `http://localhost:8084/` |
| `ai-rag-demo` | `mvn -pl ai-rag-demo spring-boot:run` | `http://localhost:8086/` |
| `ai-tool-demo` | `mvn -pl ai-tool-demo spring-boot:run` | `http://localhost:8087/` |
| `ai-observability-demo` | `mvn -pl ai-observability-demo spring-boot:run` | `http://localhost:8088/` |
| `ai-agent-demo` | `mvn -pl ai-agent-demo spring-boot:run` | `http://localhost:8089/` |
| `ai-eval-demo` | `mvn -pl ai-eval-demo spring-boot:run` | `http://localhost:8090/` |
| `project-helpdesk-agent` | `mvn -pl project-helpdesk-agent spring-boot:run` | `http://localhost:8091/` |
| `project-enterprise-rag` | `mvn -pl project-enterprise-rag spring-boot:run` | `http://localhost:8092/` |
| `ai-mcp-demo` | `mvn -pl ai-mcp-demo spring-boot:run` | `http://localhost:8093/` |
| `ai-a2a-demo` | `mvn -pl ai-a2a-demo spring-boot:run` | `http://localhost:8094/` |

## 交付文档

AI 项目上线前不能只交代码。`docs/delivery/` 放了五份最小交付文档样例：

```text
01-requirements-spec.md
02-system-design.md
03-api-contract.md
04-eval-plan.md
05-release-plan.md
```

这些文档给生产评审提供最小事实版本：需求、设计、接口、评测和发布计划要能被业务、测试、安全和运维一起 review。
