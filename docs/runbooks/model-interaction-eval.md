# 模型、检索与 Agent 评测

## 从 EvalRunner main 方法进入

在 IDE 中打开 `quality/eval-runner` 的 `EvalRunner`，Working directory 设为项目根目录。后续每种评测只更换 Program arguments，直接运行这个 `main()` 即可。

契约模式会在进程内启动确定性 HTTP Fixture，不访问真实模型、数据库或身份平台。Program arguments 填写：

```text
contract-eval --dataset datasets/model-interaction/golden-set-v2.jsonl --prompt-version knowledge-answer-v1 --environment-id local-contract-fixture --report var/learning-stage-reports/local-contract-eval
```

macOS 和 Windows 使用同一个 Java 入口。这条路径只检查数据集、HTTP 客户端、字段映射、阈值和报告逻辑。

## 真实模型评测走公开 HTTP 边界

真实模型评测应指向已经配置模型 Provider 的 Knowledge Service。本地默认使用固定身份，不需要 Token；目标环境的模型、数据库和身份参数由部署配置提供。

启动 `KnowledgeServiceApplication` 后，将 Program arguments 改为：

```text
model-eval --dataset datasets/model-interaction/golden-set-v2.jsonl --base-url http://localhost:8081 --mode LIVE_MODEL --prompt-version knowledge-answer-v1 --environment-id knowledge-test --report var/learning-stage-reports/model-live-eval
```

候选 Commit、数据集、Prompt、模型和服务环境必须保持一致。报告会保存模式、数据集版本、模型、代码版本、结果计数、Token、延迟、Trace ID 和失败样例，不得保存 API Key、Bearer Token 或私有 Provider 地址。

## 检索评测使用已准备的知识环境

Knowledge Service 需要预先导入 `datasets/retrieval/golden-set-v1.jsonl` 引用的版本化文档、ACL 和 Embedding。本地固定身份是 `tenant-a / local-user / support`，数据集文档必须对这组身份可见。

```text
retrieval-eval --dataset datasets/retrieval/golden-set-v1.jsonl --base-url http://localhost:8081 --top-k 5 --min-recall 0.80 --min-hit-rate 0.90 --min-mrr 0.60 --max-duplicate-rate 0.02 --max-p95-ms 1500 --report var/learning-stage-reports/retrieval-eval
```

Runner 记录实际排名、Embedding 模型、Recall@K、HitRate@K、MRR、重复率和 p95 延迟。任一阈值失败，或一轮结果出现多个 Embedding 模型，进程都会非零退出。

## Agent 评测先走本地固定身份

先启动本地 `TicketAgentServiceApplication`。默认 `java-ai.security.mode=fixed`，所以 Runner 不需要令牌：

```text
agent-eval --dataset datasets/agent/golden-set-v2.jsonl --base-url http://localhost:8082 --report var/learning-stage-reports/agent-eval
```

Runner 会创建任务、运行到终态或等待确认状态，再读取审计时间线。它不调用确认接口，因此数据集执行期间出现任何写 Tool 成功审计，都表示副作用边界失守。

## JWT 环境使用三枚最小权限令牌

连接已启用 JWT 的 Ticket Agent Service 时，不使用一枚全权测试凭证：

- 创建令牌：actor 为 `customer-bff`，scope 为 `ticket:task:create`；
- 运行令牌：actor 为 `ticket-agent-worker`，scope 为 `ticket:task:run`；
- 读取令牌：actor 为 `jdk8-crm`，scope 为 `ticket:task:read`。

分别把三枚短时令牌写入 `target/eval-secrets/create-token`、`run-token` 和 `read-token`，再运行：

```text
agent-eval --dataset datasets/agent/golden-set-v2.jsonl --base-url https://ticket-agent-test.example.com --create-token-file target/eval-secrets/create-token --run-token-file target/eval-secrets/run-token --read-token-file target/eval-secrets/read-token --report var/learning-stage-reports/agent-eval
```

三个令牌参数要么一起提供，要么都不提供。这样本地路径保持简单，受保护环境仍能发现 audience、scope 或 actor 配置错误。

## 连接受保护环境时再提供凭证

连接公司测试环境时，`model-eval` 和 `retrieval-eval` 可以增加 `--bearer-token-file <path>`。令牌应由目标身份系统签发，包含该环境要求的受众、主体、租户和权限。Agent 评测仍需要三枚用途分离的凭证。

令牌文件只适合隔离的短时评测会话，使用后应立即删除。共享流水线中应由凭证系统在运行时创建受限临时文件，并禁止命令回显和日志记录。若令牌曾进入命令历史或 CI 日志，立即撤销，不要等待自然过期。
