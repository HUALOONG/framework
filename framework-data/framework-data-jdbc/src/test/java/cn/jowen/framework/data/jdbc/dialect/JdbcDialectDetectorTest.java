package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcDialectDetector} 测试。
 */
class JdbcDialectDetectorTest {

    @Test
    void detect_byProductName() {
        assertThat(JdbcDialectDetector.detect("H2", "2.2")).isEqualTo(DatabaseType.H2);
        assertThat(JdbcDialectDetector.detect("MySQL", "8.0")).isEqualTo(DatabaseType.MYSQL);
        assertThat(JdbcDialectDetector.detect("MariaDB", null)).isEqualTo(DatabaseType.MYSQL);
        assertThat(JdbcDialectDetector.detect("PostgreSQL", "15")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(JdbcDialectDetector.detect("Oracle", "19c")).isEqualTo(DatabaseType.ORACLE);
        assertThat(JdbcDialectDetector.detect("Microsoft SQL Server", null)).isEqualTo(DatabaseType.SQLSERVER);
        assertThat(JdbcDialectDetector.detect("SQLite", null)).isEqualTo(DatabaseType.UNKNOWN);
        assertThat(JdbcDialectDetector.detect(null, null)).isEqualTo(DatabaseType.UNKNOWN);
    }

    @Test
    void detectByUrl() {
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:h2:mem:test")).isEqualTo(DatabaseType.H2);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:mysql://localhost/db")).isEqualTo(DatabaseType.MYSQL);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:mariadb://localhost/db")).isEqualTo(DatabaseType.MYSQL);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:postgresql://localhost/db")).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:oracle:thin:@localhost:1521:xe")).isEqualTo(DatabaseType.ORACLE);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:sqlserver://localhost;databaseName=db"))
                .isEqualTo(DatabaseType.SQLSERVER);
        assertThat(JdbcDialectDetector.detectByUrl("jdbc:unknown:db")).isEqualTo(DatabaseType.UNKNOWN);
        assertThat(JdbcDialectDetector.detectByUrl(null)).isEqualTo(DatabaseType.UNKNOWN);
    }
}
