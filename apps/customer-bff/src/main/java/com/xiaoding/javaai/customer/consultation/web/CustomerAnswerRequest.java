package com.xiaoding.javaai.customer.consultation.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerAnswerRequest(
        @Size(max = 64) String conversationId,
        @NotBlank @Size(max = 2000) String question
) {
}
