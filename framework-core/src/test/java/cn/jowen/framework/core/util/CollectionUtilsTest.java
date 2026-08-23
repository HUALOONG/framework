package cn.jowen.framework.core.util;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link CollectionUtils} 测试。
 */
class CollectionUtilsTest {

    @SuppressWarnings("unchecked")
    static Collection<?> nullCollection() {
        return null;
    }

    @SuppressWarnings("unchecked")
    static Map<?, ?> nullMap() {
        return null;
    }

    @Test
    void isEmpty_collection_nullIsTrue() {
        assertThat(CollectionUtils.isEmpty(nullCollection())).isTrue();
    }

    @Test
    void isEmpty_collection_emptyIsTrue() {
        assertThat(CollectionUtils.isEmpty(List.of())).isTrue();
    }

    @Test
    void isEmpty_collection_nonEmptyIsFalse() {
        assertThat(CollectionUtils.isEmpty(List.of("a"))).isFalse();
    }

    @Test
    void isNotEmpty_collection_inverse() {
        assertThat(CollectionUtils.isNotEmpty(nullCollection())).isFalse();
        assertThat(CollectionUtils.isNotEmpty(List.of())).isFalse();
        assertThat(CollectionUtils.isNotEmpty(List.of("a"))).isTrue();
    }

    @Test
    void isEmpty_map_nullIsTrue() {
        assertThat(CollectionUtils.isEmpty((Map<?, ?>) null)).isTrue();
    }

    @Test
    void isEmpty_map_emptyIsTrue() {
        assertThat(CollectionUtils.isEmpty(Map.of())).isTrue();
    }

    @Test
    void isEmpty_map_nonEmptyIsFalse() {
        assertThat(CollectionUtils.isEmpty(Map.of("k", "v"))).isFalse();
    }

    @Test
    void isNotEmpty_map_inverse() {
        assertThat(CollectionUtils.isNotEmpty((Map<?, ?>) null)).isFalse();
        assertThat(CollectionUtils.isNotEmpty(Map.of())).isFalse();
        assertThat(CollectionUtils.isNotEmpty(Map.of("k", "v"))).isTrue();
    }
}