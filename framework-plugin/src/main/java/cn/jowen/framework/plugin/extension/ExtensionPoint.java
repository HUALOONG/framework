package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口为扩展点。{@link Extension} 注解的实现类可经 {@link ExtensionRegistry} 注册并被查询。
 *
 * <p>示例：
 * <pre>{@code
 * @ExtensionPoint
 * public interface DataTransformer {
 *     String transform(String input);
 * }
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExtensionPoint {

    /**
     * 扩展点唯一标识。为空时默认使用类的全限定名。
     *
     * @return 扩展点标识
     */
    String value() default "";
}
