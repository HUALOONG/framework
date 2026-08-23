package cn.jowen.framework.extras.captcha.generator;

import org.jspecify.annotations.NullMarked;

/**
 * 滑块验证码生成器（骨架）。
 *
 * <p>根据 {@link cn.jowen.framework.extras.captcha.CaptchaProperties} 生成滑块拼图图，
 * 返回图片 Base64 与缺口偏移坐标，供前端拖拽校验。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SliderCaptchaGenerator implements CaptchaGenerator {

    private final cn.jowen.framework.extras.config.CaptchaProperties properties;

    public SliderCaptchaGenerator(cn.jowen.framework.extras.config.CaptchaProperties properties) {
        this.properties = properties;
    }

    @Override
    public CaptchaImage generate() {
        throw new UnsupportedOperationException("滑块验证码生成器尚未实现，本轮预留骨架");
    }
}
