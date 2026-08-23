package cn.jowen.framework.data.jdbc.statement;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * 链式 SQL 构建器：以片段拼接方式组装 SELECT / INSERT / UPDATE / DELETE。
 *
 * <p>设计为轻量助手，仓库层通过 {@link #append(String, Object...)} 追加片段与参数，
 * 最终调用 {@link #build()} 产出 {@link SqlResult}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SqlBuilder {

    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    private SqlBuilder() {
    }

    /**
     * 创建构建器实例。
     *
     * @return 构建器，不可为 {@code null}
     */
    public static SqlBuilder create() {
        return new SqlBuilder();
    }

    /**
     * 追加 SQL 片段；片段中 {@code ?} 数量应与 params 个数一致。
     *
     * @param fragment SQL 片段，不可为 {@code null}
     * @param params   片段参数，按顺序绑定
     * @return 构建器自身
     */
    public SqlBuilder append(String fragment, Object... params) {
        sql.append(fragment);
        for (Object param : params) {
            this.params.add(param);
        }
        return this;
    }

    /**
     * 追加无参 SQL 片段（如 {@code " AND "}、{@code " ORDER BY "}）。
     *
     * @param fragment SQL 片段，不可为 {@code null}
     * @return 构建器自身
     */
    public SqlBuilder append(String fragment) {
        sql.append(fragment);
        return this;
    }

    /**
     * 构建最终 SQL 结果。
     *
     * @return SQL 结果，不可为 {@code null}
     */
    public SqlResult build() {
        return new SqlResult(sql.toString(), new ArrayList<>(params));
    }

    @Override
    public String toString() {
        return sql.toString();
    }
}
