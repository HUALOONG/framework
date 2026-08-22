package cn.jowen.framework.data.core.dialect;

import cn.jowen.framework.data.core.page.Pageable;
import org.jspecify.annotations.NullMarked;

/**
 * SQL 方言，负责分页语句拼装与占位符风格，按数据库类型实现并注册。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface Dialect {

    /**
     * 将基础查询语句改造为分页语句。
     *
     * @param sql      基础查询 SQL（不含 ORDER/LIMIT），不可为 {@code null}
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
     * 方言名称（如 mysql / postgresql / h2）。
     *
     * @return 名称
     */
    String name();
}
