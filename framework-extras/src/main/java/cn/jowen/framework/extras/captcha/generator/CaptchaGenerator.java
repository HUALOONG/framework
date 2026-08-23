package cn.jowen.framework.extras.captcha.generator;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码生成器接口：生成图片与标准答案。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface CaptchaGenerator {

    /**
     * 生成一张验证码图片及其答案。
     *
     * @return 图片与答案封装，不可为 {@code null}
     */
    CaptchaImage generate();
}
