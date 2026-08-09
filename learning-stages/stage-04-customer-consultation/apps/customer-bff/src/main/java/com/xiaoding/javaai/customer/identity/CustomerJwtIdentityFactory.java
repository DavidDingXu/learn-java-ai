package com.xiaoding.javaai.customer.identity;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public final class CustomerJwtIdentityFactory {

    public CustomerAccessToken create(Jwt jwt) {
        if (jwt == null) throw new IllegalArgumentException("jwt must not be null");
        return new CustomerAccessToken(
                jwt.getTokenValue(),
                new CustomerIdentity(
                        requireText(jwt.getSubject(), "sub"),
                        requireText(jwt.getClaimAsString("tenantId"), "tenantId"),
                        stringList(jwt, "roles"),
                        stringList(jwt, "departmentIds")
                )
        );
    }

    private static List<String> stringList(Jwt jwt, String claim) {
        List<String> values = jwt.getClaimAsStringList(claim);
        return values == null ? List.of() : values;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("customer token is missing " + name);
        }
        return value.trim();
    }
}
