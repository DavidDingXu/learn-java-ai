package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.text.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.RerankRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class DashScopeRetrievalLabApplication {

    private DashScopeRetrievalLabApplication() {
    }

    public static void main(String[] args) {
        Properties config = SpringAiAlibabaLabApplication.loadConfig();
        DashScopeApi api = DashScopeApi.builder()
                .baseUrl(SpringAiAlibabaLabApplication.required(config, "lab.dashscope.base-url"))
                .apiKey(SpringAiAlibabaLabApplication.configuredSecret(config, "lab.dashscope.api-key"))
                .build();
        DashScopeEmbeddingModel embeddingModel = new DashScopeEmbeddingModel(
                api,
                MetadataMode.EMBED,
                DashScopeEmbeddingOptions.builder()
                        .model(SpringAiAlibabaLabApplication.required(
                                config, "lab.dashscope.embedding-model"))
                        .dimensions(Integer.parseInt(SpringAiAlibabaLabApplication.required(
                                config, "lab.dashscope.embedding-dimensions")))
                        .textType("document")
                        .build());
        DashScopeRerankModel rerankModel = new DashScopeRerankModel(
                api,
                DashScopeRerankOptions.builder()
                        .model(SpringAiAlibabaLabApplication.required(config, "lab.dashscope.rerank-model"))
                        .topN(Integer.parseInt(SpringAiAlibabaLabApplication.required(
                                config, "lab.dashscope.rerank-top-n")))
                        .returnDocuments(true)
                        .build());

        OnlineRetrievalReplacementExperiment experiment = new OnlineRetrievalReplacementExperiment(
                embeddingModel::embed,
                (query, candidates) -> rerankModel.call(new RerankRequest(
                                query,
                                candidates.stream()
                                        .map(candidate -> new Document(
                                                candidate.id(), candidate.text(),
                                                Map.of("candidateId", candidate.id())))
                                        .toList()))
                        .getResults().stream()
                        .map(result -> new ScoredRetrievalCandidate(
                                result.getOutput().getId(), result.getScore()))
                        .toList());
        OnlineRetrievalExperimentReport report = experiment.run(
                "refund-arrival",
                "退款审核通过后多久到账？",
                List.of(
                        new RetrievalCandidate(
                                "refund-policy",
                                "退款审核通过后原路退回，通常一到五个工作日到账。"),
                        new RetrievalCandidate(
                                "invoice-policy",
                                "电子发票可在订单完成后从开票入口申请。"),
                        new RetrievalCandidate(
                                "shipping-policy",
                                "物流签收异常可在四十八小时内提交核验。")),
                List.of("refund-policy"));

        System.out.printf(
                "embeddingRanking=%s rerankedRanking=%s recall=%.3f mrr=%.3f embeddingMs=%d rerankMs=%d%n",
                report.embeddingRanking(), report.rerankedRanking(), report.recallAtK(), report.mrr(),
                report.embeddingLatency().toMillis(), report.rerankLatency().toMillis());
    }
}
