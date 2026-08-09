package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.consultation.application.AnswerCustomerQuestion;
import com.xiaoding.javaai.customer.consultation.application.CustomerConsultationService;
import com.xiaoding.javaai.customer.consultation.application.CustomerStreamEvent;
import com.xiaoding.javaai.customer.consultation.application.HandoffConsultation;
import com.xiaoding.javaai.customer.consultation.application.RecordAnswerFeedback;
import com.xiaoding.javaai.customer.consultation.application.RetryCustomerAnswer;
import com.xiaoding.javaai.customer.consultation.domain.FeedbackRating;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.identity.CustomerAccessTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/customer/consultations")
public final class CustomerConsultationController {

    private final CustomerConsultationService service;
    private final CustomerAccessTokenProvider accessTokenProvider;

    public CustomerConsultationController(
            CustomerConsultationService service,
            CustomerAccessTokenProvider accessTokenProvider
    ) {
        this.service = service;
        this.accessTokenProvider = accessTokenProvider;
    }

    @PostMapping(
            path = "/answers",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<CustomerAnswerResponse> answer(
            @Valid @RequestBody CustomerAnswerRequest request,
            Authentication authentication
    ) {
        return service.answer(accessTokenProvider.current(authentication),
                        new AnswerCustomerQuestion(request.conversationId(), request.question()))
                .map(CustomerAnswerResponse::from);
    }

    @PostMapping(
            path = "/answers/stream",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    Flux<ServerSentEvent<CustomerStreamEvent>> stream(
            @Valid @RequestBody CustomerAnswerRequest request,
            Authentication authentication
    ) {
        return service.stream(accessTokenProvider.current(authentication),
                        new AnswerCustomerQuestion(request.conversationId(), request.question()))
                .map(event -> ServerSentEvent.builder(event)
                        .event(eventName(event))
                        .build());
    }

    @PutMapping(
            path = "/{conversationId}/attempts/{attemptId}/feedback",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Mono<Void> feedback(
            @PathVariable String conversationId,
            @PathVariable String attemptId,
            @Valid @RequestBody AnswerFeedbackRequest request,
            Authentication authentication
    ) {
        return service.recordFeedback(accessTokenProvider.current(authentication), new RecordAnswerFeedback(
                conversationId, attemptId, FeedbackRating.valueOf(request.rating()),
                request.reasonCode(), request.comment()
        ));
    }

    @PostMapping(
            path = "/{conversationId}/attempts/{attemptId}/retry",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<CustomerAnswerResponse> retry(
            @PathVariable String conversationId,
            @PathVariable String attemptId,
            Authentication authentication
    ) {
        return service.retry(accessTokenProvider.current(authentication),
                        new RetryCustomerAnswer(conversationId, attemptId))
                .map(CustomerAnswerResponse::from);
    }

    @PostMapping(
            path = "/{conversationId}/attempts/{attemptId}/handoffs",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.ACCEPTED)
    Mono<TicketHandoffReceipt> handoff(
            @PathVariable String conversationId,
            @PathVariable String attemptId,
            @Valid @RequestBody ConsultationHandoffRequest request,
            Authentication authentication
    ) {
        return service.handoff(accessTokenProvider.current(authentication),
                new HandoffConsultation(conversationId, attemptId, request.reasonCode()));
    }

    private static String eventName(CustomerStreamEvent event) {
        return switch (event) {
            case CustomerStreamEvent.SessionStarted ignored -> "session";
            case CustomerStreamEvent.Metadata ignored -> "metadata";
            case CustomerStreamEvent.Delta ignored -> "delta";
            case CustomerStreamEvent.Heartbeat ignored -> "heartbeat";
            case CustomerStreamEvent.Citation ignored -> "citation";
            case CustomerStreamEvent.Completed ignored -> "completed";
            case CustomerStreamEvent.Error ignored -> "error";
        };
    }
}
