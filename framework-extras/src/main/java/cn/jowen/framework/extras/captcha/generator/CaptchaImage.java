/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.generator;

import java.awt.image.BufferedImage;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * 验证码图片封装。
 *
 * @param image 渲染好的图片
 * @param answer 标准答案（校验依据）
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public record CaptchaImage(BufferedImage image, String answer) {

    public CaptchaImage {
        Objects.requireNonNull(image, "image must not be null");
        Objects.requireNonNull(answer, "answer must not be null");
    }
}
