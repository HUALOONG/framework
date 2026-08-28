package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 短信验证码生成器：生成数字验证码并通过 {@link SmsCaptchaSender} 下发。
 *
 * <p>验证码标识 id 由 {@link Captcha#id()} 承载，接收手机号由调用方
 * 通过 {@link #generate(String, long)} 指定。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SmsCaptchaGenerator implements CaptchaGenerator {

    /** length 不可变字段。 */
    private final int length;
    /** sender 不可变字段。 */
    private final SmsCaptchaSender sender;

    /**
     * 构造实例。
     * @param length 参数 length
     * @param sender 参数 sender
     */
    public SmsCaptchaGenerator(int length, SmsCaptchaSender sender) {
        this.length = length;
        this.sender = sender;
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public ExtrasWebProperties.Captcha.CaptchaType type() {
        return ExtrasWebProperties.Captcha.CaptchaType.SMS;
    }

    /**
     * 生成并发送短信验证码。
     *
     * @param phone         接收手机号
     * @param expireSeconds 有效期（秒）
     * @return 验证码（不含图形）
     */
    public Captcha generate(String phone, long expireSeconds) {
        if (phone == null || phone.isBlank()) {
            throw new ExtrasException("短信验证码接收手机号不能为空");
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        int bound = (int) Math.pow(10, Math.max(1, length));
        String code = String.format("%0" + Math.max(1, length) + "d", rnd.nextInt(bound));
        sender.send(phone, code);
        return new Captcha(UUID.randomUUID().toString(), code, sb.toString(), null,
                System.currentTimeMillis() + expireSeconds * 1000L);
    }

    /**
     * 执行generate操作。
     * @param expireSeconds 参数 expireSeconds
     * @return 结果
     */
    @Override
    public Captcha generate(long expireSeconds) {
        throw new ExtrasException("短信验证码需指定接收手机号，请使用 generate(phone, expireSeconds)");
    }
}
