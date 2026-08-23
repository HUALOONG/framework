package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NullMarked;

/**
 * PostgreSQL 方言：分页采用 LIMIT/OFFSET，标识符用双引号转义。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class PostgreSQLDialect extends AbstractDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String name() {
        return "postgresql";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }
}
