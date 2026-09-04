package cn.jowen.framework.extras.web.desensitize;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏标记注解：标注在 Controller/Service 方法上，
 * 由 {@link DesensitizeAspect} 对返回值执行 {@code @DesensitizeField} 策略脱敏。
 *
 * <p>字段级策略由 {@code cn.jowen.framework.core.desensitize.DesensitizeField} 声明，
 * 返回值支持单对象、{@code List}、数组与 {@code Map}。
 *
 * <p>示例：
 * <pre>{@code
 * @Desensitized
 * @GetMapping("/me")
 * public UserVO me() { ... }   // phone: 13812345678 -> 138****5678
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Desensitized {
}
