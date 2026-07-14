package com.xiaoding.javaai.customer.consultation.application;

public final class ConsultationRateLimitExceededException extends RuntimeException {
    public ConsultationRateLimitExceededException() {
        super("customer consultation rate limit exceeded");
    }
}
