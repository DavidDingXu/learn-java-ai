# 模型、检索与 Agent 评测

## 本地先跑无外部依赖的契约评测

契约模式会在进程内启动确定性 HTTP Fixture，不访问真实模型、数据库或身份平台。先构建可执行 Eval Runner：

```bash
./mvnw -pl quality/eval-runner -am package -DskipTests
```

再直接运行 Java：

```bash
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar \
  contract-eval \
  --dataset datasets/model-interaction/golden-set-v2.jsonl \
  --prompt-version knowledge-answer-v1 \
  --environment-id local-contract-fixture \
  --report docs/reports/local-contract-eval \
  --commit working-tree
```

Windows PowerShell 使用相同的 Maven 和 `java -jar` 参数，将换行符改为反引号。这条路径适合日常回归，但只检查数据集、HTTP 客户端、字段映射、阈值和报告逻辑。

## 真实模型评测走公开 HTTP 边界

真实模型评测应指向已启用 JWT 和模型 Provider 的 Knowledge Service。模型、数据库和身份参数由目标环境 `application.yml` 与部署密钥系统提供，读者无需为本地契约评测配置这些值。

准备一枚包含正确 issuer、audience、actor、tenant、subject 和 `knowledge:answer` scope 的短时令牌，把它写入权限受限且不会提交的 `target/eval-secrets/model-token` 文件，然后运行：

```bash
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar \
  model-eval \
  --dataset datasets/model-interaction/golden-set-v2.jsonl \
  --base-url https://knowledge-test.example.com \
  --mode LIVE_MODEL \
  --bearer-token-file target/eval-secrets/model-token \
  --prompt-version knowledge-answer-v1 \
  --environment-id knowledge-test \
  --report docs/reports/model-live-eval \
  --commit '<CANDIDATE_COMMIT>'
```

候选 Commit、数据集、Prompt、模型和服务环境必须保持一致。报告会保存模式、数据集版本、模型、代码版本、结果计数、Token、延迟、Trace ID 和失败样例，不得保存 API Key、Bearer Token 或私有 Provider 地址。

## 检索评测使用已准备的知识环境

目标 Knowledge Service 需要预先导入 `datasets/retrieval/golden-set-v1.jsonl` 引用的版本化文档、ACL 和 Embedding。短时令牌必须包含 `knowledge:eval` scope，以及目标环境接受的 tenant、subject、department、issuer、audience 和 actor。

```bash
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar \
  retrieval-eval \
  --dataset datasets/retrieval/golden-set-v1.jsonl \
  --base-url https://knowledge-test.example.com \
  --bearer-token-file target/eval-secrets/retrieval-token \
  --top-k 5 \
  --min-recall 0.80 \
  --min-hit-rate 0.90 \
  --min-mrr 0.60 \
  --max-duplicate-rate 0.02 \
  --max-p95-ms 1500 \
  --report docs/reports/retrieval-eval \
  --commit '<CANDIDATE_COMMIT>'
```

Runner 记录实际排名、Embedding 模型、Recall@K、HitRate@K、MRR、重复率和 p95 延迟。任一阈值失败，或一轮结果出现多个 Embedding 模型，进程都会非零退出。

## Agent 评测使用三枚最小权限令牌

Agent 评测指向已部署的 Ticket Agent Service，不使用一枚全权测试凭证：

- 创建令牌：actor 为 `customer-bff`，scope 为 `ticket:task:create`；
- 运行令牌：actor 为 `ticket-agent-worker`，scope 为 `ticket:task:run`；
- 读取令牌：actor 为 `jdk8-crm`，scope 为 `ticket:task:read`。

分别把三枚短时令牌写入 `target/eval-secrets/create-token`、`run-token` 和 `read-token`，再运行：

```bash
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar \
  agent-eval \
  --dataset datasets/agent/golden-set-v2.jsonl \
  --base-url https://ticket-agent-test.example.com \
  --create-token-file target/eval-secrets/create-token \
  --run-token-file target/eval-secrets/run-token \
  --read-token-file target/eval-secrets/read-token \
  --report docs/reports/agent-eval \
  --commit '<CANDIDATE_COMMIT>'
```

Runner 会创建任务、运行到终态或等待确认状态，再读取审计时间线。它不调用确认接口，因此数据集执行期间出现任何写 Tool 成功审计，都表示副作用边界失守。

## 凭证和报告边界

令牌文件只适合隔离的短时评测会话，使用后应立即删除。共享流水线中应由凭证系统在运行时创建受限临时文件，并禁止命令回显和日志记录。若令牌曾进入命令历史或 CI 日志，立即撤销，不要等待自然过期。
