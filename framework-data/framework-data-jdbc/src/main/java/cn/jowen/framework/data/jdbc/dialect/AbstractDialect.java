package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * 方言抽象基类：提供 LIMIT/OFFSET 分页与计数的默认实现，子类按需覆写。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class AbstractDialect implements DatabaseDialect {

    @Override
    public abstract DatabaseType type();

    @Override
    public abstract String name();

    @Override
    public String buildPageSql(String sql, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getSize();
        return sql + " LIMIT " + size + " OFFSET " + offset;
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return identifier;
    }
}
