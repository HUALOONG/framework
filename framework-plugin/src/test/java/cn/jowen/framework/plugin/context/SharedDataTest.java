package cn.jowen.framework.plugin.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SharedDataTest {

    private SharedData data;

    @BeforeEach
    void setUp() {
        data = new SharedData();
    }

    @Test
    void putAndGet() {
        data.put("key", "value");
        assertThat(data.get("key")).isEqualTo("value");
    }

    @Test
    void getTyped_typeCast() {
        data.putTyped("num", 42);
        assertThat((Integer) data.getTyped("num")).isEqualTo(42);
    }

    @Test
    void remove_removesEntry() {
        data.put("key", "value");
        assertThat(data.remove("key")).isEqualTo("value");
        assertThat(data.containsKey("key")).isFalse();
    }

    @Test
    void containsKey_checksPresence() {
        data.put("key", "value");
        assertThat(data.containsKey("key")).isTrue();
        assertThat(data.containsKey("missing")).isFalse();
    }

    @Test
    void containsValue_checksValue() {
        data.put("key", "value");
        assertThat(data.containsValue("value")).isTrue();
        assertThat(data.containsValue("other")).isFalse();
    }

    @Test
    void clear_removesAll() {
        data.put("a", 1);
        data.put("b", 2);
        data.clear();
        assertThat(data.size()).isEqualTo(0);
        assertThat(data.isEmpty()).isTrue();
    }

    @Test
    void size_returnsCount() {
        data.put("a", 1);
        data.put("b", 2);
        assertThat(data.size()).isEqualTo(2);
    }

    @Test
    void putAll_batchInsert() {
        data.putAll(java.util.Map.of("a", 1, "b", 2));
        assertThat(data.size()).isEqualTo(2);
        assertThat(data.get("a")).isEqualTo(1);
        assertThat(data.get("b")).isEqualTo(2);
    }

    @Test
    void isEmpty_initiallyTrue() {
        assertThat(data.isEmpty()).isTrue();
    }

    @Test
    void keySet_returnsKeys() {
        data.put("a", 1);
        data.put("b", 2);
        assertThat(data.keySet()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void values_returnsAllValues() {
        data.put("a", 1);
        data.put("b", 2);
        assertThat(data.values()).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void entrySet_returnsEntries() {
        data.put("a", 1);
        assertThat(data.entrySet()).hasSize(1);
        assertThat(data.entrySet().iterator().next().getValue()).isEqualTo(1);
    }
}
