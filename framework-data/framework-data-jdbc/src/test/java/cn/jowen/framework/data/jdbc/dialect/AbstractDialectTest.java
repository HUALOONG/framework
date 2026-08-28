package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.PageRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AbstractDialect} 默认行为单元测试。
 *
 * <p>使用仅实现抽象方法的最小子类，确保覆盖的是 {@link AbstractDialect} 自身的默认实现
 * （具体方言如 {@link H2Dialect} 会覆写 {@code escapeIdentifier}）。
 */
class AbstractDialectTest {

    /** 最小子类：仅实现抽象方法，其余沿用 AbstractDialect 默认实现。 */
    private static final class PlainDialect extends AbstractDialect {
        @Override
        public DatabaseType type() {
            return DatabaseType.H2;
        }

        @Override
        public String name() {
            return "plain";
        }
    }

    private final AbstractDialect dialect = new PlainDialect();

    @Test
    void escapeIdentifier_returnsAsIs() {
        // 默认实现不转义标识符
        assertThat(dialect.escapeIdentifier("user_name")).isEqualTo("user_name");
        assertThat(dialect.escapeIdentifier("")).isEqualTo("");
    }

    @Test
    void buildPageSql_appendsLimitOffset() {
        assertThat(dialect.buildPageSql("SELECT * FROM t", PageRequest.of(2, 10).toPageable()))
                .isEqualTo("SELECT * FROM t LIMIT 10 OFFSET 10");
    }

    @Test
    void buildPageSql_firstPage_zeroOffset() {
        assertThat(dialect.buildPageSql("SELECT * FROM t", PageRequest.of(1, 5).toPageable()))
                .isEqualTo("SELECT * FROM t LIMIT 5 OFFSET 0");
    }

    @Test
    void typeAndName() {
        assertThat(dialect.type()).isEqualTo(DatabaseType.H2);
        assertThat(dialect.name()).isEqualTo("plain");
    }

    @Test
    void h2Dialect_escapeIdentifier_quotesWithDoubleQuote() {
        // 具体方言覆写默认转义行为
        assertThat(new H2Dialect().escapeIdentifier("user")).isEqualTo("\"user\"");
    }
}
