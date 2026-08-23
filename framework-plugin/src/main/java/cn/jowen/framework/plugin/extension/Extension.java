package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 扩展实现标注。绑定到 core {@code @SPI} 的 name（String）。
 *
 * <p>示例：
 * <pre>{@code
 * @Extension(id = "alipay-provider", extensionPoint = "payment.provider", order = 10)
 * public class AlipayPaymentProvider implements PaymentProvider { ... }
 * }</pre>
 *
 * @author 王飞
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {

    /**
     * 扩展实现唯一标识。
     */
    String id();

    /**
     * 所属扩展点 id（对应 core {@code @SPI} 的 name），不可为 {@code null}。
     */
    String extensionPoint();

    /**
     * 排序权重，数值越小优先级越高。
     */
    int order() default 0;

    /**
     * 扩展属性。
     */
    String[] properties() default {};
}
