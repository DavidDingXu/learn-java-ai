# LangChain4j AI Services 适配验证

Status: VERIFIED_ISOLATED_LAB


## 已验证

- LangChain4j AI Services 实现稳定的 `PolicyAnswerPort`，不暴露框架类型。
- 确定性模型用于验证 Prompt 渲染、适配器映射和业务响应校验。
- 切换路由或移除该实现时，调用方不需要修改。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## 外部验证边界

这个实验只验证适配器接口契约，不代表已验证真实 Provider 效果或生产并发能力。
