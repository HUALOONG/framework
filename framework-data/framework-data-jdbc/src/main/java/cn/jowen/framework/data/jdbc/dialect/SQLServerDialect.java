package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * SQL Server 方言：分页采用 {@code OFFSET ... ROWS FETCH NEXT ... ROWS ONLY}（需配合 ORDER BY）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SQLServerDialect extends AbstractDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.SQLSERVER;
    }

    @Override
    public String name() {
        return "sqlserver";
    }

    @Override
    public String buildPageSql(String sql, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getSize();
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "[" + identifier + "]";
    }
}
