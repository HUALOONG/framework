package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.RowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 标量行映射器：取结果行首列并转换为目标类型，用于 {@code queryForObject(Class)} / {@code queryForList(Class)}。
 *
 * <p>类型转换复用 {@link BeanPropertyRowMapper#convert(Object, Class)}，覆盖常见数值 / 时间 / 枚举等场景。
 *
 * @param <T> 目标类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ScalarRowMapper<T> implements RowMapper<T> {

    private final Class<T> requiredType;

    public ScalarRowMapper(Class<T> requiredType) {
        this.requiredType = requiredType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable T mapRow(Map<String, Object> row, int rowNum) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Object value = row.values().iterator().next();
        if (value == null) {
            return null;
        }
        return (T) BeanPropertyRowMapper.convert(value, requiredType);
    }
}
