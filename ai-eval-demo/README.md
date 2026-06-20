# ai-eval-demo

AI 功能 Golden Set 评测 demo。

本模块先不用 LLM-as-Judge，演示最小可解释评测：

- 普通 Golden Set：样本包含问题、期望关键依据、实际回答，`EvalRunner` 计算每条样本是否通过和整体通过率。
- RAG Eval：样本包含期望 chunk、实际召回 chunk、实际引用 chunk、是否拒答，`RagEvalRunner` 分别计算召回、引用和无依据拒答。
- Agent Eval：样本包含期望 Tool 调用路径、是否需要人工确认，`AgentEvalRunner` 评估工具路径、人工确认和风险等级。
- Judge Calibration：样本包含人工标签、模型评委标签和置信度，`JudgeCalibrationRunner` 评估评委结果是否可以信任。
- Prompt Regression：样本包含 baseline 和候选 Prompt 的通过率，`PromptRegressionRunner` 判断候选版本是否低于可接受阈值。
- Harness Experiment：样本包含 baseline 和 candidate 的质量、成本、延迟观测值，`HarnessExperimentRunner` 判断候选策略是否可以发布。

## 当前边界

当前 demo 是规则版评测，不接真实模型评委，也不读取线上日志平台。

它的目标是先把 Java 后端的评测合同写清楚：

```text
样本怎么定义
观察结果怎么传入
通过率怎么计算
哪些结果必须人工复核
Prompt 候选版本什么时候不能发布
候选策略什么时候不能因为质量提升而忽略成本和延迟回退
```

生产项目里可以把输入换成线上 trace、RAG 检索日志、Agent 执行步骤和人工标注结果，但不应该改变这些核心对象的含义。

## 启动

```bash
mvn -pl ai-eval-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8090/
```

页面会运行基础回答、RAG 引用和 Agent 路径评测，并展示通过率。

## 验证

```bash
curl -X POST http://localhost:8090/api/eval/run \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "caseId": "case-1",
      "question": "退款怎么处理",
      "expectedKeyword": "核对物流状态",
      "actualAnswer": "需要先核对物流状态，再决定是否转人工。"
    },
    {
      "caseId": "case-2",
      "question": "能直接关闭工单吗",
      "expectedKeyword": "人工确认",
      "actualAnswer": "关闭工单需要人工确认。"
    }
  ]'
```

RAG Eval 示例：

```bash
curl -X POST http://localhost:8090/api/eval/rag/run \
  -H 'Content-Type: application/json' \
  -d '{
    "cases": [
      {
        "caseId": "rag-1",
        "question": "发货后退款怎么处理",
        "expectedChunkIds": ["refund-policy-001-c1"],
        "expectNoEvidence": false
      }
    ],
    "observations": [
      {
        "caseId": "rag-1",
        "retrievedChunkIds": ["refund-policy-001-c1"],
        "citedChunkIds": ["refund-policy-001-c1"],
        "noEvidenceReturned": false,
        "answer": "根据制度，发货后退款需先核对物流状态。"
      }
    ]
  }'
```

Agent Eval 示例：

```bash
curl -X POST http://localhost:8090/api/eval/agent/run \
  -H 'Content-Type: application/json' \
  -d '{
    "cases": [
      {
        "caseId": "agent-1",
        "question": "客户申请退款但订单已发货怎么办",
        "expectedToolPath": ["ticket.lookup", "order.lookup", "policy.search", "advice.compose"],
        "expectHumanApproval": false
      }
    ],
    "observations": [
      {
        "caseId": "agent-1",
        "actualToolPath": ["ticket.lookup", "order.lookup", "policy.search", "advice.compose"],
        "humanApprovalRequested": false,
        "riskLevel": "MEDIUM"
      }
    ]
  }'
```

Judge Calibration 示例：

```bash
curl -X POST http://localhost:8090/api/eval/judge/calibrate \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "caseId": "judge-1",
      "question": "退款建议是否引用了正确制度",
      "humanPassed": true,
      "judgePassed": true,
      "judgeConfidence": 0.86,
      "note": "引用了 refund-policy-001-c1"
    },
    {
      "caseId": "judge-2",
      "question": "高风险退款是否需要人工确认",
      "humanPassed": false,
      "judgePassed": true,
      "judgeConfidence": 0.91,
      "note": "模型评委忽略了人工确认"
    }
  ]'
```

Prompt Regression 示例：

```bash
curl -X POST http://localhost:8090/api/eval/prompt/regression \
  -H 'Content-Type: application/json' \
  -d '[
    {
      "caseId": "prompt-1",
      "promptKey": "ticket-advice",
      "baselineVersion": "v1",
      "candidateVersion": "v2",
      "baselinePassRate": 0.82,
      "candidatePassRate": 0.85,
      "tolerance": 0.02
    },
    {
      "caseId": "prompt-2",
      "promptKey": "policy-answer",
      "baselineVersion": "v4",
      "candidateVersion": "v5",
      "baselinePassRate": 0.91,
      "candidatePassRate": 0.86,
      "tolerance": 0.03
    }
  ]'
```

Harness Experiment 示例：

```bash
curl -X POST http://localhost:8090/api/eval/harness/run \
  -H 'Content-Type: application/json' \
  -d '{
    "cases": [
      {
        "caseId": "harness-1",
        "scenario": "ticket-advice",
        "minQualityImprovement": 0.03,
        "maxCostIncreaseRate": 0.20,
        "maxLatencyIncreaseRate": 0.25
      }
    ],
    "observations": [
      {
        "caseId": "harness-1",
        "strategy": "baseline",
        "qualityScore": 0.82,
        "costUnits": 900,
        "latencyMillis": 1200
      },
      {
        "caseId": "harness-1",
        "strategy": "candidate",
        "qualityScore": 0.87,
        "costUnits": 980,
        "latencyMillis": 1300
      }
    ]
  }'
```

## 测试

```bash
mvn -pl ai-eval-demo test
```

正常情况下会看到 18 个测试通过，覆盖：

- 普通 Golden Set 关键字命中率。
- RAG 召回命中。
- RAG 引用命中。
- 无依据拒答样本。
- Agent 工具路径命中。
- Agent 缺少必需 Tool。
- Agent 人工确认预期。
- 模型评委与人工标签一致性。
- 模型评委低置信度样本。
- Prompt 候选版本低于 baseline 的回归判断。
- Harness 候选策略的质量、成本和延迟护栏。
