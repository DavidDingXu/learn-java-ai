import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

function implementationRevision(markdown) {
  const match = markdown.match(
    /(?:Implementation commit|Commit): `([0-9a-f]{40}|column-v\d+\.\d+\.\d+)`/,
  );
  assert.notEqual(match, null, "report must bind a full commit SHA or a column release tag");
  return match[1];
}

test("lessons 07 through 11 are backed by the real model engineering code", () => {
  const adapter = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/infrastructure/SpringAiKnowledgeAnswerModel.java");
  const prompt = read("services/knowledge-service/src/main/resources/prompts/knowledge-answer/v1/system.txt");
  const controller = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/web/KnowledgeAnswerController.java");
  const configuration = read("services/knowledge-service/src/main/resources/application.yml");

  assert.match(adapter, /BeanOutputConverter/);
  assert.match(adapter, /UNTRUSTED_USER_INPUT/);
  assert.match(adapter, /AUTHORIZED_KNOWLEDGE_CONTEXT/);
  assert.match(prompt, /未实际发生的业务动作/);
  assert.match(controller, /TEXT_EVENT_STREAM/);
  assert.match(configuration, /knowledgeAnswer:/);
  assert.match(configuration, /context-propagation: auto/);
});

test("contract and live eval reports use the same dataset without conflating modes", () => {
  const evidenceSummary = read("docs/reports/lesson-12-eval-observation.md");
  const contractMarkdown = read("docs/reports/lesson-12-contract-eval.md");
  const liveMarkdown = read("docs/reports/lesson-12-live-model-eval.md");
  const contract = JSON.parse(read("docs/reports/lesson-12-contract-eval.json"));
  const live = JSON.parse(read("docs/reports/lesson-12-live-model-eval.json"));

  assert.equal(contract.datasetVersion, "golden-set-v2");
  assert.equal(live.datasetVersion, "golden-set-v2");
  assert.equal(contract.mode, "CONTRACT_FIXTURE");
  assert.equal(live.mode, "LIVE_MODEL");
  assert.equal(contract.passed, 5);
  assert.equal(contract.failed, 0);
  assert.equal(live.passed, 5);
  assert.equal(live.failed, 0);
  assert.equal(contract.commit, implementationRevision(contractMarkdown));
  assert.equal(live.commit, implementationRevision(liveMarkdown));
  if (contract.commit !== live.commit) {
    assert.match(
      evidenceSummary,
      /代码、数据集、Prompt、模型与环境必须保持一致/,
    );
  }
  assert.equal(
    evidenceSummary.match(/`[0-9a-f]{40}`/g)?.length ?? 0,
    0,
    "reader-facing evidence summary must not expose commit reconciliation details",
  );
  assert.doesNotMatch(evidenceSummary, /当前契约报告|历史真实模型报告|当前代码的真实模型结论/);
  assert.ok(live.results.every((result) => /^[0-9a-f]{32}$/.test(result.traceId)));
});

test("live evidence uses one implementation and milestone 12 explains its boundary", () => {
  const smoke = read("docs/reports/lesson-04-live-model-smoke.md");
  const milestone = read("docs/reports/milestone-12.md");
  const live = read("docs/reports/lesson-12-live-model-eval.md");
  const reports = `${smoke}\n${live}\n${milestone}`;

  assert.match(smoke, /Status: LIVE_MODEL/);
  assert.match(milestone, /VERIFIED_LIVE_MODEL/);
  assert.equal(implementationRevision(smoke), implementationRevision(live));
  assert.match(milestone, /接口契约模式与真实模型模式分别运行/);
  assert.doesNotMatch(milestone, /Tag Rule|必须指向|must point/i);
  assert.doesNotMatch(reports, /sk-[A-Za-z0-9]{12,}|memberai\.tech/);
});

test("lesson 10 separates a model smoke call from the target SSE chain", () => {
  const report = read("docs/reports/lesson-10-sse-streaming.md");
  assert.match(report, /VERIFIED_PROTOCOL_AND_SERVICE/);
  assert.match(report, /普通 `LIVE_MODEL` 调用不能代替目标 SSE 链路/);
});
