package cn.jowen.framework.extras.web.captcha;

import org.jspecify.annotations.NullMarked;

/**
 * 短信验证码发送回调：屏蔽具体短信渠道（可对接 message 模块或第三方 SDK）。
 *
 * <p>框架不强制依赖任何短信服务商，由业务方注入实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface SmsCaptchaSender {

    /**
     * 发送短信验证码。
     *
     * @param phone 手机号
     * @param code  验证码明文
     */
    void send(String phone, String code);
}
