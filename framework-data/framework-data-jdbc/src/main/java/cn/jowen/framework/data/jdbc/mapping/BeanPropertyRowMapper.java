package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import cn.jowen.framework.data.core.mapping.PropertyMetadata;
import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.data.core.exception.DataAccessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bean 属性行映射器：将「列名 → 值」的 Map 映射为实体对象。
 *
 * <p>支持两类实体：
 * <ul>
 *   <li>普通 POJO：优先无参构造实例化，再按字段名反射赋值；</li>
 *   <li>record：使用规范构造器按参数名匹配列完成实例化。</li>
 * </ul>
 * 列名（snake_case）到字段名（camelCase）的映射由 {@link EntityMetadataResolver}（内含 {@link NamingStrategy}）决定，
 * 并对常见类型（数值、{@code java.time}、枚举等）做自动转换。
 *
 * @param <T> 实体类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class BeanPropertyRowMapper<T> implements RowMapper<T> {

    private static final DefaultEntityMetadataResolver DEFAULT_RESOLVER =
            new DefaultEntityMetadataResolver(new CamelCaseNamingStrategy());

    private final Class<T> mappedClass;
    private final EntityMetadataResolver resolver;

    private BeanPropertyRowMapper(Class<T> mappedClass, EntityMetadataResolver resolver) {
        this.mappedClass = mappedClass;
        this.resolver = resolver;
    }

    /**
     * 创建映射器（使用默认命名策略与缓存解析器）。
     *
     * @param clazz 实体类，不可为 {@code null}
     * @param <T>   类型
     * @return 映射器，不可为 {@code null}
     */
    public static <T> BeanPropertyRowMapper<T> of(Class<T> clazz) {
        return new BeanPropertyRowMapper<>(clazz, DEFAULT_RESOLVER);
    }

    /**
     * 创建映射器（使用指定解析器，便于复用上下文内的统一解析器）。
     *
     * @param clazz    实体类，不可为 {@code null}
     * @param resolver 解析器，不可为 {@code null}
     * @param <T>      类型
     * @return 映射器，不可为 {@code null}
     */
    public static <T> BeanPropertyRowMapper<T> of(Class<T> clazz, EntityMetadataResolver resolver) {
        return new BeanPropertyRowMapper<>(clazz, resolver);
    }

    @Override
    public @Nullable T mapRow(Map<String, Object> row, int rowNum) {
        if (row == null) {
            return null;
        }
        EntityMetadata meta = resolver.resolve(mappedClass);
        T instance = instantiate(meta, row);
        for (PropertyMetadata property : meta.getProperties()) {
            Object value = row.get(property.getColumnName());
            if (value == null) {
                continue;
            }
            Object converted = TypeConvert.convert(value, property.getJavaType());
            if (converted == null) {
                continue;
            }
            try {
                ReflectionUtils.setFieldValue(instance, property.getName(), converted);
            } catch (RuntimeException e) {
                throw new DataAccessException(
                        "映射字段失败：" + mappedClass.getName() + "." + property.getName(), e);
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T instantiate(EntityMetadata meta, Map<String, Object> row) {
        try {
            return ReflectionUtils.newInstance(mappedClass);
        } catch (RuntimeException noArgFailed) {
            // 退回 record / 全参构造器
            return instantiateByConstructor(meta, row);
        }
    }

    @SuppressWarnings("unchecked")
    private T instantiateByConstructor(EntityMetadata meta, Map<String, Object> row) {
        Constructor<?>[] constructors = mappedClass.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new DataAccessException("实体无可用构造器：" + mappedClass.getName());
        }
        Constructor<?> constructor = constructors[0];
        constructor.setAccessible(true);
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];
        List<PropertyMetadata> properties = meta.getProperties();
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            PropertyMetadata property = findByName(properties, paramName);
            if (property == null) {
                throw new DataAccessException("构造器参数 " + paramName + " 无对应实体属性：" + mappedClass.getName());
            }
            Object value = row.get(property.getColumnName());
            args[i] = value == null ? null : TypeConvert.convert(value, property.getJavaType());
        }
        try {
            return (T) constructor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new DataAccessException("实例化实体失败：" + mappedClass.getName(), e);
        }
    }

    private static @Nullable PropertyMetadata findByName(List<PropertyMetadata> properties, String name) {
        for (PropertyMetadata property : properties) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    /**
     * 类型转换工具（包内可见，供 {@link ScalarRowMapper} 等复用）。
     */
    static final class TypeConvert {
        private TypeConvert() {
        }

        @Nullable
        static Object convert(@Nullable Object value, Class<?> targetType) {
            if (value == null) {
                return null;
            }
            if (targetType.isInstance(value)) {
                return value;
            }
            try {
                if (value instanceof Number num) {
                    return fromNumber(num, targetType);
                }
                if (value instanceof String str) {
                    return fromString(str, targetType);
                }
                if (value instanceof java.sql.Timestamp ts) {
                    return fromTimestamp(ts, targetType);
                }
                if (value instanceof java.sql.Date d) {
                    return fromSqlDate(d, targetType);
                }
                if (value instanceof java.sql.Time t) {
                    return fromSqlTime(t, targetType);
                }
                if (value instanceof LocalDate ld) {
                    if (targetType == LocalDateTime.class) {
                        return ld.atStartOfDay();
                    }
                    if (targetType == java.sql.Date.class) {
                        return java.sql.Date.valueOf(ld);
                    }
                }
                if (value instanceof LocalDateTime ldt) {
                    if (targetType == LocalDate.class) {
                        return ldt.toLocalDate();
                    }
                    if (targetType == LocalTime.class) {
                        return ldt.toLocalTime();
                    }
                    if (targetType == java.sql.Timestamp.class) {
                        return java.sql.Timestamp.valueOf(ldt);
                    }
                    if (targetType == Date.class) {
                        return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
                    }
                }
                if (value instanceof LocalTime lt && targetType == java.sql.Time.class) {
                    return java.sql.Time.valueOf(lt);
                }
                if (targetType.isEnum() && value instanceof String str) {
                    return Enum.valueOf((Class<Enum>) targetType, str);
                }
                if (targetType == String.class) {
                    return value.toString();
                }
            } catch (RuntimeException e) {
                throw new DataAccessException("类型转换失败：" + value.getClass().getName()
                        + " -> " + targetType.getName() + " : " + e.getMessage(), e);
            }
            if (targetType.isInstance(value)) {
                return value;
            }
            throw new DataAccessException("无法转换类型：" + value.getClass().getName() + " -> " + targetType.getName());
        }

        private static Object fromNumber(Number num, Class<?> targetType) {
            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            }
            if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            }
            if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            }
            if (targetType == float.class || targetType == Float.class) {
                return num.floatValue();
            }
            if (targetType == short.class || targetType == Short.class) {
                return num.shortValue();
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return num.byteValue();
            }
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(num.doubleValue());
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return num.intValue() != 0;
            }
            if (targetType == String.class) {
                return num.toString();
            }
            throw new DataAccessException("无法将数值 " + num.getClass().getSimpleName() + " 转换为 " + targetType.getName());
        }

        private static Object fromString(String str, Class<?> targetType) {
            if (targetType == String.class) {
                return str;
            }
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(str.trim());
            }
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(str.trim());
            }
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(str.trim());
            }
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(str.trim());
            }
            if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(str.trim());
            }
            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(str.trim());
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(str.trim());
            }
            if (targetType == char.class || targetType == Character.class) {
                return str.isEmpty() ? ' ' : str.charAt(0);
            }
            if (targetType == BigDecimal.class) {
                return new BigDecimal(str.trim());
            }
            if (targetType == LocalDate.class) {
                return LocalDate.parse(str.trim());
            }
            if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(str.trim());
            }
            if (targetType == LocalTime.class) {
                return LocalTime.parse(str.trim());
            }
            if (targetType == java.sql.Date.class) {
                return java.sql.Date.valueOf(LocalDate.parse(str.trim()));
            }
            if (targetType == java.sql.Timestamp.class) {
                return java.sql.Timestamp.valueOf(LocalDateTime.parse(str.trim()));
            }
            if (targetType == Date.class) {
                return java.util.Date.from(LocalDateTime.parse(str.trim())
                        .atZone(java.time.ZoneId.systemDefault()).toInstant());
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<Enum>) targetType, str.trim());
            }
            throw new DataAccessException("无法将字符串转换为 " + targetType.getName());
        }

        private static Object fromTimestamp(java.sql.Timestamp ts, Class<?> targetType) {
            if (targetType == LocalDateTime.class) {
                return ts.toLocalDateTime();
            }
            if (targetType == LocalDate.class) {
                return ts.toLocalDateTime().toLocalDate();
            }
            if (targetType == LocalTime.class) {
                return ts.toLocalDateTime().toLocalTime();
            }
            if (targetType == java.sql.Date.class) {
                return java.sql.Date.valueOf(ts.toLocalDateTime().toLocalDate());
            }
            if (targetType == java.sql.Time.class) {
                return java.sql.Time.valueOf(ts.toLocalDateTime().toLocalTime());
            }
            if (targetType == java.sql.Timestamp.class) {
                return ts;
            }
            if (targetType == Date.class) {
                return new Date(ts.getTime());
            }
            if (targetType == String.class) {
                return ts.toString();
            }
            throw new DataAccessException("无法将 Timestamp 转换为 " + targetType.getName());
        }

        private static Object fromSqlDate(java.sql.Date d, Class<?> targetType) {
            if (targetType == LocalDate.class) {
                return d.toLocalDate();
            }
            if (targetType == LocalDateTime.class) {
                return d.toLocalDate().atStartOfDay();
            }
            if (targetType == java.sql.Date.class) {
                return d;
            }
            if (targetType == Date.class) {
                return new Date(d.getTime());
            }
            if (targetType == String.class) {
                return d.toString();
            }
            throw new DataAccessException("无法将 java.sql.Date 转换为 " + targetType.getName());
        }

        private static Object fromSqlTime(java.sql.Time t, Class<?> targetType) {
            if (targetType == LocalTime.class) {
                return t.toLocalTime();
            }
            if (targetType == java.sql.Time.class) {
                return t;
            }
            if (targetType == Date.class) {
                return new Date(t.getTime());
            }
            if (targetType == String.class) {
                return t.toString();
            }
            throw new DataAccessException("无法将 java.sql.Time 转换为 " + targetType.getName());
        }
    }

    /** 便于仓库层复用类型转换。 */
    public static @Nullable Object convert(@Nullable Object value, Class<?> targetType) {
        return TypeConvert.convert(value, targetType);
    }
}
