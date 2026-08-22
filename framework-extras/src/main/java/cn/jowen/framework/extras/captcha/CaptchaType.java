/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码类型枚举（全量声明）。
 *
 * <p>本轮仅 {@link #IMAGE} 与 {@link #ARITHMETIC} 提供生成器实现；
 * {@link #SLIDER} / {@link #SMS} 调用 {@code generate()} 时抛出
 * {@link UnsupportedOperationException}（缺口图与真实短信发送本轮不做）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public enum CaptchaType {

    /** 图形验证码（字符 + 干扰线 + 噪点）。 */
    IMAGE,

    /** 算术验证码（随机算式，答案自洽）。 */
    ARITHMETIC,

    /** 滑块验证码（本轮未实现）。 */
    SLIDER,

    /** 短信验证码（本轮未实现）。 */
    SMS
}
