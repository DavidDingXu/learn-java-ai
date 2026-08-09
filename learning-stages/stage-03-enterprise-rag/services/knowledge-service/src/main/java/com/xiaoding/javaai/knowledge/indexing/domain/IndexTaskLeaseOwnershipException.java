package com.xiaoding.javaai.knowledge.indexing.domain;

public final class IndexTaskLeaseOwnershipException extends RuntimeException {
    IndexTaskLeaseOwnershipException(String expectedOwner, String actualOwner) {
        super("index task lease belongs to " + actualOwner + ", not " + expectedOwner);
    }
}
