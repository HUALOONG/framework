/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.captcha.store;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 内存验证码存储：基于 {@link ConcurrentHashMap} + TTL（纳秒时间戳），支持惰性失效与主动清理。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public final class LocalCaptchaStore implements CaptchaStore {

    private record Entry(String answer, long expireAtNanos) {
    }

    private final ConcurrentMap<String, Entry> map = new ConcurrentHashMap<>();

    @Override
    public void put(String id, String answer, long ttlMillis) {
        if (id == null || answer == null) {
            throw new IllegalArgumentException("id and answer must not be null");
        }
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be positive");
        }
        map.put(id, new Entry(answer, System.nanoTime() + ttlMillis * 1_000_000L));
    }

    @Override
    public @Nullable String get(String id) {
        if (id == null) {
            return null;
        }
        Entry entry = map.get(id);
        if (entry == null) {
            return null;
        }
        if (entry.expireAtNanos() < System.nanoTime()) {
            map.remove(id);
            return null;
        }
        return entry.answer();
    }

    @Override
    public void remove(String id) {
        if (id != null) {
            map.remove(id);
        }
    }

    @Override
    public void clearExpired() {
        long now = System.nanoTime();
        map.entrySet().removeIf(en -> en.getValue().expireAtNanos() < now);
    }
}
