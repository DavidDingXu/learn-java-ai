package com.xiaoding.javaai.labs.langchain4j;

import java.util.Map;

public final class FrameworkCoexistencePolicy {

    private final Map<String, FrameworkChoice> routes;

    public FrameworkCoexistencePolicy(Map<String, FrameworkChoice> routes) {
        this.routes = Map.copyOf(routes);
        if (this.routes.isEmpty()) {
            throw new IllegalArgumentException("routes must not be empty");
        }
    }

    public FrameworkChoice frameworkFor(String capability) {
        FrameworkChoice choice = routes.get(capability);
        if (choice == null) {
            throw new IllegalArgumentException("no framework route for capability: " + capability);
        }
        return choice;
    }
}
