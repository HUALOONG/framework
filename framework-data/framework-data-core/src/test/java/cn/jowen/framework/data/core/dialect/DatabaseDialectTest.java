package cn.jowen.framework.data.core.dialect;

import cn.jowen.framework.data.core.page.Pageable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DatabaseDialect} 测试：接口默认方法 {@code buildCountSql} / {@code escapeIdentifier} 的开箱行为，
 * 以及实现类覆盖默认方法后的生效情况。
 */
class DatabaseDialectTest {

    /** 仅实现抽象方法的最小方言，用于验证接口默认方法。 */
    private static final DatabaseDialect MYSQL = new DatabaseDialect() {
        @Override
        public DatabaseType type() {
            return DatabaseType.MYSQL;
        }

        @Override
        public String name() {
            return "mysql";
        }

        @Override
        public String buildPageSql(String sql, Pageable pageable) {
            return sql + " LIMIT " + pageable.getSize() + " OFFSET " + pageable.getOffset();
        }
    };

    @Test
    void buildCountSql_wrapsOriginalSql() {
        assertThat(MYSQL.buildCountSql("SELECT * FROM sys_user"))
                .isEqualTo("SELECT COUNT(*) FROM (SELECT * FROM sys_user) AS __cnt");
    }

    @Test
    void escapeIdentifier_returnsRawIdentifierByDefault() {
        assertThat(MYSQL.escapeIdentifier("order")).isEqualTo("order");
        assertThat(MYSQL.escapeIdentifier("")).isEmpty();
    }

    @Test
    void buildPageSql_usesPageableOffsetAndSize() {
        assertThat(MYSQL.buildPageSql("SELECT * FROM sys_user", Pageable.of(2, 10)))
                .isEqualTo("SELECT * FROM sys_user LIMIT 10 OFFSET 20");
    }

    @Test
    void customImplementation_canOverrideDefaults() {
        DatabaseDialect postgres = new DatabaseDialect() {
            @Override
            public DatabaseType type() {
                return DatabaseType.POSTGRESQL;
            }

            @Override
            public String name() {
                return "postgresql";
            }

            @Override
            public String buildPageSql(String sql, Pageable pageable) {
                return sql;
            }

            @Override
            public String buildCountSql(String sql) {
                return "SELECT count(1) FROM (" + sql + ") t";
            }

            @Override
            public String escapeIdentifier(String identifier) {
                return '"' + identifier + '"';
            }
        };

        assertThat(postgres.type()).isEqualTo(DatabaseType.POSTGRESQL);
        assertThat(postgres.name()).isEqualTo("postgresql");
        assertThat(postgres.escapeIdentifier("order")).isEqualTo("\"order\"");
        assertThat(postgres.buildCountSql("SELECT 1")).isEqualTo("SELECT count(1) FROM (SELECT 1) t");
        assertThat(postgres.buildPageSql("SELECT 1", Pageable.of(0, 5))).isEqualTo("SELECT 1");
    }
}
