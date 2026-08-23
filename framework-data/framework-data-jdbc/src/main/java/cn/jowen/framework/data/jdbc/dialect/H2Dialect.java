package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NullMarked;

/**
 * H2 方言：分页采用 LIMIT/OFFSET，标识符用双引号转义。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class H2Dialect extends AbstractDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.H2;
    }

    @Override
    public String name() {
        return "h2";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }
}
