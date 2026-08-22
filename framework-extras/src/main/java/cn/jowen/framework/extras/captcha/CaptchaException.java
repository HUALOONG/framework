/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;

/**
 * 验证码业务异常（如图片编码失败、生成器不支持等可预期错误）。
 *
 * @author Jowen
 * @date 2026-08-22
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
