package com.xiaoding.javaai.knowledge.indexing.web;

import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskRunResult;

public record IndexTaskRunResponse(String result) {
    static IndexTaskRunResponse from(IndexTaskRunResult result) {
        return new IndexTaskRunResponse(result.name());
    }
}
