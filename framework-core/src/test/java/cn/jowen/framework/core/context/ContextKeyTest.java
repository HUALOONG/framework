package cn.jowen.framework.core.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ContextKey} 测试。
 */
class ContextKeyTest {

    @Test
    void named_createsKey() {
        ContextKey<String> key = ContextKey.named("tenant", String.class);
        assertThat(key.name()).isEqualTo("tenant");
        assertThat(key.type()).isSameAs(String.class);
    }

    @Test
    void named_blankName_throws() {
        assertThatThrownBy(() -> ContextKey.named("  ", String.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void named_nullName_throws() {
        assertThatThrownBy(() -> ContextKey.named(null, String.class))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void cast_matchingType() {
        ContextKey<Integer> key = ContextKey.named("n", Integer.class);
        assertThat(key.cast(42)).isEqualTo(42);
    }

    @Test
    void cast_nonMatchingType_returnsNull() {
        ContextKey<Integer> key = ContextKey.named("n", Integer.class);
        assertThat(key.cast("hello")).isNull();
    }

    @Test
    void cast_nullValue_returnsNull() {
        ContextKey<String> key = ContextKey.named("s", String.class);
        assertThat(key.cast(null)).isNull();
    }

    @Test
    void equals_sameNameAndType() {
        ContextKey<String> a = ContextKey.named("k", String.class);
        ContextKey<String> b = ContextKey.named("k", String.class);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_differentName_notEqual() {
        ContextKey<String> a = ContextKey.named("a", String.class);
        ContextKey<String> b = ContextKey.named("b", String.class);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_differentType_notEqual() {
        ContextKey<String> a = ContextKey.named("k", String.class);
        ContextKey<Integer> b = ContextKey.named("k", Integer.class);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_returnsName() {
        ContextKey<String> key = ContextKey.named("myKey", String.class);
        assertThat(key.toString()).isEqualTo("myKey");
    }
}