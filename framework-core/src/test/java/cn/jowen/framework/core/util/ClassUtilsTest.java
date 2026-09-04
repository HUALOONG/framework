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
    void getDefaultClassLoader_nullContextLoader_fallsBackToClassClassLoader() {
        // 上下文类加载器为空时须回退到本类加载器，且必须恢复原值以免污染其它测试
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            assertThat(ClassUtils.getDefaultClassLoader()).isSameAs(ClassUtils.class.getClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
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