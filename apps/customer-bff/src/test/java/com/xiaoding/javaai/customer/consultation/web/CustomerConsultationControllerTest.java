package com.xiaoding.javaai.customer.consultation.web;

import com.xiaoding.javaai.customer.consultation.application.CustomerAnswer;
import com.xiaoding.javaai.customer.consultation.application.CustomerConsultationService;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.identity.CustomerJwtIdentityFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
                service, new CustomerJwtIdentityFactory());

        CustomerAnswerResponse response = controller.answer(
                new CustomerAnswerRequest(null, "退款多久到账？"), jwt()).block();

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
                service, new CustomerJwtIdentityFactory());

        controller.feedback("conversation-1", "attempt-1",
                new AnswerFeedbackRequest("NOT_HELPFUL", "ANSWER_INCOMPLETE", "缺少银行卡差异"),
                jwt()).block();
        controller.retry("conversation-1", "attempt-1", jwt()).block();
        TicketHandoffReceipt receipt = controller.handoff(
                "conversation-1", "attempt-1",
                new ConsultationHandoffRequest("CUSTOMER_REQUESTED_HUMAN"), jwt()).block();

        verify(service).recordFeedback(any(), any());
        verify(service).retry(any(), any());
        verify(service).handoff(any(), any());
        assertThat(receipt.taskId()).isEqualTo("task-100");
    }

    private static CustomerAnswer answer() {
        return new CustomerAnswer(
                "conversation-1", "attempt-1", null,
                "退款通常在 1 到 5 个工作日到账。",
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false, null, "trace-123"
        );
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("customer-token")
                .header("alg", "RS256")
                .subject("customer-42")
                .claim("tenantId", "tenant-a")
                .claim("roles", List.of("customer"))
                .claim("departmentIds", List.of("support"))
                .issuedAt(Instant.parse("2026-07-13T03:55:00Z"))
                .expiresAt(Instant.parse("2026-07-13T04:05:00Z"))
                .build();
    }
}
