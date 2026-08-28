package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.extras.web.properties.ExtrasWebProperties;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 算术验证码生成器：产出形如 {@code 3 + 5 = ?} 的题目，答案为计算结果。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ArithmeticCaptchaGenerator implements CaptchaGenerator {

    /** length 不可变字段。 */
    private final int length;

    /**
     * 构造实例。
     * @param length 参数 length
     */
    public ArithmeticCaptchaGenerator(int length) {
        this.length = length;
    }

    /**
     * 执行type操作。
     * @return 结果
     */
    @Override
    public ExtrasWebProperties.Captcha.CaptchaType type() {
        return ExtrasWebProperties.Captcha.CaptchaType.ARITHMETIC;
    }

    /**
     * 执行generate操作。
     * @param expireSeconds 参数 expireSeconds
     * @return 结果
     */
    @Override
    public Captcha generate(long expireSeconds) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int bound = Math.max(10, 10 * length);
        int a = rnd.nextInt(1, bound);
        int b = rnd.nextInt(1, bound);
        String question = a + " + " + b + " = ?";
        return new Captcha(UUID.randomUUID().toString(), String.valueOf(a + b),
                question, null, System.currentTimeMillis() + expireSeconds * 1000L);
    }
}
