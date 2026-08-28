package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.annotation.I18nField;
import cn.jowen.framework.i18n.api.MessageSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link I18nFieldInterceptor} 测试。
 */
class I18nFieldInterceptorTest {

    private final MessageSource messageSource = mock(MessageSource.class);
    private final I18nFieldInterceptor interceptor = new I18nFieldInterceptor(messageSource);

    @Test
    void translate_replacesAnnotatedStringField() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn("管理员");
        User user = new User();
        user.role = "user.role";

        interceptor.translate(user, Locale.CHINA);

        assertThat(user.role).isEqualTo("管理员");
    }

    @Test
    void translate_withCodePrefix() {
        when(messageSource.getMessage(eq("dict.status.ENABLED"), eq(Locale.CHINA), any())).thenReturn("启用");
        Status status = new Status();
        status.status = "ENABLED";

        interceptor.translate(status, Locale.CHINA);

        assertThat(status.status).isEqualTo("启用");
    }

    @Test
    void translate_messageNotFound_keepsCode() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn(null);
        User user = new User();
        user.role = "user.role";

        interceptor.translate(user, Locale.CHINA);

        assertThat(user.role).isEqualTo("user.role");
    }

    @Test
    void translate_nonAnnotatedField_untouched() {
        when(messageSource.getMessage(eq("name"), eq(Locale.CHINA), any())).thenReturn("改名");
        User user = new User();
        user.name = "name";

        interceptor.translate(user, Locale.CHINA);

        assertThat(user.name).isEqualTo("name");
    }

    @Test
    void translate_collection_recurses() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn("管理员");
        User a = new User();
        a.role = "user.role";
        User b = new User();
        b.role = "user.role";

        interceptor.translate(List.of(a, b), Locale.CHINA);

        assertThat(a.role).isEqualTo("管理员");
        assertThat(b.role).isEqualTo("管理员");
    }

    @Test
    void translate_map_recursesOnValues() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn("管理员");
        User user = new User();
        user.role = "user.role";

        interceptor.translate(Map.of("key", user), Locale.CHINA);

        assertThat(user.role).isEqualTo("管理员");
    }

    @Test
    void translate_null_doesNothing() {
        interceptor.translate(null, Locale.CHINA);
    }

    @Test
    void translate_nestedObject_recurses() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn("管理员");
        when(messageSource.getMessage(eq("order.status"), eq(Locale.CHINA), any())).thenReturn("已支付");
        User user = new User();
        user.role = "user.role";
        Order order = new Order();
        order.user = user;
        order.status = "order.status";

        interceptor.translate(order, Locale.CHINA);

        assertThat(order.status).isEqualTo("已支付");
        assertThat(user.role).isEqualTo("管理员");
    }

    @Test
    void translate_cycle_doesNotLoopInfinitely() {
        when(messageSource.getMessage(eq("user.role"), eq(Locale.CHINA), any())).thenReturn("管理员");
        User user = new User();
        user.role = "user.role";
        user.self = user; // 循环引用

        interceptor.translate(user, Locale.CHINA);

        assertThat(user.role).isEqualTo("管理员");
    }

    @Test
    void translate_superclassField_resolved() {
        when(messageSource.getMessage(eq("base.code"), eq(Locale.CHINA), any())).thenReturn("基类");
        SubClass sub = new SubClass();
        sub.baseCode = "base.code";

        interceptor.translate(sub, Locale.CHINA);

        assertThat(sub.baseCode).isEqualTo("基类");
    }

    static class User {
        @I18nField
        String role;
        String name;
        User self;
    }

    static class Status {
        @I18nField(codePrefix = "dict.status.")
        String status;
    }

    static class Order {
        @I18nField
        String status;
        @I18nField
        User user;
    }

    static class Base {
        @I18nField
        String baseCode;
    }

    static class SubClass extends Base {
    }
}
