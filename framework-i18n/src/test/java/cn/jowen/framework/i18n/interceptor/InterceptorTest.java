package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.source.PropertiesMessageSource;
import cn.jowen.framework.i18n.annotation.I18nException;
import cn.jowen.framework.i18n.annotation.I18nField;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InterceptorTest {

    static final PropertiesMessageSource SOURCE = new PropertiesMessageSource("i18n/messages");

    /** 带国际化字段的 POJO。 */
    static class Order {
        @I18nField(codePrefix = "status.")
        String status;
        String raw;

        Order(String status, String raw) {
            this.status = status;
            this.raw = raw;
        }
    }

    /** 带国际化注解的异常。 */
    @I18nException("order.error")
    static class OrderException extends RuntimeException {
        OrderException(String message) {
            super(message);
        }
    }

    @Test
    void translatesAnnotatedFieldsRecursively() {
        PropertiesMessageSource source = new PropertiesMessageSource("i18n/messages");
        Order order = new Order("CANCELLED", "keep-me");
        I18nFieldInterceptor interceptor = new I18nFieldInterceptor(source);
        interceptor.translate(order, Locale.ENGLISH);
        assertThat(order.status).isEqualTo("Cancelled");
        assertThat(order.raw).isEqualTo("keep-me");
    }

    @Test
    void translatesListElements() {
        PropertiesMessageSource source = new PropertiesMessageSource("i18n/messages");
        I18nFieldInterceptor interceptor = new I18nFieldInterceptor(source);
        List<Order> orders = List.of(new Order("PAID", "x"));
        interceptor.translate(orders, Locale.ENGLISH);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).status).isEqualTo("Paid");
    }

    @Test
    void resolvesExceptionMessageWithFallback() {
        I18nExceptionInterceptor interceptor = new I18nExceptionInterceptor(SOURCE);
        OrderException ex = new OrderException("ORDER_NOT_FOUND");
        // order.error=Order failed: {0} → 参数化原始消息
        assertThat(interceptor.resolve(ex, Locale.ENGLISH)).isEqualTo("Order failed: ORDER_NOT_FOUND");
        // 无注解异常 → 原消息
        assertThat(interceptor.resolve(new IllegalStateException("boom"), Locale.ENGLISH))
                .isEqualTo("boom");
        assertThat(interceptor.resolve(null, Locale.ENGLISH)).isNull();
    }

    @Test
    void nullSafeTranslate() {
        I18nFieldInterceptor interceptor = new I18nFieldInterceptor(SOURCE);
        interceptor.translate(null, Locale.ENGLISH);
        interceptor.translate("plain", Locale.ENGLISH);
        interceptor.translate(42, Locale.ENGLISH);
    }
}
