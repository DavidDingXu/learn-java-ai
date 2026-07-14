package com.xiaoding.javaai.customer.identity;

import reactor.core.publisher.Mono;

public interface DelegatedTokenClient {
    Mono<DelegatedAccessToken> exchange(CustomerAccessToken source);
}
