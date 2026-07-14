package com.xiaoding.javaai.customer.consultation.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultationHandoffRequest(
        @NotBlank @Size(max = 64) String reasonCode
) {
}
