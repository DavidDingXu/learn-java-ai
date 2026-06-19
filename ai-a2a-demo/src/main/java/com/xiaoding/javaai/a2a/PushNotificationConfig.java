package com.xiaoding.javaai.a2a;

public record PushNotificationConfig(
        String url,
        String token
) {
    public PushNotificationConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("push notification url must not be blank");
        }
        token = token == null ? "" : token;
    }
}
