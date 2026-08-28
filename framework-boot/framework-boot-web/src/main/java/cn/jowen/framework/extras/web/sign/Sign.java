package cn.jowen.framework.extras.web.sign;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求签名校验注解，标记接口需进行签名验签。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Sign {

    /** 参与签名的字段名（请求参数 key），按顺序拼接待签名串 */
    String[] fields() default {};

    /** 签名头名称 */
    String header() default "X-Signature";

    /** 时间戳头名称 */
    String timestampHeader() default "X-Timestamp";

    /** 时间戳允许的最大偏差（秒） */
    int toleranceSeconds() default 300;

    /** 验签失败提示 */
    String message() default "签名校验失败";
}
