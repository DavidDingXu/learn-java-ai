# Quick Start

本项目是可独立运行的开源代码仓库。clone 后，准备好 JDK 21+ 和 Maven 就可以先跑测试；需要真实调用模型或本地基础设施时，再按 `.env.example` 和 `docker-compose.yml` 配置。

## 1. 准备环境

- JDK 21+
- Maven 3.9+
- Docker Desktop

本机如果有多个 JDK，可以显式指定：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" java -version
```

## 2. 配置模型和基础设施

复制环境变量模板：

```bash
cp .env.example .env
```

填写自己的配置：

```text
AI_BASE_URL=https://api.openai.com
AI_API_KEY=replace-with-your-api-key
AI_CHAT_MODEL=gpt-4o-mini
AI_GATEWAY_MAX_ATTEMPTS=2
AI_GATEWAY_CALL_TIMEOUT=30s

REDIS_HOST=localhost
MYSQL_URL=jdbc:mysql://localhost:3306/java_ai_practice
PGVECTOR_URL=jdbc:postgresql://localhost:5432/java_ai_practice
MINIO_ENDPOINT=http://localhost:9000
```

让当前终端读取 `.env`：

```bash
set -a
source .env
set +a
```

当前大部分 demo 和测试使用内存仓储或 fake client，不要求先启动 Docker。需要验证 Redis、MySQL、pgvector、MinIO 接入时，再启动本地依赖：

```bash
docker compose -f docker/docker-compose.yml up -d
```

## 3. 运行测试

```bash
mvn test
```

如果本机默认 JDK 不是 21+，先执行前面的 `JAVA_HOME` 设置，再运行 Maven。

## 4. 启动第一个 demo

第一次只启动单个模块前，先在项目根安装本地多模块依赖：

```bash
mvn install -DskipTests
```

```bash
mvn -pl ai-gateway-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8081/
```

页面会调用同一个 `POST /api/ai/chat` 接口，并把模型回复、`traceId`、模型名称和耗时展示出来。

验证接口：

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1001","message":"客户申请退款，但订单已经发货。"}'
```

## 5. 跑两个主项目

如果你想先看完整业务链路，直接跑两个主项目的测试：

```bash
mvn -pl project-enterprise-rag test
mvn -pl project-helpdesk-agent test
```

企业制度 RAG 重点看：

```text
EnterpriseRagProjectTest
EnterpriseRagControllerTest
EvidenceGovernanceServiceTest
```

工单 AI 助手重点看：

```text
HelpdeskAgentProjectTest
HelpdeskAgentControllerTest
```

也可以启动 REST 服务做接口验证。

企业制度 RAG：

```bash
mvn -pl project-enterprise-rag spring-boot:run
```

默认端口是 `8092`。可以直接打开页面完成上传、问答和评测：

```text
http://localhost:8092/
```

也可以用 curl 上传制度文档：

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/documents \
  -H 'Content-Type: application/json' \
  -d '{
    "documentId": "refund-policy-2026",
    "tenantId": "tenant-a",
    "departments": ["support"],
    "type": "POLICY",
    "content": "退款制度\n发货后退款需先核对物流状态。\n高金额退款必须转人工复核。"
  }'
```

发起问答：

```bash
curl -X POST http://localhost:8092/api/enterprise-rag/answers \
  -H 'Content-Type: application/json' \
  -d '{
    "question": "客户申请退款但订单已发货怎么办",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

重点看返回里的 `citations` 和 `trace.steps`。

工单 AI 助手：

```bash
mvn -pl project-helpdesk-agent spring-boot:run
```

默认端口是 `8091`。可以先打开页面观察建议生成、人工确认关闭和 Trace：

```text
http://localhost:8091/
```

也可以用 curl 跑退款工单场景：

```bash
curl http://localhost:8091/api/helpdesk-agent/scenarios/refund
```

重点看返回里的：

```text
riskLevel = HIGH
requiredAction = MANUAL_REVIEW
toolNames = ticket.lookup, order.lookup, policy.search
traceStepNames = ticket.lookup, order.lookup, policy.search, advice.compose, approval.plan
```

关闭工单接口需要人工确认和确认 token：

```bash
curl -X POST http://localhost:8091/api/helpdesk-agent/tickets/close \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "humanApproved": true,
    "confirmationToken": "confirm-close-1001",
    "userId": "lead-1",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

同一个 `confirmationToken` 重复提交不会重复执行。工单已经关闭后，换一个 token 也不能绕过状态检查。

完整主项目学习路线见 [main-project-roadmap.md](main-project-roadmap.md)。

## 6. 模块入口

下表里的启动命令默认已经在项目根执行过 `mvn install -DskipTests` 或 `mvn test`。

| 模块 | 目标 | 前端页面 |
|---|---|---|
| `ai-gateway-demo` | 普通 Chat 调用治理 | `http://localhost:8081/` |
| `ai-streaming-demo` | SSE 流式输出 | `http://localhost:8083/` |
| `ai-prompt-demo` | Prompt 模板和版本 | `http://localhost:8084/` |
| `ai-output-demo` | Spring AI 结构化输出 | `http://localhost:8082/` |
| `ai-rag-demo` | 企业 RAG 权限过滤和引用 | `http://localhost:8086/` |
| `ai-tool-demo` | Agent Tool API、人工确认、审计 | `http://localhost:8087/` |
| `ai-observability-demo` | Trace、Span、成本追踪 | `http://localhost:8088/` |
| `ai-agent-demo` | 受控工作流 Agent | `http://localhost:8089/` |
| `ai-eval-demo` | Golden Set 评测 | `http://localhost:8090/` |
| `project-helpdesk-agent` | 企业工单 AI 助手主项目 | `http://localhost:8091/` |
| `project-enterprise-rag` | 企业制度 RAG 知识库主项目 | `http://localhost:8092/` |
| `ai-legacy-demo` | 老 JDK8 系统接入外部 Agent 服务 | 暂无 HTTP 页面 |
| `ai-mcp-demo` | MCP Host / Client / Server 边界 | `http://localhost:8093/` |
| `ai-a2a-demo` | A2A Agent Card、Skill、Task 状态 | `http://localhost:8094/` |
