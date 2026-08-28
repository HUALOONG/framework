package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import org.jspecify.annotations.NullMarked;

/**
 * MySQL 方言：使用反引号转义标识符，分页采用 LIMIT/OFFSET。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class MySQLDialect extends AbstractDialect {

    @Override
    public DatabaseType type() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String name() {
        return "mysql";
    }

    @Override
    public String escapeIdentifier(String identifier) {
        return "`" + identifier + "`";
    }
}
