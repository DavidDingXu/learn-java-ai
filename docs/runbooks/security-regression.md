# AI 安全回归

## 从安全数据集观察真实服务边界

先在目标隔离环境启动 `TicketAgentServiceApplication`，再在 IDE 中运行 `EvalRunner.main()`。Working directory 设为项目根目录，三枚最小权限短时令牌分别保存到不会提交的受限文件。

Program arguments 填写：

```text
security-eval --dataset datasets/security/agent-security-v1.jsonl --base-url https://ticket-agent-test.example.com --create-token-file target/eval-secrets/create-token --run-token-file target/eval-secrets/run-token --read-token-file target/eval-secrets/read-token --report var/learning-stage-reports/security-eval
```

Runner 不调用确认接口，评测身份也不能拥有正式写权限。三枚令牌分别只允许创建任务、运行任务和读取结果；服务端状态机与禁止事件断言继续阻止确认前执行。macOS 和 Windows 使用同一个 Java 入口与参数。

## 适用范围

初始数据集验证回归机制，不代表安全覆盖已经完整。公司项目还要加入脱敏的 Prompt Injection、Tool 输出注入、PII 类型、跨租户与部门 ACL、角色提升、超大参数和远程 Tool 失败案例。
