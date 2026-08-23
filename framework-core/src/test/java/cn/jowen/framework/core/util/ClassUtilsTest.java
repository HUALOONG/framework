package cn.jowen.framework.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ClassUtils} 测试。
 */
class ClassUtilsTest {

    @Test
    void getDefaultClassLoader_notNull() {
        assertThat(ClassUtils.getDefaultClassLoader()).isNotNull();
    }

    @Test
    void forName_validClass() throws ClassNotFoundException {
        Class<?> clazz = ClassUtils.forName("java.lang.String");
        assertThat(clazz).isSameAs(String.class);
    }

    @Test
    void forName_nullClassName_throwsNullPointerException() {
        assertThatThrownBy(() -> ClassUtils.forName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void forName_invalidClass_throwsClassNotFoundException() {
        assertThatThrownBy(() -> ClassUtils.forName("nonexistent.class.X"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void isSameType_bothSameClass_returnsTrue() {
        assertThat(ClassUtils.isSameType("a", "b")).isTrue();
    }

    @Test
    void isSameType_differentClass_returnsFalse() {
        assertThat(ClassUtils.isSameType("a", 1)).isFalse();
    }

    @Test
    void isSameType_bothNull_returnsTrue() {
        assertThat(ClassUtils.isSameType(null, null)).isTrue();
    }

    @Test
    void isSameType_oneNull_returnsFalse() {
        assertThat(ClassUtils.isSameType(null, "a")).isFalse();
        assertThat(ClassUtils.isSameType("a", null)).isFalse();
    }
}