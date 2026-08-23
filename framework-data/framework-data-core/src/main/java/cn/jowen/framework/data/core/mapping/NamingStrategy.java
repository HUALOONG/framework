package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;

/**
 * 命名策略：实体类名 → 表名、字段名 → 列名的转换规则。
 */
@NullMarked
public interface NamingStrategy {

    /**
     * 实体类名转换为表名。
     *
     * @param className 实体类简单名，不可为 {@code null}
     * @return 表名
     */
    String toTableName(String className);

    /**
     * 字段名转换为列名。
     *
     * @param fieldName 字段名，不可为 {@code null}
     * @return 列名
     */
    String toColumnName(String fieldName);
}