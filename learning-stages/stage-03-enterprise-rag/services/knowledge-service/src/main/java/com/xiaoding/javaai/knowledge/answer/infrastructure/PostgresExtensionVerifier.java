package com.xiaoding.javaai.knowledge.answer.infrastructure;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class PostgresExtensionVerifier {

    private static final List<String> REQUIRED_EXTENSIONS = List.of("vector", "pg_trgm");

    private PostgresExtensionVerifier() {
    }

    static void verify(DataSource dataSource) {
        Set<String> installed = new HashSet<>();
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT extname FROM pg_extension WHERE extname IN ('vector', 'pg_trgm')")) {
            while (result.next()) {
                installed.add(result.getString(1));
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Cannot inspect PostgreSQL extensions", error);
        }

        List<String> missing = REQUIRED_EXTENSIONS.stream()
                .filter(extension -> !installed.contains(extension))
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        String commands = missing.stream()
                .map(extension -> "CREATE EXTENSION IF NOT EXISTS " + extension + ";")
                .collect(Collectors.joining(" "));
        throw new IllegalStateException(
                "Required PostgreSQL extensions are missing: " + String.join(", ", missing)
                        + ". Connect as a database administrator and run: " + commands);
    }
}
