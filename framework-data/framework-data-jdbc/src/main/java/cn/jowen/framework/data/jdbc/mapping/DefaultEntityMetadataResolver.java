package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.data.core.meta.Column;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体元信息解析器默认实现。
 *
 * <p>按以下规则解析实体类：
 * <ul>
 *   <li>表名：取 {@link Table#value()}，为空则用 {@link NamingStrategy#toTableName(ClassName)} 推断；</li>
 *   <li>列名：取 {@link Column#value()}，为空则用 {@link NamingStrategy#toColumnName(FieldName)} 推断；</li>
 *   <li>主键：标注 {@link Id} 的字段；</li>
 *   <li>生成策略：标注 {@link GeneratedValue} 时记录其 {@link GeneratedValue.Strategy}；</li>
 *   <li>{@link Column#ignore()} 为 {@code true} 或 {@code transient}/static 字段将被忽略。</li>
 * </ul>
 * 解析结果按实体类缓存，避免重复反射。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DefaultEntityMetadataResolver implements EntityMetadataResolver {

    private final NamingStrategy namingStrategy;
    private final ConcurrentHashMap<Class<?>, EntityMetadata> cache = new ConcurrentHashMap<>();

    public DefaultEntityMetadataResolver() {
        this(new CamelCaseNamingStrategy());
    }

    public DefaultEntityMetadataResolver(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    @Override
    public EntityMetadata resolve(Class<?> entityClass) {
        EntityMetadata cached = cache.get(entityClass);
        if (cached != null) {
            return cached;
        }
        EntityMetadata metadata = doResolve(entityClass);
        EntityMetadata previous = cache.putIfAbsent(entityClass, metadata);
        return previous != null ? previous : metadata;
    }

    private EntityMetadata doResolve(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        String tableName = (table != null && !table.value().isBlank())
                ? table.value()
                : namingStrategy.toTableName(entityClass.getSimpleName());

        List<PropertyMetadata> properties = new ArrayList<>();
        String idColumn = null;

        for (Field field : ReflectionUtils.getAllFields(entityClass)) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.ignore()) {
                continue;
            }
            String columnName = (column != null && !column.value().isBlank())
                    ? column.value()
                    : namingStrategy.toColumnName(field.getName());
            boolean isId = field.isAnnotationPresent(Id.class);
            Optional<GeneratedValue.Strategy> generated = field.isAnnotationPresent(GeneratedValue.class)
                    ? Optional.of(field.getAnnotation(GeneratedValue.class).value())
                    : Optional.empty();
            if (isId) {
                idColumn = columnName;
            }
            properties.add(new PropertyMetadata(field.getName(), field.getType(), null,
                    isId, !isId, columnName, generated));
        }
        return new DefaultEntityMetadata(entityClass, tableName, List.copyOf(properties), idColumn);
    }

    /**
     * 清空解析缓存（主要用于测试或热重载场景）。
     */
    public void clearCache() {
        cache.clear();
    }
}
