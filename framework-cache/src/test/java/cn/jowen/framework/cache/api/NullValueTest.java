package cn.jowen.framework.cache.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NullValue} 测试。
 */
class NullValueTest {

    @Test
    void instance_isSingleton() {
        assertThat(NullValue.INSTANCE).isNotNull();
    }

    @Test
    void instance_isSameReference() {
        NullValue v1 = NullValue.INSTANCE;
        NullValue v2 = NullValue.INSTANCE;
        assertThat(v1).isSameAs(v2);
    }

    @Test
    void toString_returnsNullValue() {
        assertThat(NullValue.INSTANCE.toString()).isEqualTo("NullValue{}");
    }

    @Test
    void instanceOf_check() {
        assertThat(NullValue.INSTANCE instanceof NullValue).isTrue();
        assertThat(null instanceof NullValue).isFalse();
    }
}
