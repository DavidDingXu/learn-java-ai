package com.xiaoding.javaai.labs.alibaba;

import java.util.List;

@FunctionalInterface
public interface TextEmbeddingGateway {

    List<float[]> embed(List<String> inputs);
}
