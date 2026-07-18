package com.xiaoding.javaai.customer.consultation.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnswerFeedbackRequest(
        @NotBlank @Pattern(regexp = "HELPFUL|NOT_HELPFUL") String rating,
        @Size(max = 64) String reasonCode,
        @Size(max = 500) String comment
) {

    @AssertTrue(message = "reasonCode is required for NOT_HELPFUL feedback")
    public boolean isReasonCodeValid() {
        return !"NOT_HELPFUL".equals(rating) || reasonCode != null && !reasonCode.isBlank();
    }
}
