package cn.jowen.framework.data.mybatis.repository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorAdapterTest {

    @Test
    void autoIncrement_generateUnique() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.autoIncrement();
        Object id1 = adapter.generate();
        Object id2 = adapter.generate();
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void snowflake_generateLong() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.snowflake();
        Object id = adapter.generate();
        assertThat(id).isInstanceOf(Long.class);
        assertThat((Long) id).isPositive();
    }

    @Test
    void snowflake_unique() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.snowflake();
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add((Long) adapter.generate());
        }
        assertThat(ids).hasSize(100);
    }

    @Test
    void uuid_generateString() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.uuid();
        Object id = adapter.generate();
        assertThat(id).isInstanceOf(String.class);
        assertThat((String) id).hasSize(32);
    }

    @Test
    void uuid_unique() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.uuid();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add((String) adapter.generate());
        }
        assertThat(ids).hasSize(100);
    }

    @Test
    void custom_strategy() {
        IdGeneratorAdapter adapter = IdGeneratorAdapter.custom(() -> "custom-123");
        assertThat(adapter.generate()).isEqualTo("custom-123");
    }
}
