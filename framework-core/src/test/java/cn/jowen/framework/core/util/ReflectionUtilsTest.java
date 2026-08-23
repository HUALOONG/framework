package cn.jowen.framework.core.util;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link ReflectionUtils} 测试。
 */
class ReflectionUtilsTest {

    static class Parent {
        private String parentField = "parent";

        void parentMethod() {
        }
    }

    static class Child extends Parent {
        private String childField = "child";
        private Integer num = 42;

        void childMethod() {
        }

        String greet(String name) {
            return "hello, " + name;
        }

        int add(int a, int b) {
            return a + b;
        }
    }

    @Test
    void getField_currentClassField() {
        Field field = ReflectionUtils.getField(Child.class, "childField");
        assertThat(field.getType()).isSameAs(String.class);
    }

    @Test
    void getField_parentClassField() {
        Field field = ReflectionUtils.getField(Child.class, "parentField");
        assertThat(field.getType()).isSameAs(String.class);
    }

    @Test
    void getField_nonExistent_throws() {
        assertThatThrownBy(() -> ReflectionUtils.getField(Child.class, "nope"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("字段不存在");
    }

    @Test
    void getAllFields_includesParent() {
        List<Field> fields = ReflectionUtils.getAllFields(Child.class);
        List<String> names = fields.stream().map(Field::getName).toList();
        assertThat(names).contains("childField", "parentField", "num");
    }

    @Test
    void getFieldValue_instance() {
        Child child = new Child();
        assertThat(ReflectionUtils.getFieldValue(child, "childField")).isEqualTo("child");
    }

    @Test
    void setFieldValue_instance() {
        Child child = new Child();
        ReflectionUtils.setFieldValue(child, "childField", "modified");
        assertThat(ReflectionUtils.getFieldValue(child, "childField")).isEqualTo("modified");
    }

    @Test
    void getMethod_currentClass() {
        assertThat(ReflectionUtils.getMethod(Child.class, "childMethod")).isNotNull();
    }

    @Test
    void getMethod_parentClass() {
        assertThat(ReflectionUtils.getMethod(Child.class, "parentMethod")).isNotNull();
    }

    @Test
    void invokeMethod_byName() {
        Child child = new Child();
        Object result = ReflectionUtils.invokeMethod(child, "greet", "world");
        assertThat(result).isEqualTo("hello, world");
    }

    @Test
    void invokeMethod_intAutoboxing() {
        Child child = new Child();
        Object result = ReflectionUtils.invokeMethod(child, "add", 3, 7);
        assertThat(result).isEqualTo(10);
    }

    @Test
    void invokeMethod_noSuchMethod_throws() {
        Child child = new Child();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(child, "nope"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法不存在");
    }

    @Test
    void invokeMethod_paramMismatch_throws() {
        Child child = new Child();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(child, "add", 3, "x"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法参数不匹配");
    }

    @Test
    void newInstance_publicNoArg() {
        Child child = ReflectionUtils.newInstance(Child.class);
        assertThat(child).isNotNull();
    }

    @Test
    void newInstance_noNoArgConstructor_throws() {
        assertThatThrownBy(() -> ReflectionUtils.newInstance(ChildWithArg.class))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("实例化失败");
    }

    static class ChildWithArg {
        ChildWithArg(String s) {
        }
    }
}