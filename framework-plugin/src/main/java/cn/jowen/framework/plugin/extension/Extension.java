package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记类为实现某个扩展点的实现。与 {@link ExtensionPoint} 配合使用。
 *
 * <p>示例：
 * <pre>{@code
 * @Extension(point = DataTransformer.class, order = 10)
 * public class UpperCaseTransformer implements DataTransformer { ... }
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Extension {

    /**
     * 所属扩展点接口，不可为 {@code null}。
     *
     * @return 扩展点接口类型
     */
    Class<?> point();

    /**
     * 扩展实现名。为空时使用类名小写作为默认名。
     *
     * @return 实现名
     */
    String name() default "";

    /**
     * 排序权重，数值越小优先级越高。
     *
     * @return 排序值
     */
    int order() default 0;
}
