package cn.jowen.framework.data.jdbc.mapping;

import cn.jowen.framework.data.core.mapping.RowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 行映射器：原样返回「列名 → 值」的 {@link Map}，供无实体类的动态查询使用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class MapRowMapper implements RowMapper<Map<String, Object>> {

    /** 全局单例。 */
    public static final MapRowMapper INSTANCE = new MapRowMapper();

    private MapRowMapper() {
    }

    @Override
    public @Nullable Map<String, Object> mapRow(Map<String, Object> row, int rowNum) {
        return row;
    }
}
