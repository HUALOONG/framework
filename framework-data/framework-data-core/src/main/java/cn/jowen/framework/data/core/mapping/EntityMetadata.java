package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 实体元信息：表名、列名集合、主键字段等。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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
     * 全部可持久化属性（含主键）。
     *
     * @return 属性元数据列表
     */
    List<PropertyMetadata> getProperties();

    /**
     * 主键列名，无主键返回 {@code null}。
     *
     * @return 主键列名
     */
    @Nullable String getIdColumn();

    /**
     * 按列名取属性元数据。
     *
     * @param column 列名，不可为 {@code null}
     * @return 属性元数据，可能为 {@code null}
     */
    @Nullable PropertyMetadata getProperty(String column);

    /**
     * 全部列名（含主键）。
     *
     * @return 列名列表
     */
    default List<String> getColumnNames() {
        List<String> names = new java.util.ArrayList<>(getProperties().size());
        for (PropertyMetadata p : getProperties()) {
            names.add(p.getColumnName());
        }
        return names;
    }

    /**
     * 按列名取字段名。
     *
     * @param column 列名，不可为 {@code null}
     * @return 字段名，可能为 {@code null}
     */
    default @Nullable String getFieldName(String column) {
        PropertyMetadata prop = getProperty(column);
        return prop != null ? prop.getName() : null;
    }
}
