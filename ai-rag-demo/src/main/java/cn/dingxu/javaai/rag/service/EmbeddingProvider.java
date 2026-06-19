package cn.dingxu.javaai.rag.service;

import java.util.List;

public interface EmbeddingProvider {

    List<List<Double>> embed(List<String> texts);
}
