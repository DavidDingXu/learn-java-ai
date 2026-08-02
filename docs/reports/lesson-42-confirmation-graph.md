# 人工确认 Graph 路由验证

Status: VERIFIED_ISOLATED_GRAPH


## 已验证

- 真实 Spring AI Alibaba `StateGraph` 定义准备、等待确认和执行节点。
- 业务确认状态保持显式，不隐藏在模型消息中。
- 测试覆盖确认路由和状态输出。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

这个图实验只对比隔离的流程实现。持久化 checkpoint、并发确认和生产恢复仍需要目标环境集成测试。
