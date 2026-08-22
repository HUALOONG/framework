/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.generator;

import org.jspecify.annotations.NullMarked;

/**
 * 验证码生成器接口：生成图片与标准答案。
 *
 * @author Jowen
 * @date 2026-08-22
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
