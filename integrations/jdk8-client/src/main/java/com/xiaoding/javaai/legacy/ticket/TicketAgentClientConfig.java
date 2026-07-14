package com.xiaoding.javaai.legacy.ticket;

public final class TicketAgentClientConfig {

    private final String baseUrl;
    private final int connectTimeoutMillis;
    private final int responseTimeoutMillis;
    private final int maxConnectionsPerRoute;
    private final int maxConnectionsTotal;

    public TicketAgentClientConfig(
            String baseUrl,
            int connectTimeoutMillis,
            int responseTimeoutMillis,
            int maxConnectionsPerRoute,
            int maxConnectionsTotal
    ) {
        this.baseUrl = requireBaseUrl(baseUrl);
        this.connectTimeoutMillis = requirePositive(connectTimeoutMillis, "connectTimeoutMillis");
        this.responseTimeoutMillis = requirePositive(responseTimeoutMillis, "responseTimeoutMillis");
        this.maxConnectionsPerRoute = requirePositive(maxConnectionsPerRoute, "maxConnectionsPerRoute");
        this.maxConnectionsTotal = requirePositive(maxConnectionsTotal, "maxConnectionsTotal");
        if (maxConnectionsPerRoute > maxConnectionsTotal) {
            throw new IllegalArgumentException("maxConnectionsPerRoute must not exceed maxConnectionsTotal");
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getResponseTimeoutMillis() {
        return responseTimeoutMillis;
    }

    public int getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public int getMaxConnectionsTotal() {
        return maxConnectionsTotal;
    }

    private static String requireBaseUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must use http or https");
        }
        return normalized;
    }

    private static int requirePositive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
}
