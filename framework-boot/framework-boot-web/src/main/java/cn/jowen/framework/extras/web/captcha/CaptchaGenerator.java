package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

/**
 * 验证码生成器：按类型产出 {@link Captcha}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CaptchaGenerator {

    /** @return 支持的验证码类型 */
    ExtrasWebProperties.Captcha.CaptchaType type();

    /**
     * 生成验证码。
     *
     * @param expireSeconds 有效期（秒）
     * @return 验证码
     */
    Captcha generate(long expireSeconds);
}
