package cn.jowen.framework.data.core.dialect;

import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * 数据库方言接口（SPI），负责 SQL 方言相关的分页、函数、关键字转义等差异。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@SPI
@NullMarked
public interface DatabaseDialect {

    /**
     * 返回数据库类型。
     *
     * @return 数据库类型，不可为 {@code null}
     */
    DatabaseType type();

    /**
     * 方言名称（如 mysql / postgresql / h2）。
     *
     * @return 名称
     */
    String name();

    /**
     * 将基础查询语句改造为分页语句。
     *
     * @param sql      基础查询 SQL，不可为 {@code null}
     * @param pageable 分页请求，不可为 {@code null}
     * @return 分页 SQL，不可为 {@code null}
     */
    String buildPageSql(String sql, Pageable pageable);

    /**
     * 计算分页查询的总记录数 SQL（对原 SQL 做 COUNT 包裹）。
     *
     * @param sql 基础查询 SQL，不可为 {@code null}
     * @return 计数 SQL，不可为 {@code null}
     */
    default String buildCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") AS __cnt";
    }

    /**
     * 转义 SQL 关键字或标识符。
     *
     * @param identifier 标识符，不可为 {@code null}
     * @return 转义后的标识符
     */
    default String escapeIdentifier(String identifier) {
        return identifier;
    }
}
