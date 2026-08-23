package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * Oracle 方言：分页采用 {@code ROWNUM} 三层嵌套（兼容 11g 及更早版本）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class OracleDialect extends AbstractDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.ORACLE;
    }

    @Override
    public String name() {
        return "oracle";
    }

    @Override
    public String buildPageSql(String sql, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getSize();
        long endRow = offset + size;
        return "SELECT * FROM (SELECT a.*, ROWNUM rn__ FROM (" + sql + ") a WHERE ROWNUM <= " + endRow
                + ") WHERE rn__ > " + offset;
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }
}
