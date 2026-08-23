package cn.jowen.framework.extras.desensitize.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏跳过条件注解。
 *
 * <p>在 Jackson 序列化时，当 SpEL 表达式返回 {@code true} 时跳过脱敏，
 * 供管理员查看原始数据。与 {@code framework.core.context.ContextCarrier} 配合使用，
 * 管理员可通过 {@code DesensitizeContext.skip()} 临时关闭脱敏。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 管理员查看时跳过脱敏
 * try (var ignored = DesensitizeContext.skip()) {
 *     String json = jsonMapper.writeValueAsString(user);
 * }
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-25
 * @see cn.jowen.framework.core.desensitize.DesensitizeContext
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DesensitizeCondition {

    /**
     * SpEL 表达式，返回 true 时跳过脱敏。
     */
    String value() default "";
}
