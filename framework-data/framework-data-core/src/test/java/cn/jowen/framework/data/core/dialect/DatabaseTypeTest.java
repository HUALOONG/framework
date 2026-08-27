package cn.jowen.framework.data.core.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DatabaseTypeTest {

    @Test
    void enumValues() {
        DatabaseType[] values = DatabaseType.values();
        assertThat(values).containsExactly(
            DatabaseType.MYSQL,
            DatabaseType.POSTGRESQL,
            DatabaseType.ORACLE,
            DatabaseType.SQLSERVER,
            DatabaseType.H2,
            DatabaseType.UNKNOWN
        );
    }
}
