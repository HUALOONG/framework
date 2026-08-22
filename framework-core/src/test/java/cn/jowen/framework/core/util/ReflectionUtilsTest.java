package cn.jowen.framework.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

/**
 * {@link ReflectionUtils} 反射工具测试。
 */
class ReflectionUtilsTest {

    @Test
    void getField_findsInheritedField() {
        assertThat(ReflectionUtils.getField(Child.class, "parentField").getName()).isEqualTo("parentField");
        assertThat(ReflectionUtils.getField(Child.class, "childField").getName()).isEqualTo("childField");
    }

    @Test
    void getField_missing_throws() {
        assertThatThrownBy(() -> ReflectionUtils.getField(Child.class, "nope"))
                .isInstanceOf(SystemException.class);
    }

    @Test
    void setAndGetFieldValue() {
        Child child = new Child();
        ReflectionUtils.setFieldValue(child, "parentField", "p-1");
        ReflectionUtils.setFieldValue(child, "childField", "c-1");
        assertThat(ReflectionUtils.getFieldValue(child, "parentField")).isEqualTo("p-1");
        assertThat(ReflectionUtils.getFieldValue(child, "childField")).isEqualTo("c-1");
    }

    @Test
    void getAllFields_includesInherited() {
        assertThat(ReflectionUtils.getAllFields(Child.class))
                .extracting(f -> f.getName())
                .contains("parentField", "childField");
    }

    @Test
    void invokeMethod_works() {
        Child child = new Child();
        assertThat(ReflectionUtils.invokeMethod(child, "sum", 2, 3)).isEqualTo(5);
    }

    @Test
    void invokeMethod_propagatesCause() {
        Child child = new Child();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(child, "boom"))
                .isInstanceOf(SystemException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void newInstance_privateConstructor() {
        assertThat(ReflectionUtils.newInstance(Hidden.class)).isNotNull();
    }

    static class Parent {
        private String parentField;

        int sum(int a, int b) {
            return a + b;
        }

        void boom() {
            throw new IllegalStateException("explode");
        }
    }

    static class Child extends Parent {
        private String childField;
    }

    static class Hidden {
        private Hidden() {
        }
    }
}
