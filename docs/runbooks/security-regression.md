# AI 安全回归

## 本地先验证确定性安全边界

本地测试不需要服务地址、Token 或环境变量，直接验证 JWT、ACL、Tool Catalog、Prompt 边界和 Agent 评测规则：

```bash
./mvnw \
  -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner \
  -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Windows PowerShell 把 `./mvnw` 换成 `.\mvnw.cmd`，其余 Maven 参数不变。

## 在共享测试环境运行安全数据集

共享环境使用专用测试租户和三枚最小权限短时令牌。分别把令牌写入不会提交的受限文件，再直接运行 Eval Runner：

```bash
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar \
  security-eval \
  --dataset datasets/security/agent-security-v1.jsonl \
  --base-url https://ticket-agent-test.example.com \
  --create-token-file target/eval-secrets/create-token \
  --run-token-file target/eval-secrets/run-token \
  --read-token-file target/eval-secrets/read-token \
  --report target/reports/security-eval \
  --commit replace-with-tested-commit-sha
```

Runner 不调用确认接口，测试身份也不能拥有生产写权限。三枚令牌分别只允许创建任务、运行任务和读取结果；服务端状态机与禁止事件断言继续阻止确认前执行。

## 适用范围

初始数据集验证回归机制，不代表安全覆盖已经完整。公司项目还要加入脱敏的 Prompt Injection、Tool 输出注入、PII 类型、跨租户与部门 ACL、角色提升、超大参数和远程 Tool 失败案例。
