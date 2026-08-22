/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.core.desensitize;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 脱敏执行上下文：控制单次脱敏的保留位数、替换符与跳过标记。
 *
 * <p>掩码规则：保留前 {@code startKeep} 位与后 {@code endKeep} 位，中间以 {@code replacement}
 * 重复填充（每脱 1 位填充 1 个替换符）；当 {@code skip} 为 true 时原样返回不脱敏
 * （用于配置开关或审计豁免场景）。
 *
 * @param startKeep 开头保留位数（非负，负值按 0 处理）
 * @param endKeep   末尾保留位数（非负，负值按 0 处理）
 * @param replacement 替换符（默认 {@code *}，单字符）
 * @param skip      是否跳过脱敏（true 时 {@link #mask(String)} 原样返回）
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public record DesensitizeContext(int startKeep, int endKeep, String replacement, boolean skip) {

    /** 全量脱敏（不保留任何位）。 */
    public static final DesensitizeContext DEFAULT = new DesensitizeContext(0, 0, "*", false);

    /**
     * 紧凑构造：归一化参数（负数按 0 处理，替换符为空时兜底为 {@code *}）。
     */
    public DesensitizeContext {
        startKeep = Math.max(0, startKeep);
        endKeep = Math.max(0, endKeep);
        replacement = (replacement == null || replacement.isEmpty()) ? "*" : replacement;
    }

    /**
     * 便捷工厂：仅指定保留位数，其余取默认。
     *
     * @param startKeep 开头保留位数
     * @param endKeep   末尾保留位数
     * @return 上下文实例
     */
    public static DesensitizeContext of(int startKeep, int endKeep) {
        return new DesensitizeContext(startKeep, endKeep, "*", false);
    }

    /**
     * 便捷工厂：仅指定保留位数与替换符。
     *
     * @param startKeep  开头保留位数
     * @param endKeep    末尾保留位数
     * @param replacement 替换符
     * @return 上下文实例
     */
    public static DesensitizeContext of(int startKeep, int endKeep, String replacement) {
        return new DesensitizeContext(startKeep, endKeep, replacement, false);
    }

    /**
     * 对原始文本执行脱敏。
     *
     * @param raw 原始文本（null 或空白时原样返回）
     * @return 脱敏结果；{@code skip} 为 true 时返回 {@code raw} 本身
     */
    public String mask(@Nullable String raw) {
        if (raw == null || raw.isBlank() || skip) {
            return raw;
        }
        int length = raw.length();
        int keep = startKeep + endKeep;
        if (keep >= length) {
            return raw; // 全部保留，无需脱敏
        }
        int masked = length - keep;
        String head = raw.substring(0, Math.min(startKeep, length));
        String tail = startKeep >= length ? "" : raw.substring(Math.max(startKeep, length - endKeep));
        return head + replacement.repeat(masked) + tail;
    }
}
