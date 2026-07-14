package com.xiaoding.javaai.knowledge.answer.application.port;

import com.xiaoding.javaai.knowledge.answer.application.PolicyContext;
import com.xiaoding.javaai.knowledge.answer.application.PolicyContextQuery;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PolicyContextSource {

    Mono<List<PolicyContext>> load(PolicyContextQuery query);
}
