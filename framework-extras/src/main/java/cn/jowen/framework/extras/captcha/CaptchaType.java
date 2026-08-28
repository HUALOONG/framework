package cn.jowen.framework.extras.captcha;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码类型枚举（全量声明）。
 *
 * <p>{@link #IMAGE} / {@link #ARITHMETIC} / {@link #SLIDER} 已提供生成器实现；
 * {@link #SMS} 需真实短信发送能力，本轮调用 {@code generate()} 时抛
 * {@link UnsupportedOperationException}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public enum CaptchaType {

    /**
     * 图形验证码（字符 + 干扰线 + 噪点）。
     */
    IMAGE,

    /**
     * 算术验证码（随机算式，答案自洽）。
     */
    ARITHMETIC,

    /**
     * 滑块验证码（JDK AWT 基础实现，见 {@code SliderCaptchaGenerator}）。
     */
    SLIDER,

    /**
     * 短信验证码（本轮未实现）。
     */
    SMS
}
