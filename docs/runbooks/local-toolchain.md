# 本地直接启动

## 第一次只启动 Knowledge Service

准备 JDK 21 或更高版本，用 IntelliJ IDEA 打开项目根目录，并把 Project SDK 设为 JDK 21。

在根目录 `config/application.yml` 中填写模型 API Key。使用 OpenAI 兼容服务时，再按实际协议修改 `base-url`、Chat 模型和 Embedding 模型。真实 Key 只能留在本机，不能提交到 Git。

直接运行 `services/knowledge-service` 中的 `KnowledgeServiceApplication`。默认端口是 `8081`，本地固定身份和 classpath 知识已经配好，不需要 PostgreSQL、身份平台或消息中间件。

启动后先看健康状态：

```bash
curl http://localhost:8081/actuator/health
```

再调用真实模型：

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{"question":"退款通常多久到账？"}' \
  http://localhost:8081/api/v1/knowledge/answers
```

Windows PowerShell：

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/v1/knowledge/answers" `
  -ContentType "application/json" `
  -Body '{"question":"退款通常多久到账？"}'
```

## 启动完整咨询链路

需要体验会话、工单升级和受控 Agent 时，在 IDE 中按顺序运行：

1. `KnowledgeServiceApplication`，端口 `8081`；
2. `TicketAgentServiceApplication`，端口 `8082`；
3. `CustomerBffApplication`，端口 `8080`。

三个服务默认使用固定本地身份和 localhost HTTP 地址。只要模型配置有效，就不需要先准备 JWT、IdP、Redis、Kafka 或 Legacy Tool。

Customer Web 需要 Node.js 24。进入 `apps/customer-web` 后执行 `npm ci` 和 `npm run dev`，再打开 `http://127.0.0.1:5173`。

## 开启完整 RAG

完整 RAG 额外需要 PostgreSQL、`pgvector`、`pg_trgm` 和可返回 1536 维向量的 Embedding Provider。把 Knowledge Service 的 `java-ai.knowledge.mode` 改为 `postgres-rag`，填写数据库连接后，重新运行 `KnowledgeServiceApplication`。

上传、发布、索引和问答的连续步骤见[知识文档导入](knowledge-ingestion.md)。

## 常见问题

### IDE 找不到启动类

确认 Project SDK 是完整 JDK 21，并重新加载根目录项目。`java` 和 `javac` 必须来自同一套 JDK。

### 启动后提示 API Key 无效

确认根目录 `config/application.yml` 已替换占位 Key，并且 IDE 的 Working directory 是项目根目录。兼容 Provider 还要同时核对 `base-url` 和模型名。

### Knowledge Service 正常，完整链路仍失败

确认三个服务都已启动，并分别检查 8081、8082、8080 的健康接口。默认本地链路不需要 Token；若主动切换了 JWT 或 OAuth2 配置，则必须同时准备相应身份服务。
