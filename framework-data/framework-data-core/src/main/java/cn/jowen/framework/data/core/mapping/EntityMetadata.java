package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 实体元信息：表名、列名集合、主键字段等，由实现层基于 {@code meta} 注解反射构建并缓存。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface EntityMetadata {

    /**
     * 实体类。
     *
     * @return 实体类型，不可为 {@code null}
     */
    Class<?> getEntityClass();

    /**
     * 表名。
     *
     * @return 表名，不可为 {@code null}
     */
    String getTableName();

    /**
     * 全部可持久化列名（含主键）。
     *
     * @return 列名列表
     */
    List<String> getColumnNames();

    /**
     * 主键列名，无主键返回 {@code null}。
     *
     * @return 主键列名
     */
    @Nullable String getIdColumn();

    /**
     * 按列名取实体字段名。
     *
     * @param column 列名，不可为 {@code null}
     * @return 字段名，可能为 {@code null}
     */
    @Nullable String getFieldName(String column);
}
