package com.xiaoding.javaai.knowledge.answer.infrastructure;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgresExtensionVerifierTest {

    @Test
    void accepts_a_database_with_both_required_extensions() throws Exception {
        DataSource dataSource = dataSourceWithExtensions("vector", "pg_trgm");

        assertThatCode(() -> PostgresExtensionVerifier.verify(dataSource))
                .doesNotThrowAnyException();
    }

    @Test
    void names_the_missing_extension_and_the_admin_command() throws Exception {
        DataSource dataSource = dataSourceWithExtensions("vector");

        assertThatThrownBy(() -> PostgresExtensionVerifier.verify(dataSource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pg_trgm")
                .hasMessageContaining("database administrator")
                .hasMessageContaining("CREATE EXTENSION IF NOT EXISTS pg_trgm;");
    }

    private static DataSource dataSourceWithExtensions(String... extensions) throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(
                "SELECT extname FROM pg_extension WHERE extname IN ('vector', 'pg_trgm')"))
                .thenReturn(resultSet);

        Boolean[] rows = new Boolean[extensions.length + 1];
        for (int index = 0; index < extensions.length; index++) {
            rows[index] = true;
        }
        rows[extensions.length] = false;
        when(resultSet.next()).thenReturn(rows[0], java.util.Arrays.copyOfRange(rows, 1, rows.length));
        when(resultSet.getString(1)).thenReturn(
                extensions.length == 0 ? null : extensions[0],
                extensions.length <= 1
                        ? new String[0]
                        : java.util.Arrays.copyOfRange(extensions, 1, extensions.length));
        return dataSource;
    }
}
