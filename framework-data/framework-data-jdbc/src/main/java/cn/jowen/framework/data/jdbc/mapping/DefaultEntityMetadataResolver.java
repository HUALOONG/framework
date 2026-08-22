package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.exception.DataException;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认实体元信息解析器：基于 {@code meta} 注解反射实体类，缓存结果。表名/列名缺省按驼峰转下划线推断。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DefaultEntityMetadataResolver implements EntityMetadataResolver {

    private final Map<Class<?>, EntityMetadata> cache = new ConcurrentHashMap<>();

    @Override
    public EntityMetadata resolve(Class<?> entityClass) {
        EntityMetadata cached = cache.get(entityClass);
        if (cached != null) {
            return cached;
        }
        EntityMetadata meta = build(entityClass);
        cache.put(entityClass, meta);
        return meta;
    }

    private EntityMetadata build(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        String tableName = (table != null && !table.value().isBlank())
                ? table.value() : toUnderline(entityClass.getSimpleName());

        Map<String, String> columnToField = new LinkedHashMap<>();
        List<String> columnNames = new ArrayList<>();
        String idColumn = null;

        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.ignore()) {
                continue;
            }
            String columnName = (column != null && !column.value().isBlank())
                    ? column.value() : toUnderline(field.getName());
            columnToField.put(columnName, field.getName());
            columnNames.add(columnName);
            if (field.isAnnotationPresent(Id.class) && idColumn == null) {
                idColumn = columnName;
            }
        }
        if (columnToField.isEmpty()) {
            throw new DataException("实体 " + entityClass.getName() + " 无任何可持久化字段");
        }
        return new DefaultEntityMetadata(entityClass, tableName, columnNames, idColumn, columnToField);
    }

    private static String toUnderline(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 默认实体元信息实现。 */
    @NullMarked
    private static final class DefaultEntityMetadata implements EntityMetadata {
        private final Class<?> entityClass;
        private final String tableName;
        private final List<String> columnNames;
        private final @Nullable String idColumn;
        private final Map<String, String> columnToField;

        private DefaultEntityMetadata(Class<?> entityClass, String tableName,
                                       List<String> columnNames, @Nullable String idColumn,
                                       Map<String, String> columnToField) {
            this.entityClass = entityClass;
            this.tableName = tableName;
            this.columnNames = columnNames;
            this.idColumn = idColumn;
            this.columnToField = columnToField;
        }

        @Override
        public Class<?> getEntityClass() {
            return entityClass;
        }

        @Override
        public String getTableName() {
            return tableName;
        }

        @Override
        public List<String> getColumnNames() {
            return columnNames;
        }

        @Override
        public @Nullable String getIdColumn() {
            return idColumn;
        }

        @Override
        public @Nullable String getFieldName(String column) {
            return columnToField.get(column);
        }
    }
}
