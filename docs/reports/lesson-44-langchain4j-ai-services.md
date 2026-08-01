# LangChain4j AI Services 适配验证

Status: VERIFIED_ISOLATED_LAB


## 已验证

- LangChain4j AI Services implements a stable `PolicyAnswerPort` without exposing framework types.
- A deterministic model proves prompt rendering, adapter mapping and business response validation.
- The use case can be routed or removed without changing callers.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl langchain4j-lab test
```

## 外部验证边界

The lab proves the adapter contract, not live provider quality or production concurrency.
