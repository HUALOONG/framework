package cn.jowen.framework.core.util;

import cn.jowen.framework.core.exception.SystemException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionUtilsTest {

    static class Base {
        private String privateBaseField = "base";
        public int baseMethod(int x) { return x + 1; }
    }

    static class Sub extends Base {
        private String privateSubField = "sub";
        public String subMethod(String s) { return s; }
        private Sub() { }
    }

    static class Holder {
        public String publicField = "pub";
        private String secret = "shh";
        public int add(int a, int b) { return a + b; }
        public String accept(Object o) { return o.toString(); }
        public String primitives(short s, byte b, char c, boolean bool, float f) {
            return s + ":" + b + ":" + c + ":" + bool + ":" + f;
        }
        public String numerics(long a, double b) { return a + ":" + b; }
        public void doThrow() { throw new IllegalStateException("boom"); }
        public Holder() { }
    }

    static class NoDefaultCtor {
        public NoDefaultCtor(int x) { }
    }

    @Test
    void getField_findsDeclaredAndInherited() {
        assertThat(ReflectionUtils.getField(Sub.class, "privateSubField").getName()).isEqualTo("privateSubField");
        assertThat(ReflectionUtils.getField(Sub.class, "privateBaseField").getName()).isEqualTo("privateBaseField");
    }

    @Test
    void getField_missing_throws() {
        assertThatThrownBy(() -> ReflectionUtils.getField(Sub.class, "nope"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("字段不存在");
    }

    @Test
    void getAllFields_includesSuperclassFields() {
        List<?> fields = ReflectionUtils.getAllFields(Sub.class);
        assertThat(fields).hasSize(2);
    }

    @Test
    void getFieldValue_and_setFieldValue() {
        Holder h = new Holder();
        assertThat(ReflectionUtils.getFieldValue(h, "publicField")).isEqualTo("pub");
        ReflectionUtils.setFieldValue(h, "publicField", "changed");
        assertThat(ReflectionUtils.getFieldValue(h, "publicField")).isEqualTo("changed");
    }

    @Test
    void getFieldValue_staticTarget_throws() {
        assertThatThrownBy(() -> ReflectionUtils.getFieldValue(null, "x"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法解析目标类型");
    }

    @Test
    void getMethod_findsAndInvokes() {
        Sub sub = new Sub();
        assertThat(ReflectionUtils.getMethod(Sub.class, "baseMethod", int.class).getName()).isEqualTo("baseMethod");
        assertThat(ReflectionUtils.invokeMethod(sub, "baseMethod", 4)).isEqualTo(5);
    }

    @Test
    void getMethod_missing_throws() {
        assertThatThrownBy(() -> ReflectionUtils.getMethod(Sub.class, "nope"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法不存在");
    }

    @Test
    void invokeMethod_boxingAndAssignableMatching() {
        Holder h = new Holder();
        assertThat(ReflectionUtils.invokeMethod(h, "add", 1, 2)).isEqualTo(3);
        assertThat(ReflectionUtils.invokeMethod(h, "accept", "str")).isEqualTo("str");
        assertThat(ReflectionUtils.invokeMethod(h, "accept", Integer.valueOf(5))).isEqualTo("5");
        assertThat(ReflectionUtils.invokeMethod(h, "primitives",
                Short.valueOf((short) 1), Byte.valueOf((byte) 2), 'c', Boolean.TRUE, Float.valueOf(1.5f)))
                .isEqualTo("1:2:c:true:1.5");
        assertThat(ReflectionUtils.invokeMethod(h, "numerics", 3L, 2.5))
                .isEqualTo("3:2.5");
    }

    @Test
    void invokeMethod_noMatchingParams_throws() {
        Holder h = new Holder();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(h, "add", "not", "numbers"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法参数不匹配");
    }

    @Test
    void invokeMethod_staticTarget_throws() {
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(null, "x"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("无法解析目标类型");
    }

    @Test
    void invokeMethod_propagatesCause() {
        Holder h = new Holder();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(h, "doThrow"))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void invokeMethod_nullArgToPrimitiveParam_throws() {
        Holder h = new Holder();
        assertThatThrownBy(() -> ReflectionUtils.invokeMethod(h, "add", (Object) null, 2))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("方法参数不匹配");
    }

    @Test
    void newInstance_privateConstructor() {
        Sub sub = ReflectionUtils.newInstance(Sub.class);
        assertThat(sub).isNotNull();
    }

    @Test
    void newInstance_noDefaultConstructor_throws() {
        assertThatThrownBy(() -> ReflectionUtils.newInstance(NoDefaultCtor.class))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("实例化失败");
    }
}
