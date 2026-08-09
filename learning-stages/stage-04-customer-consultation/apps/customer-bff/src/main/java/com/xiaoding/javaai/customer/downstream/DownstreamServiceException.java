package com.xiaoding.javaai.customer.downstream;

public final class DownstreamServiceException extends RuntimeException {

    private final String service;
    private final int statusCode;

    public DownstreamServiceException(String service, int statusCode) {
        super("Downstream service request failed");
        this.service = requireService(service);
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP status");
        }
        this.statusCode = statusCode;
    }

    public DownstreamServiceException(String service, Throwable cause) {
        super("Downstream service request failed", cause);
        this.service = requireService(service);
        this.statusCode = 0;
    }

    public String service() {
        return service;
    }

    public int statusCode() {
        return statusCode;
    }

    private static String requireService(String service) {
        if (service == null || service.isBlank()) {
            throw new IllegalArgumentException("service must not be blank");
        }
        return service;
    }
}
