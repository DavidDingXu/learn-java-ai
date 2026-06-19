package com.xiaoding.javaai.rag.service;

import java.util.List;

public interface VectorIndex {

    void upsert(List<VectorIndexEntry> entries);

    List<VectorSearchResult> search(List<Double> queryVector, OperatorScope scope, int topK);
}
