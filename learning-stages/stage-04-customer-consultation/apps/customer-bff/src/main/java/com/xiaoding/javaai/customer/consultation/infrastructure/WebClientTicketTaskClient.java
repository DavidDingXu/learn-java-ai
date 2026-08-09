package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.TicketTaskClient;
import com.xiaoding.javaai.customer.consultation.domain.AnswerFeedback;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffSnapshot;
import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public final class WebClientTicketTaskClient implements TicketTaskClient {

    private final WebClient webClient;
    private final Duration timeout;

    public WebClientTicketTaskClient(
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
    public Mono<TicketHandoffReceipt> createHandoff(
            DelegatedAccessToken token,
            String idempotencyKey,
            TicketHandoffSnapshot snapshot
    ) {
        return webClient.post()
                .uri("/api/v1/agent/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    headers.setBearerAuth(token.tokenValue());
                    headers.set("Idempotency-Key", idempotencyKey);
                })
                .bodyValue(toRequest(snapshot))
                .retrieve()
                .onStatus(status -> status.isError(), response -> Mono.error(
                        new DownstreamServiceException("ticket-agent-service", response.statusCode().value())))
                .bodyToMono(TaskResponse.class)
                .switchIfEmpty(Mono.error(new DownstreamServiceException(
                        "ticket-agent-service", new IllegalStateException("empty response body"))))
                .map(response -> new TicketHandoffReceipt(
                        response.taskId(), response.status(), response.duplicate()))
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        error -> new DownstreamTimeoutException("ticket-agent-service", error))
                .onErrorMap(WebClientRequestException.class,
                        error -> new DownstreamServiceException("ticket-agent-service", error))
                .onErrorMap(DecodingException.class,
                        error -> new DownstreamServiceException("ticket-agent-service", error))
                .onErrorMap(IllegalArgumentException.class,
                        error -> new DownstreamServiceException("ticket-agent-service", error));
    }

    private static TaskRequest toRequest(TicketHandoffSnapshot snapshot) {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("source", "customer-consultation");
        context.put("attemptId", snapshot.attemptId());
        context.put("question", truncate(snapshot.question(), 2000));
        context.put("previousAnswer", truncate(snapshot.answer(), 2000));
        context.put("citations", truncate(snapshot.citations().stream()
                .map(WebClientTicketTaskClient::citationKey)
                .collect(Collectors.joining(",")), 2000));
        context.put("reasonCode", snapshot.reasonCode());
        context.put("sourceTraceId", snapshot.sourceTraceId());
        if (!snapshot.conversationSummary().isBlank()) {
            context.put("conversationSummary", truncate(snapshot.conversationSummary(), 2000));
        }
        if (snapshot.refusalReason() != null) {
            context.put("refusalReason", truncate(snapshot.refusalReason(), 2000));
        }
        addFeedback(context, snapshot.feedback());
        return new TaskRequest(
                snapshot.conversationId(),
                truncate("Resolve customer consultation: " + snapshot.question(), 1000),
                context
        );
    }

    private static void addFeedback(Map<String, String> context, AnswerFeedback feedback) {
        if (feedback == null) return;
        context.put("feedbackRating", feedback.rating().name());
        if (feedback.reasonCode() != null) context.put("feedbackReason", feedback.reasonCode());
        if (feedback.comment() != null) context.put("feedbackComment", feedback.comment());
    }

    private static String citationKey(CitationView citation) {
        return citation.documentId() + "/" + citation.version() + "#" + citation.sectionId();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    private record TaskRequest(String caseId, String objective, Map<String, String> businessContext) {
    }

    private record TaskResponse(String taskId, String status, boolean duplicate) {
    }
}
