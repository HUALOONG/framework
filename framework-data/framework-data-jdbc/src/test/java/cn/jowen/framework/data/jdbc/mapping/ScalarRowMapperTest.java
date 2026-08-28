package cn.jowen.framework.data.jdbc.mapping;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ScalarRowMapper} 单元测试。
 */
class ScalarRowMapperTest {

    @Test
    void mapRow_nullRow_returnsNull() {
        assertThat(new ScalarRowMapper<>(Long.class).mapRow((Map<String, Object>) null, 0))
                .isNull();
    }

    @Test
    void mapRow_emptyRow_returnsNull() {
        assertThat(new ScalarRowMapper<>(Long.class).mapRow(Map.of(), 0)).isNull();
    }

    @Test
    void mapRow_nullFirstValue_returnsNull() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", null);
        assertThat(new ScalarRowMapper<>(Long.class).mapRow(row, 0)).isNull();
    }

    @Test
    void mapRow_takesFirstColumnAndConverts() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("cnt", "42");
        row.put("ignored", 7);
        assertThat(new ScalarRowMapper<>(Long.class).mapRow(row, 0)).isEqualTo(42L);
    }

    @Test
    void mapRow_stringType_returnsAsString() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", 123);
        assertThat(new ScalarRowMapper<>(String.class).mapRow(row, 0)).isEqualTo("123");
    }
}
