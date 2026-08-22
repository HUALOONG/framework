/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.core.desensitize;

import cn.jowen.framework.core.exception.FrameworkException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 脱敏异常：脱敏配置错误（未知策略、非法保留位数等）时抛出。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class DesensitizeException extends FrameworkException {

    /**
     * 构造异常。
     *
     * @param message 错误描述
     */
    public DesensitizeException(@Nullable String message) {
        super(message);
    }

    /**
     * 构造异常。
     *
     * @param message 错误描述
     * @param cause   根因
     */
    public DesensitizeException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
