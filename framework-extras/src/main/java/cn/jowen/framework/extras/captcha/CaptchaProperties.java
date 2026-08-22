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
 * 验证码配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * <p>仅作为配置数据载体，提供默认值；等待后续装配层绑定。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class CaptchaProperties {

    /** 字符长度（默认 4）。 */
    private int length = 4;

    /** 图片宽度（默认 120）。 */
    private int width = 120;

    /** 图片高度（默认 40）。 */
    private int height = 40;

    /** 有效期（毫秒，默认 5 分钟）。 */
    private long ttlMillis = 5 * 60 * 1000L;

    /** 是否区分大小写（默认 false，校验时忽略大小写）。 */
    private boolean caseSensitive = false;

    /** 可选字符集（去除易混淆字符，如 0/O、1/I）。 */
    private String charSet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public CaptchaProperties() {
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length must be positive");
        }
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        this.height = height;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(long ttlMillis) {
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be positive");
        }
        this.ttlMillis = ttlMillis;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public String getCharSet() {
        return charSet;
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet == null ? "" : charSet;
    }
}
