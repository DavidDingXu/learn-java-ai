package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.consultation.domain.KnowledgeAnswerView;
import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

public final class WebClientKnowledgeAnswerClient implements KnowledgeAnswerClient {

    private final WebClient webClient;
    private final Duration timeout;

    public WebClientKnowledgeAnswerClient(
            WebClient.Builder builder,
            String baseUrl,
            Duration timeout
    ) {
        this.webClient = builder.baseUrl(requireText(baseUrl, "baseUrl")).build();
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    @Override
    public Mono<KnowledgeAnswerView> answer(DelegatedAccessToken token, Request request) {
        return webClient.post()
                .uri("/api/v1/knowledge/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(token.tokenValue()))
                .bodyValue(RequestBody.from(request))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.error(
                        new DownstreamServiceException("knowledge-service", response.statusCode().value())))
                .bodyToMono(ResponseBody.class)
                .switchIfEmpty(Mono.error(new DownstreamServiceException(
                        "knowledge-service", new IllegalStateException("empty response body"))))
                .map(ResponseBody::toDomain)
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        error -> new DownstreamTimeoutException("knowledge-service", error))
                .onErrorMap(WebClientRequestException.class,
                        error -> new DownstreamServiceException("knowledge-service", error))
                .onErrorMap(DecodingException.class,
                        error -> new DownstreamServiceException("knowledge-service", error))
                .onErrorMap(IllegalArgumentException.class,
                        error -> new DownstreamServiceException("knowledge-service", error));
    }

    private record RequestBody(String question, ConversationContextBody conversationContext) {
        static RequestBody from(Request request) {
            return new RequestBody(
                    request.question(),
                    ConversationContextBody.from(request.context())
            );
        }
    }

    private record ConversationContextBody(String summary, List<ConversationTurnBody> turns) {
        static ConversationContextBody from(ConversationContextView context) {
            return new ConversationContextBody(
                    context.summary(),
                    context.turns().stream()
                            .map(turn -> new ConversationTurnBody(
                                    turn.role().name(), turn.content()))
                            .toList()
            );
        }
    }

    private record ConversationTurnBody(String role, String content) {
    }

    private record ResponseBody(
            String answer,
            List<CitationBody> citations,
            boolean refused,
            String refusalReason,
            String traceId
    ) {
        KnowledgeAnswerView toDomain() {
            return new KnowledgeAnswerView(
                    answer,
                    citations == null ? List.of() : citations.stream().map(CitationBody::toDomain).toList(),
                    refused,
                    refusalReason,
                    traceId
            );
        }
    }

    private record CitationBody(String documentId, String version, String sectionId, String title) {
        CitationView toDomain() {
            return new CitationView(documentId, version, sectionId, title);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
