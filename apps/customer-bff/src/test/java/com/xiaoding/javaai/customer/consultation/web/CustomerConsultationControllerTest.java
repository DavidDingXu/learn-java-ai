package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.consultation.application.CustomerAnswer;
import com.xiaoding.javaai.customer.consultation.application.CustomerConsultationService;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.identity.FixedCustomerAccessTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerConsultationControllerTest {

    @Test
    void returns_a_channel_dto_without_provider_model_or_token_metadata() {
        CustomerConsultationService service = mock(CustomerConsultationService.class);
        when(service.answer(any(), any())).thenReturn(Mono.just(answer()));
        CustomerConsultationController controller = new CustomerConsultationController(
                service, fixedIdentity());

        CustomerAnswerResponse response = controller.answer(
                new CustomerAnswerRequest(null, "退款多久到账？"), null).block();

        assertThat(response.conversationId()).isEqualTo("conversation-1");
        assertThat(response.attemptId()).isEqualTo("attempt-1");
        assertThat(response.citations()).singleElement()
                .extracting(CitationView::sectionId)
                .isEqualTo("arrival-time");
    }

    @Test
    void maps_feedback_retry_and_handoff_to_attempt_scoped_commands() {
        CustomerConsultationService service = mock(CustomerConsultationService.class);
        when(service.recordFeedback(any(), any())).thenReturn(Mono.empty());
        when(service.retry(any(), any())).thenReturn(Mono.just(answer()));
        when(service.handoff(any(), any())).thenReturn(Mono.just(
                new TicketHandoffReceipt("task-100", "ACCEPTED", false)));
        CustomerConsultationController controller = new CustomerConsultationController(
                service, fixedIdentity());

        controller.feedback("conversation-1", "attempt-1",
                new AnswerFeedbackRequest("NOT_HELPFUL", "ANSWER_INCOMPLETE", "缺少银行卡差异"),
                null).block();
        controller.retry("conversation-1", "attempt-1", null).block();
        TicketHandoffReceipt receipt = controller.handoff(
                "conversation-1", "attempt-1",
                new ConsultationHandoffRequest("CUSTOMER_REQUESTED_HUMAN"), null).block();

        verify(service).recordFeedback(any(), any());
        verify(service).retry(any(), any());
        verify(service).handoff(any(), any());
        assertThat(receipt.taskId()).isEqualTo("task-100");
    }

    @Test
    void binds_attempt_scoped_path_variables_through_the_real_http_router() {
        CustomerConsultationService service = mock(CustomerConsultationService.class);
        when(service.recordFeedback(any(), any())).thenReturn(Mono.empty());
        when(service.retry(any(), any())).thenReturn(Mono.just(answer()));
        when(service.handoff(any(), any())).thenReturn(Mono.just(
                new TicketHandoffReceipt("task-100", "ACCEPTED", false)));
        WebTestClient client = WebTestClient.bindToController(new CustomerConsultationController(
                        service, fixedIdentity()))
                .build();

        client.put()
                .uri("/api/v1/customer/consultations/conversation-1/attempts/attempt-1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AnswerFeedbackRequest(
                        "NOT_HELPFUL", "ANSWER_INCOMPLETE", "缺少银行卡差异"))
                .exchange()
                .expectStatus().isNoContent();

        client.post()
                .uri("/api/v1/customer/consultations/conversation-1/attempts/attempt-1/retry")
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/customer/consultations/conversation-1/attempts/attempt-1/handoffs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ConsultationHandoffRequest("CUSTOMER_REQUESTED_HUMAN"))
                .exchange()
                .expectStatus().isAccepted();

        verify(service).recordFeedback(any(), any());
        verify(service).retry(any(), any());
        verify(service).handoff(any(), any());
    }

    private static CustomerAnswer answer() {
        return new CustomerAnswer(
                "conversation-1", "attempt-1", null,
                "退款通常在 1 到 5 个工作日到账。",
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false, null, "trace-123"
        );
    }

    private static FixedCustomerAccessTokenProvider fixedIdentity() {
        return new FixedCustomerAccessTokenProvider(
                "tenant-a", "local-user", List.of("customer"), List.of("support"));
    }
}
