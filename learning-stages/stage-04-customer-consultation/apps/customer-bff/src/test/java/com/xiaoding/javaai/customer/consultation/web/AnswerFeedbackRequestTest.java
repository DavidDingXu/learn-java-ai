package com.xiaoding.javaai.customer.consultation.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerFeedbackRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requires_a_reason_code_only_for_not_helpful_feedback() {
        assertThat(validator.validate(new AnswerFeedbackRequest("HELPFUL", null, null))).isEmpty();
        assertThat(validator.validate(new AnswerFeedbackRequest("NOT_HELPFUL", null, null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("reasonCodeValid");
        assertThat(validator.validate(new AnswerFeedbackRequest(
                "NOT_HELPFUL", "ANSWER_INCOMPLETE", null))).isEmpty();
    }
}
