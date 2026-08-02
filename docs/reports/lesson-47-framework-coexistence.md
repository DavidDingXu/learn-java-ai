# 双框架共存验证

Status: VERIFIED_CAPABILITY_ROUTING


## 已验证

- `FrameworkCoexistencePolicy` routes fixed business capabilities to Spring AI or LangChain4j.
- 未注册能力默认拒绝执行。
- 主工程 Enforcer 拒绝 LangChain4j 和 AgentScope 依赖；隔离实验工程维护自己的 BOM，也不会在同一个 Boot 进程中加载两套自动配置。

## 验证命令

```bash
./mvnw -f labs/pom.xml verify
```

## 外部验证边界

`labs` POM 尚未禁止数据库或消息依赖。当前也没有在同一个 Boot 进程中加载两套自动配置；若采用这种方案，需要增加独立的兼容性测试。
