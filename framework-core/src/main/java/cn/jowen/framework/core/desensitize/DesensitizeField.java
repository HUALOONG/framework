package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏字段注解：标注在需要脱敏的字符串字段上，由
 * {@link Desensitizer#maskObject(Object)} 反射识别并处理。
 *
 * <p>示例：
 * <pre>{@code
 * public class UserVO {
 *     @DesensitizeField(strategy = "PHONE")
 *     private String phone;          // 13812345678 -> 138****5678
 *
 *     @DesensitizeField(strategy = "ID_CARD")
 *     private String idCard;         // 保留前 3 后 4
 * }
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DesensitizeField {

    /**
     * 脱敏策略名，对应 {@link DesensitizeStrategies} 枚举常量（大小写不敏感）。
     *
     * @return 策略名
     */
    String strategy() default "PHONE";

    /**
     * 开头保留位数（覆盖策略默认值）。
     *
     * @return 保留位数，默认 -1 表示采用策略默认
     */
    int startKeep() default -1;

    /**
     * 末尾保留位数（覆盖策略默认值）。
     *
     * @return 保留位数，默认 -1 表示采用策略默认
     */
    int endKeep() default -1;

    /**
     * 替换符（覆盖策略默认值）。
     *
     * @return 替换符，默认空串表示采用策略默认
     */
    String replacement() default "";

    /**
     * 是否跳过脱敏（true 时字段原样保留）。
     *
     * @return 默认 false
     */
    boolean skip() default false;
}
