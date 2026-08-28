package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DialectRegistry} 单元测试。
 */
class DialectRegistryTest {

    private final DialectRegistry registry = new DialectRegistry();

    @Test
    void defaultsRegistered() {
        assertThat(registry.get(DatabaseType.H2)).isInstanceOf(H2Dialect.class);
        assertThat(registry.get(DatabaseType.MYSQL)).isInstanceOf(MySQLDialect.class);
        assertThat(registry.get(DatabaseType.POSTGRESQL)).isInstanceOf(PostgreSQLDialect.class);
        assertThat(registry.get(DatabaseType.ORACLE)).isInstanceOf(OracleDialect.class);
        assertThat(registry.get(DatabaseType.SQLSERVER)).isInstanceOf(SQLServerDialect.class);
    }

    @Test
    void defaultDialect_isH2() {
        assertThat(registry.getDefault()).isInstanceOf(H2Dialect.class);
    }

    @Test
    void get_unregisteredType_returnsNull() {
        assertThat(registry.get(DatabaseType.UNKNOWN)).isNull();
    }

    @Test
    void setDefault_byType() {
        registry.setDefault(DatabaseType.MYSQL);
        assertThat(registry.getDefault()).isInstanceOf(MySQLDialect.class);
    }

    @Test
    void setDefault_unregisteredType_throws() {
        assertThatThrownBy(() -> registry.setDefault(DatabaseType.UNKNOWN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未注册方言");
    }

    @Test
    void setDefault_byDialect() {
        registry.setDefault(new OracleDialect());
        assertThat(registry.getDefault()).isInstanceOf(OracleDialect.class);
    }

    @Test
    void detectByProduct_knownType() {
        assertThat(registry.detectByProduct("MySQL", "8.0"))
                .isInstanceOf(MySQLDialect.class);
        assertThat(registry.detectByProduct("PostgreSQL", "15"))
                .isInstanceOf(PostgreSQLDialect.class);
    }

    @Test
    void detectByProduct_unknown_fallsBackToDefault() {
        assertThat(registry.detectByProduct("UnknownDB", "1.0"))
                .isInstanceOf(H2Dialect.class);
        assertThat(registry.detectByProduct(null, null))
                .isInstanceOf(H2Dialect.class);
    }

    @Test
    void getDefault_noDialectRegistered_throws() throws Exception {
        // 反射清空默认方言，触发 getDefault 的守护分支
        java.lang.reflect.Field field = DialectRegistry.class.getDeclaredField("defaultDialect");
        field.setAccessible(true);
        field.set(registry, null);

        assertThatThrownBy(registry::getDefault)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未注册任何方言");
    }

    @Test
    void register_overridesAndGet() {
        DatabaseDialect custom = new H2Dialect();
        registry.register(custom);
        assertThat(registry.get(DatabaseType.H2)).isSameAs(custom);
        // 重新设置默认方言生效
        registry.setDefault(DatabaseType.H2);
        assertThat(registry.getDefault()).isSameAs(custom);
    }
}
