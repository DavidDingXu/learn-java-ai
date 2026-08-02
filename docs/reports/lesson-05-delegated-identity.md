# 身份与访问范围验证

## 状态

`LOCAL_SECURITY_CONTRACT_VERIFIED`

## 证据边界

- Knowledge Service 通过 `KnowledgeAccessScopeProvider` 向应用层提供受信访问范围，Controller 不直接依赖 JWT Claim。
- 默认固定身份返回 `tenant-a / local-user / support`，不读取请求头或请求体，也不要求 Token。
- 生产 JWT 适配器在请求进入业务代码前检查签名、issuer、audience、有效期、租户和委托操作者；缺少 `knowledge:answer` 权限返回 HTTP 403。
- 调用方传入的身份 Header 不会成为授权依据，回答请求体只保留问题和会话上下文。

## 可执行证据

```bash
./mvnw -pl services/knowledge-service \
  -Dtest=KnowledgeFixedSecurityTest,FixedKnowledgeAccessScopeProviderTest,KnowledgeJwtSecurityTest \
  test
```

生产 JWT 的负向案例保存在 `datasets/security/jwt-boundary-cases-v1.jsonl`，由服务安全测试执行。固定本地身份另有 HTTP 与 Provider 单测，确认伪造 Header 不能改变访问范围。

## 不包含的结论

固定身份不验证真实用户，不能用于正式部署。公司身份平台的连通、密钥轮换、客户端认证、撤权和动态授权策略，仍要在目标环境验证。
