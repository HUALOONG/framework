package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 实体元信息默认实现（record 风格不可变对象）。
 *
 * @param entityClass 实体类型
 * @param tableName   表名
 * @param properties  可持久化属性列表
 * @param idColumn    主键列名，无主键为 {@code null}
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record DefaultEntityMetadata(Class<?> entityClass, String tableName,
                                    List<PropertyMetadata> properties,
                                    @Nullable String idColumn) implements EntityMetadata {

    @Override
    public Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public List<PropertyMetadata> getProperties() {
        return properties;
    }

    @Override
    public @Nullable String getIdColumn() {
        return idColumn;
    }

    @Override
    public @Nullable PropertyMetadata getProperty(String column) {
        for (PropertyMetadata property : properties) {
            if (property.getColumnName().equals(column)) {
                return property;
            }
        }
        return null;
    }
}
