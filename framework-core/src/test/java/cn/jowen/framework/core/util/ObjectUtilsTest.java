package cn.jowen.framework.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ObjectUtils} 测试。
 */
class ObjectUtilsTest {

    @Test
    void defaultIfNull_objNotNull_returnsObj() {
        assertThat(ObjectUtils.defaultIfNull("a", "b")).isEqualTo("a");
    }

    @Test
    void defaultIfNull_objNull_returnsDefault() {
        assertThat(ObjectUtils.defaultIfNull(null, "default")).isEqualTo("default");
    }

    @Test
    void defaultIfNull_bothNull_returnsNull() {
        assertThat(ObjectUtils.defaultIfNull(null, (String) null)).isNull();
    }

    @Test
    void defaultIfNull_supplier_notCalledWhenNotNull() {
        boolean[] called = {false};
        ObjectUtils.defaultIfNull("x", () -> {
            called[0] = true;
            return "fallback";
        });
        assertThat(called[0]).isFalse();
    }

    @Test
    void defaultIfNull_supplier_calledWhenNull() {
        assertThat(ObjectUtils.defaultIfNull((String) null, () -> "supplier")).isEqualTo("supplier");
    }

    @Test
    void nullSafeHashCode_nullReturnsZero() {
        assertThat(ObjectUtils.nullSafeHashCode(null)).isEqualTo(0);
    }

    @Test
    void nullSafeHashCode_value() {
        String s = "hello";
        assertThat(ObjectUtils.nullSafeHashCode(s)).isEqualTo(s.hashCode());
    }
}