package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.RowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 基于 {@link EntityMetadata} 的反射行映射器，将一行（列名→值）映射为实体实例。
 *
 * @param <T> 实体类型
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class BeanRowMapper<T> implements RowMapper<T> {

    private final Class<T> entityClass;
    private final EntityMetadata metadata;

    public BeanRowMapper(Class<T> entityClass, EntityMetadataResolver resolver) {
        this.entityClass = entityClass;
        this.metadata = resolver.resolve(entityClass);
    }

    @Override
    public @Nullable T mapRow(Map<String, Object> row, int rowNum) {
        T instance;
        try {
            Constructor<T> ctor = entityClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            instance = ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new cn.jowen.framework.data.core.exception.DataException(
                    "无法实例化实体 " + entityClass.getName(), e);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String fieldName = metadata.getFieldName(entry.getKey());
            if (fieldName == null) {
                continue;
            }
            Object value = convert(entry.getValue(), fieldName);
            setField(instance, fieldName, value);
        }
        return instance;
    }

    private @Nullable Object convert(@Nullable Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        Class<?> type = fieldType(fieldName);
        if (type.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (type == Long.class || type == long.class) {
            return ((Number) value).longValue();
        }
        if (type == Integer.class || type == int.class) {
            return ((Number) value).intValue();
        }
        if (type == BigDecimal.class && value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (type == BigInteger.class && value instanceof Number) {
            return BigInteger.valueOf(((Number) value).longValue());
        }
        if (type == LocalDateTime.class && value instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (type == LocalDate.class && value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return value;
    }

    private Class<?> fieldType(String fieldName) {
        try {
            return entityClass.getDeclaredField(fieldName).getType();
        } catch (NoSuchFieldException e) {
            throw new cn.jowen.framework.data.core.exception.DataException("字段不存在 " + fieldName, e);
        }
    }

    private void setField(T instance, String fieldName, @Nullable Object value) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(instance, value);
        } catch (ReflectiveOperationException e) {
            throw new cn.jowen.framework.data.core.exception.DataException("设置字段失败 " + fieldName, e);
        }
    }
}
