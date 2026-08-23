package cn.jowen.framework.extras.captcha;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;

/**
 * 验证码业务异常（如图片编码失败、生成器不支持等可预期错误）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class CaptchaException extends BusinessException {

    public CaptchaException(String message) {
        super(message);
    }

    public CaptchaException(String message, Throwable cause) {
        super(message, cause);
    }
}
