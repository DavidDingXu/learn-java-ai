package com.xiaoding.javaai.labs.alibaba;

import java.util.List;

@FunctionalInterface
public interface TextRerankGateway {

    List<ScoredRetrievalCandidate> rerank(String query, List<RetrievalCandidate> candidates);
}
