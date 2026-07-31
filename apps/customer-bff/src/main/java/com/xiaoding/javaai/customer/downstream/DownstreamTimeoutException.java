package com.xiaoding.javaai.customer.downstream;

public final class DownstreamTimeoutException extends RuntimeException {

    private final String service;

    public DownstreamTimeoutException(String service, Throwable cause) {
        super("Downstream service timed out", cause);
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("service must not be blank");
        }
        this.service = service;
    }

    public String service() {
        return service;
    }
}
