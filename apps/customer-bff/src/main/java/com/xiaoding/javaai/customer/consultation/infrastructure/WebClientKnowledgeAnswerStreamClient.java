package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerStreamClient;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.List;

public final class WebClientKnowledgeAnswerStreamClient implements KnowledgeAnswerStreamClient {

    private static final ParameterizedTypeReference<ServerSentEvent<JsonNode>> EVENT_TYPE =
            new ParameterizedTypeReference<>() { };

    private final WebClient webClient;
    private final Duration idleTimeout;

    public WebClientKnowledgeAnswerStreamClient(
            WebClient.Builder builder,
            String baseUrl,
            Duration idleTimeout
    ) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        if (idleTimeout == null || idleTimeout.isZero() || idleTimeout.isNegative()) {
            throw new IllegalArgumentException("idleTimeout must be positive");
        }
        this.webClient = builder.baseUrl(baseUrl.trim()).build();
        this.idleTimeout = idleTimeout;
    }

    @Override
    public Flux<Event> stream(DelegatedAccessToken token, KnowledgeAnswerClient.Request request) {
        return webClient.post()
                .uri("/api/v1/knowledge/answers/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(headers -> headers.setBearerAuth(token.tokenValue()))
                .bodyValue(RequestBody.from(request))
                .exchangeToFlux(response -> {
                    if (response.statusCode().isError()) {
                        return Flux.error(new DownstreamServiceException(
                                "knowledge-service", response.statusCode().value()));
                    }
                    return response.bodyToFlux(EVENT_TYPE).map(this::mapEvent);
                })
                .timeout(idleTimeout);
    }

    private Event mapEvent(ServerSentEvent<JsonNode> event) {
        JsonNode data = event.data();
        if (data == null) throw new IllegalStateException("knowledge stream event has no data");
        return switch (String.valueOf(event.event())) {
            case "metadata" -> new Metadata(requiredText(data, "traceId"));
            case "delta" -> new Delta(requiredText(data, "text"));
            case "heartbeat" -> new Heartbeat(data.path("epochMillis").asLong());
            case "citation" -> new Citation(citation(data.path("citation")));
            case "completed" -> new Completed();
            case "error" -> new Error(requiredText(data, "code"), requiredText(data, "message"));
            default -> throw new IllegalStateException("unknown knowledge stream event: " + event.event());
        };
    }

    private static CitationView citation(JsonNode node) {
        return new CitationView(
                requiredText(node, "documentId"),
                requiredText(node, "version"),
                requiredText(node, "sectionId"),
                requiredText(node, "title")
        );
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asString();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("knowledge stream event is missing " + field);
        }
        return value;
    }

    private record RequestBody(String question, ConversationContextBody conversationContext) {
        static RequestBody from(KnowledgeAnswerClient.Request request) {
            return new RequestBody(request.question(), ConversationContextBody.from(request.context()));
        }
    }

    private record ConversationContextBody(String summary, List<TurnBody> turns) {
        static ConversationContextBody from(ConversationContextView context) {
            return new ConversationContextBody(context.summary(), context.turns().stream()
                    .map(turn -> new TurnBody(turn.role().name(), turn.content()))
                    .toList());
        }
    }

    private record TurnBody(String role, String content) {
    }
}
