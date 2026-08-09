package com.xiaoding.javaai.stages.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.stages.support.StageConfig;
import com.xiaoding.javaai.stages.support.StageHttp;
import com.xiaoding.javaai.stages.support.StageOutput;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnterpriseRagStageApplication {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final StageConfig config = StageConfig.load();
    private final StageHttp http = new StageHttp();
    private final String baseUrl = config.required("stages.knowledge-base-url");
    private final RagStageProgressStore progressStore = new RagStageProgressStore(
            Path.of(config.required("stages.rag.state-file"))
    );

    public static void main(String[] args) {
        new EnterpriseRagStageApplication().run(RagStageSelection.parse(args));
    }

    private void run(RagStageSelection selection) {
        StageOutput.heading("企业 RAG 分篇实验");
        StageOutput.value("Knowledge Service", StageOutput.text(http.get(baseUrl + "/actuator/health"), "status"));
        for (int lesson : selection.lessons()) runLesson(lesson);
    }

    private void runLesson(int lesson) {
        switch (lesson) {
            case 13 -> showDocumentDomain();
            case 14 -> uploadDocuments();
            case 15 -> {
                publishDocuments();
                indexDocuments();
            }
            case 16 -> showVectorRetrieval();
            case 17 -> checkAcl();
            case 18 -> compareRetrievalModes();
            case 19 -> showGroundedAnswer();
            case 20 -> checkIncrementalWorker();
            case 21 -> writeEvaluationReport();
            default -> throw new IllegalArgumentException("Unsupported RAG lesson: " + lesson);
        }
    }

    private void showDocumentDomain() {
        StageOutput.heading("13 文档领域对象");
        StageOutput.value("可读文档 ID", config.required("stages.rag.allowed-document-id"));
        StageOutput.value("ACL 负例文档 ID", config.required("stages.rag.blocked-document-id"));
        StageOutput.value("生命周期", "DRAFT -> PUBLISHED -> INDEXED");
        StageOutput.value("发布时 ACL", "support / finance");
    }

    private void uploadDocuments() {
        StageOutput.heading("14 文档上传");
        RagStageProgress progress = progressStore.load();
        if (progress.allowedDocumentId() == null) {
            Uploaded allowed = upload(
                    config.required("stages.rag.allowed-document-id"),
                    "售后政策",
                    Path.of(config.required("stages.rag.source-file"))
            );
            progress = progress.withAllowedUpload(allowed.documentId(), allowed.version(), allowed.revision());
            progressStore.save(progress);
        }
        if (progress.blockedDocumentId() == null) {
            Uploaded blocked = upload(
                    config.required("stages.rag.blocked-document-id"),
                    "财务内部政策",
                    Path.of(config.required("stages.rag.blocked-source-file"))
            );
            progress = progress.withBlockedUpload(blocked.documentId(), blocked.version(), blocked.revision());
            progressStore.save(progress);
        }
        StageOutput.value("可读文档", progress.allowedDocumentId() + " v" + progress.allowedVersion());
        StageOutput.value("ACL 负例文档", progress.blockedDocumentId() + " v" + progress.blockedVersion());
    }

    private void publishDocuments() {
        StageOutput.heading("15 发布与索引任务");
        RagStageProgress progress = requireUploaded(15);
        if (progress.allowedIndexTaskId() == null) {
            String taskId = publish(
                    progress.allowedDocumentId(), progress.allowedVersion(), progress.allowedRevision(), "support"
            );
            progress = progress.withAllowedIndexTask(taskId);
            progressStore.save(progress);
        }
        if (progress.blockedIndexTaskId() == null) {
            String taskId = publish(
                    progress.blockedDocumentId(), progress.blockedVersion(), progress.blockedRevision(), "finance"
            );
            progress = progress.withBlockedIndexTask(taskId);
            progressStore.save(progress);
        }
        StageOutput.value("可读文档索引任务", progress.allowedIndexTaskId());
        StageOutput.value("ACL 负例索引任务", progress.blockedIndexTaskId());
    }

    private Uploaded upload(String documentId, String title, Path source) {
        JsonNode uploaded = http.upload(
                baseUrl + "/api/v1/knowledge/documents/" + documentId + "/versions",
                "{\"title\":\"" + title + "\",\"expectedRevision\":0}",
                source,
                "text/markdown"
        );
        return new Uploaded(
                documentId,
                uploaded.path("versionNumber").asInt(),
                uploaded.path("revision").asLong()
        );
    }

    private String publish(String documentId, int version, long revision, String department) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("expectedRevision", revision);
        body.put("effectiveFrom", Instant.parse("2026-01-01T00:00:00Z").toString());
        body.put("effectiveUntil", null);
        body.put("acl", List.of(Map.of("subjectType", "DEPARTMENT", "subjectId", department)));
        return http.post(
                baseUrl + "/api/v1/knowledge/documents/" + documentId + "/versions/" + version + "/publish",
                body
        ).path("indexTaskId").asText();
    }

    private void runIndexWorker() {
        JsonNode result = http.postWithoutBody(baseUrl + "/internal/v1/knowledge/index-tasks/run-once");
        StageOutput.value("索引 Worker", result.isTextual() ? result.asText() : result);
    }

    private void indexDocuments() {
        StageOutput.heading("15 文档切分与向量索引");
        RagStageProgress progress = requirePublished(15);
        if (!progress.indexed()) {
            runIndexWorker();
            runIndexWorker();
            progressStore.save(progress.markIndexed());
        } else {
            StageOutput.value("索引状态", "已完成，复用现有 Chunk");
        }
    }

    private void showVectorRetrieval() {
        requireIndexed(16);
        JsonNode vector = retrieve(config.required("stages.question"), "VECTOR", 6);
        StageOutput.heading("16 pgvector TopK");
        StageOutput.value("Embedding", StageOutput.text(vector, "embeddingModel"));
        StageOutput.value("向量 TopK", vector.path("chunkIds"));
    }

    private void checkAcl() {
        RagStageProgress progress = requireIndexed(17);
        boolean leaked = aclLeaked(progress.blockedDocumentId());
        StageOutput.heading("17 ACL 负例");
        StageOutput.value("财务文档进入 TopK", leaked);
        if (leaked) {
            throw new IllegalStateException("ACL failure: blocked document entered the support user's TopK");
        }
    }

    private void compareRetrievalModes() {
        requireIndexed(18);
        String question = config.required("stages.question");
        JsonNode vector = retrieve(question, "VECTOR", 6);
        JsonNode hybrid = retrieve(question, "HYBRID", 6);
        StageOutput.heading("18 向量与混合检索对比");
        StageOutput.value("向量 TopK", vector.path("chunkIds"));
        StageOutput.value("混合 TopK", hybrid.path("chunkIds"));
    }

    private void showGroundedAnswer() {
        requireIndexed(19);
        JsonNode answer = answer();
        StageOutput.heading("19 带引用的回答");
        StageOutput.value("回答", StageOutput.text(answer, "answer"));
        StageOutput.value("引用数", answer.path("citations").size());
    }

    private void checkIncrementalWorker() {
        requireIndexed(20);
        StageOutput.heading("20 增量索引幂等结果");
        runIndexWorker();
    }

    private void writeEvaluationReport() {
        RagStageProgress progress = requireIndexed(21);
        boolean leaked = aclLeaked(progress.blockedDocumentId());
        if (leaked) {
            throw new IllegalStateException("ACL failure: blocked document entered the support user's TopK");
        }
        JsonNode answer = answer();
        Evaluation vectorEvaluation = evaluate("VECTOR");
        Evaluation hybridEvaluation = evaluate("HYBRID");
        Path report = writeReport(vectorEvaluation, hybridEvaluation, false, answer);
        StageOutput.heading("21 RAG 评测报告");
        StageOutput.value("Embedding", vectorEvaluation.embeddingModel());
        StageOutput.value("语义质量证据", vectorEvaluation.semanticQualityEvidence());
        StageOutput.value("向量 Recall", format(vectorEvaluation.recall()));
        StageOutput.value("混合 Recall", format(hybridEvaluation.recall()));
        StageOutput.value("报告", report.toAbsolutePath().normalize());
    }

    private boolean aclLeaked(String blockedDocumentId) {
        JsonNode blockedResult = retrieve("quartz-lotus-927 是什么？", "HYBRID", 10);
        return contains(blockedResult.path("documentIds"), blockedDocumentId);
    }

    private JsonNode answer() {
        return http.post(
                baseUrl + "/api/v1/knowledge/answers",
                Map.of("question", config.required("stages.question"))
        );
    }

    private RagStageProgress requireUploaded(int lesson) {
        RagStageProgress progress = progressStore.load();
        if (!progress.uploaded()) {
            throw new IllegalStateException("第 " + lesson + " 篇需要先运行 Program arguments 13");
        }
        return progress;
    }

    private RagStageProgress requirePublished(int lesson) {
        RagStageProgress progress = requireUploaded(lesson);
        if (!progress.published()) {
            throw new IllegalStateException("第 " + lesson + " 篇需要先运行 Program arguments 14");
        }
        return progress;
    }

    private RagStageProgress requireIndexed(int lesson) {
        RagStageProgress progress = requirePublished(lesson);
        if (!progress.indexed()) {
            throw new IllegalStateException("第 " + lesson + " 篇需要先运行 Program arguments 15");
        }
        return progress;
    }

    private JsonNode retrieve(String question, String mode, int topK) {
        return http.post(
                baseUrl + "/internal/v1/knowledge/retrieval/evaluations",
                Map.of("question", question, "topK", topK, "mode", mode)
        );
    }

    private Evaluation evaluate(String mode) {
        Path dataset = Path.of(config.required("stages.rag.golden-set"));
        List<CaseResult> results = new ArrayList<>();
        String embeddingModel = null;
        try {
            for (String line : Files.readAllLines(dataset)) {
                if (line.isBlank()) continue;
                JsonNode evalCase = JSON.readTree(line);
                Set<String> expected = values(evalCase.path("expectedChunkIds"));
                JsonNode retrieval = retrieve(evalCase.path("question").asText(), mode, 6);
                String caseEmbeddingModel = StageOutput.text(retrieval, "embeddingModel");
                if (embeddingModel == null) embeddingModel = caseEmbeddingModel;
                if (!embeddingModel.equals(caseEmbeddingModel)) {
                    throw new IllegalStateException("one evaluation run used multiple embedding models");
                }
                Set<String> actual = values(retrieval.path("chunkIds"));
                int hits = intersection(expected, actual);
                int firstRank = firstRank(expected, actual);
                results.add(new CaseResult(
                        evalCase.path("id").asText(),
                        expected.isEmpty() ? 1.0 : (double) hits / expected.size(),
                        hits > 0 ? 1.0 : 0.0,
                        firstRank == 0 ? 0.0 : 1.0 / firstRank
                ));
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Golden Set " + dataset, error);
        }
        return new Evaluation(
                mode,
                average(results, CaseResult::recall),
                average(results, CaseResult::hitRate),
                average(results, CaseResult::mrr),
                results.size(),
                embeddingModel == null ? "unknown" : embeddingModel
        );
    }

    private Path writeReport(
            Evaluation vector,
            Evaluation hybrid,
            boolean leaked,
            JsonNode answer
    ) {
        Path directory = Path.of(config.required("stages.rag.report-directory"));
        Path report = directory.resolve("rag-learning-journey.md");
        String markdown = "# RAG Learning Journey\n\n"
                + "| 检查 | 结果 |\n|---|---|\n"
                + "| Embedding model | " + vector.embeddingModel() + " |\n"
                + "| 可作为语义质量证据 | " + vector.semanticQualityEvidence() + " |\n"
                + "| VECTOR Recall@6 | " + format(vector.recall()) + " |\n"
                + "| VECTOR HitRate@6 | " + format(vector.hitRate()) + " |\n"
                + "| VECTOR MRR | " + format(vector.mrr()) + " |\n"
                + "| HYBRID Recall@6 | " + format(hybrid.recall()) + " |\n"
                + "| HYBRID HitRate@6 | " + format(hybrid.hitRate()) + " |\n"
                + "| HYBRID MRR | " + format(hybrid.mrr()) + " |\n"
                + "| ACL 越权文档进入 TopK | " + leaked + " |\n"
                + "| 回答引用数 | " + answer.path("citations").size() + " |\n\n"
                + "Golden Set cases: " + vector.caseCount() + "\n";
        try {
            Files.createDirectories(directory);
            Files.writeString(report, markdown);
            return report;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot write RAG report " + report, error);
        }
    }

    private static Set<String> values(JsonNode array) {
        Set<String> values = new LinkedHashSet<>();
        array.forEach(item -> values.add(item.asText()));
        return values;
    }

    private static int intersection(Set<String> left, Set<String> right) {
        int count = 0;
        for (String value : left) if (right.contains(value)) count++;
        return count;
    }

    private static int firstRank(Set<String> expected, Set<String> actual) {
        int rank = 1;
        for (String value : actual) {
            if (expected.contains(value)) return rank;
            rank++;
        }
        return 0;
    }

    private static boolean contains(JsonNode array, String value) {
        for (JsonNode item : array) if (value.equals(item.asText())) return true;
        return false;
    }

    private static double average(List<CaseResult> values, Metric metric) {
        return values.stream().mapToDouble(metric::value).average().orElse(0.0);
    }

    private static String format(double value) {
        return "%.4f".formatted(value);
    }

    private record Uploaded(String documentId, int version, long revision) {
    }

    private record CaseResult(String id, double recall, double hitRate, double mrr) {
    }

    private record Evaluation(
            String mode,
            double recall,
            double hitRate,
            double mrr,
            int caseCount,
            String embeddingModel
    ) {
        boolean semanticQualityEvidence() {
            return !embeddingModel.startsWith("deterministic-hash-");
        }
    }

    @FunctionalInterface
    private interface Metric {
        double value(CaseResult result);
    }
}
