package cn.jowen.framework.data.core.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import cn.jowen.framework.data.core.page.Pageable;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link Dialect} 接口的 default 方法 {@code buildCountSql} 与抽象方法契约。
 */
class DialectTest {

    private static final class FakeDialect implements Dialect {
        @Override
        public String buildPageSql(String sql, Pageable pageable) {
            return sql + " LIMIT " + pageable.getOffset() + "," + pageable.getSize();
        }

        @Override
        public String name() {
            return "fake";
        }
    }

    @Test
    void buildCountSqlWrapsSqlWithCount() {
        Dialect dialect = new FakeDialect();
        assertThat(dialect.buildCountSql("SELECT * FROM t"))
                .isEqualTo("SELECT COUNT(*) FROM (SELECT * FROM t) AS __cnt");
    }

    @Test
    void buildPageSqlMustBeImplementedBySubtype() {
        Dialect dialect = new FakeDialect();
        String sql = dialect.buildPageSql("SELECT * FROM t", Pageable.of(1, 10));
        assertThat(sql).isEqualTo("SELECT * FROM t LIMIT 10,10");
    }

    @Test
    void nameReturnsDialectName() {
        assertThat(new FakeDialect().name()).isEqualTo("fake");
    }
}
