package cn.jowen.framework.data.jdbc.mapping;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MapRowMapper} 单元测试。
 */
class MapRowMapperTest {

    @Test
    void mapRow_returnsOriginalRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1);
        row.put("name", "jowen");

        Map<String, Object> result = MapRowMapper.INSTANCE.mapRow(row, 3);

        assertThat(result).isSameAs(row);
    }

    @Test
    void mapRow_acceptsEmptyMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        assertThat(MapRowMapper.INSTANCE.mapRow(row, 0)).isEmpty();
    }

    @Test
    void instance_isSingleton() {
        assertThat(MapRowMapper.INSTANCE).isSameAs(MapRowMapper.INSTANCE);
    }
}
