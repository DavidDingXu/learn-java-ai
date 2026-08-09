package com.xiaoding.javaai.knowledge.indexing.domain;

public final class IndexTaskNotClaimableException extends RuntimeException {
    IndexTaskNotClaimableException(IndexTaskStatus status) {
        super("index task is not claimable in status " + status);
    }
}
