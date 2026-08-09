package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeLexicalSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeQueryRewriter;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeReranker;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import com.xiaoding.javaai.knowledge.retrieval.application.port.RetrievalPlanProvider;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public final class HybridKnowledgeRetrievalService implements KnowledgeRetriever {

    private final KnowledgeQueryRewriter queryRewriter;
    private final KnowledgeEmbeddingModel embeddingModel;
    private final KnowledgeChunkSearchRepository vectorSearch;
    private final KnowledgeLexicalSearchRepository lexicalSearch;
    private final KnowledgeReranker reranker;
    private final RetrievalPlanProvider planProvider;
    private final ReciprocalRankFusion rankFusion;

    public HybridKnowledgeRetrievalService(
            KnowledgeQueryRewriter queryRewriter,
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkSearchRepository vectorSearch,
            KnowledgeLexicalSearchRepository lexicalSearch,
            KnowledgeReranker reranker,
            RetrievalPlanProvider planProvider,
            ReciprocalRankFusion rankFusion
    ) {
        this.queryRewriter = queryRewriter;
        this.embeddingModel = embeddingModel;
        this.vectorSearch = vectorSearch;
        this.lexicalSearch = lexicalSearch;
        this.reranker = reranker;
        this.planProvider = planProvider;
        this.rankFusion = rankFusion;
    }

    @Override
    public KnowledgeRetrievalResult retrieve(RetrieveKnowledgeQuery query) {
        return retrieve(query, planProvider.planFor(query));
    }

    public KnowledgeRetrievalResult retrieve(RetrieveKnowledgeQuery query, RetrievalPlan plan) {
        if (plan.candidateK() < query.topK()) {
            throw new IllegalArgumentException("candidateK must be greater than or equal to topK");
        }

        String retrievalQuery = plan.rewriteQuery()
                ? requireText(queryRewriter.rewrite(query.question()), "rewritten query")
                : query.question();
        KnowledgeEmbedding embedding = embeddingModel.embed(retrievalQuery);
        List<RetrievedKnowledgeChunk> vectorCandidates = vectorSearch.search(
                embedding.vector(), embedding.model(), query.accessScope(), query.effectiveAt(), plan.candidateK()
        );

        List<RetrievedKnowledgeChunk> candidates = vectorCandidates;
        if (plan.lexicalSearch()) {
            List<RetrievedKnowledgeChunk> lexicalCandidates = lexicalSearch.search(
                    retrievalQuery, query.accessScope(), query.effectiveAt(), plan.candidateK()
            );
            candidates = rankFusion.fuse(vectorCandidates, lexicalCandidates, plan.candidateK());
        }

        List<RetrievedKnowledgeChunk> result = plan.rerank()
                ? restoreRerankedCandidates(
                        candidates,
                        reranker.rerank(query.question(), candidates, query.topK()),
                        query.topK()
                )
                : candidates.stream().limit(query.topK()).toList();
        return new KnowledgeRetrievalResult(embedding.model(), result.stream().limit(query.topK()).toList());
    }

    private static List<RetrievedKnowledgeChunk> restoreRerankedCandidates(
            List<RetrievedKnowledgeChunk> candidates,
            List<String> rerankedIds,
            int topK
    ) {
        if (rerankedIds == null) throw new IllegalArgumentException("reranker result must not be null");
        if (rerankedIds.size() > topK) throw new IllegalArgumentException("reranker returned more than topK ids");

        var candidatesById = new LinkedHashMap<String, RetrievedKnowledgeChunk>();
        candidates.forEach(candidate -> candidatesById.putIfAbsent(candidate.chunkId(), candidate));
        var uniqueIds = new LinkedHashSet<String>();
        var result = new java.util.ArrayList<RetrievedKnowledgeChunk>();
        for (String chunkId : rerankedIds) {
            if (chunkId == null || chunkId.isBlank()) {
                throw new IllegalArgumentException("reranker returned a blank chunk id");
            }
            if (!uniqueIds.add(chunkId)) {
                throw new IllegalArgumentException("reranker returned duplicate chunk id " + chunkId);
            }
            RetrievedKnowledgeChunk candidate = candidatesById.get(chunkId);
            if (candidate == null) {
                throw new IllegalArgumentException("reranker returned unknown chunk id " + chunkId);
            }
            result.add(candidate);
        }
        int expectedSize = Math.min(topK, candidatesById.size());
        if (result.size() != expectedSize) {
            throw new IllegalArgumentException(
                    "reranker must return exactly " + expectedSize + " candidate ids"
            );
        }
        return List.copyOf(result);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }
}
