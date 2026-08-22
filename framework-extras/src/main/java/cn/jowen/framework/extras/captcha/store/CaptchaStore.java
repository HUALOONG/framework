/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.store;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 验证码存储接口（答案 + TTL）。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public interface CaptchaStore {

    /**
     * 写入验证码答案。
     *
     * @param id         验证码标识
     * @param answer     标准答案
     * @param ttlMillis  存活毫秒
     */
    void put(String id, String answer, long ttlMillis);

    /**
     * 读取答案（过期或不存在返回 {@code null}）。
     *
     * @param id 验证码标识
     * @return 答案，过期/不存在返回 {@code null}
     */
    @Nullable
    String get(String id);

    /**
     * 删除验证码。
     *
     * @param id 验证码标识
     */
    void remove(String id);

    /**
     * 清理所有已过期的条目（也可由 {@link #get(String)} 惰性清理）。
     */
    void clearExpired();
}
