package cn.jowen.framework.extras.web.crypto;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记请求体字段需要在进入控制器前解密、响应体返回前加密。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Encrypt {

    /** 使用的密钥别名，对应配置中的密钥 */
    String keyAlias() default "default";
}
