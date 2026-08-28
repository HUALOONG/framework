package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PreparedStatementBuilder} 单元测试。
 */
class PreparedStatementBuilderTest {

    private Connection connection;
    private final TypeHandlerRegistry registry = new TypeHandlerRegistry();

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:psb;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void create_withSqlResult_bindsParams() throws SQLException {
        SqlResult sqlResult = new SqlResult("SELECT ?", List.of(42));
        try (PreparedStatement stmt =
                     PreparedStatementBuilder.create(connection, sqlResult, registry)) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(42);
            }
        }
    }

    @Test
    void create_withSqlResultAndGeneratedKeys() throws SQLException {
        SqlResult sqlResult = new SqlResult("SELECT ?", List.of(7));
        try (PreparedStatement stmt = PreparedStatementBuilder.create(
                connection, sqlResult, registry, true)) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(7);
            }
        }
    }

    @Test
    void create_withSqlAndParams_bindsParams() throws SQLException {
        try (PreparedStatement stmt = PreparedStatementBuilder.create(
                connection, "SELECT ?", List.of("abc"), registry)) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("abc");
            }
        }
    }

    @Test
    void create_withSqlAndParamsAndGeneratedKeys() throws SQLException {
        try (PreparedStatement stmt = PreparedStatementBuilder.create(
                connection, "SELECT ?", List.of(1L), registry, false)) {
            try (ResultSet rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(1L);
            }
        }
    }
}
