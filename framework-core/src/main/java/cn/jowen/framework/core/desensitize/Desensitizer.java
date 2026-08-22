/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.core.desensitize;

import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.util.ReflectionUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 脱敏执行器，聚合内置策略、SPI 注册的自定义规则与 {@link DesensitizeField} 注解反射脱敏，
 * 对外提供统一的入口。
 *
 * <p>典型用法：
 * <pre>{@code
 * Desensitizer.getInstance().mask("13812345678");                    // 内置规则自动识别
 * Desensitizer.getInstance().mask("13812345678", "PHONE");           // 按策略名
 * Desensitizer.getInstance().maskObject(userVO);                     // 按 @DesensitizeField 注解
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class Desensitizer {

    private static final Desensitizer INSTANCE = new Desensitizer();

    /** 手动注册的自定义规则（SPI 自动发现的规则同样生效）。 */
    private final List<DesensitizeRule> extraRules = new ArrayList<>();

    private Desensitizer() {
    }

    /**
     * 返回全局默认执行器实例。
     *
     * @return 执行器，不可为 {@code null}
     */
    public static Desensitizer getInstance() {
        return INSTANCE;
    }

    /**
     * 对文本执行脱敏：先跑内置规则，再跑所有自定义规则（SPI + 手动注册）。
     *
     * @param text 原文，可为 {@code null}
     * @return 脱敏后文本；{@code null} 原样返回
     */
    public @Nullable String mask(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String result = MaskPattern.maskAll(text);
        for (DesensitizeRule rule : rules()) {
            result = rule.apply(result);
        }
        return result;
    }

    /**
     * 按内置策略名脱敏（大小写不敏感）。
     *
     * @param text     原文
     * @param strategy 策略名，对应 {@link DesensitizeStrategies}
     * @return 脱敏结果；未知策略抛 {@link DesensitizeException}
     * @throws DesensitizeException 策略不存在
     */
    public @Nullable String mask(@Nullable String text, String strategy) {
        return mask(text, strategy, null);
    }

    /**
     * 按内置策略名 + 上下文脱敏。
     *
     * @param text     原文
     * @param strategy 策略名
     * @param ctx      执行上下文（null 用策略默认）
     * @return 脱敏结果
     * @throws DesensitizeException 策略不存在
     */
    public @Nullable String mask(@Nullable String text, String strategy, @Nullable DesensitizeContext ctx) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return resolve(strategy).mask(text, ctx);
    }

    /**
     * 对象脱敏：反射遍历字段，对标注 {@link DesensitizeField} 的字符串字段按注解脱敏
     * （支持继承字段；静态/final 字段自动跳过）。
     *
     * @param target 目标对象（null 原样返回）
     * @return 脱敏后的同一对象（原地修改）
     * @throws DesensitizeException 注解引用的策略不存在
     */
    public @Nullable Object maskObject(@Nullable Object target) {
        if (target == null) {
            return null;
        }
        for (Field field : ReflectionUtils.getAllFields(target.getClass())) {
            DesensitizeField annotation = field.getAnnotation(DesensitizeField.class);
            if (annotation == null || field.getType() != String.class) {
                continue;
            }
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            String raw = (String) ReflectionUtils.getFieldValue(target, field.getName());
            if (raw == null || raw.isBlank()) {
                continue;
            }
            DesensitizeContext ctx = contextOf(annotation);
            String masked = resolve(annotation.strategy()).mask(raw, ctx);
            ReflectionUtils.setFieldValue(target, field.getName(), masked);
        }
        return target;
    }

    /**
     * 手动注册自定义规则（线程安全）。
     *
     * @param rule 规则，不能为 {@code null}
     */
    public synchronized void register(DesensitizeRule rule) {
        if (rule != null && !extraRules.contains(rule)) {
            extraRules.add(rule);
        }
    }

    private DesensitizeStrategies resolve(String strategy) {
        try {
            return DesensitizeStrategies.valueOf(strategy.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DesensitizeException("未知脱敏策略: " + strategy);
        }
    }

    private static DesensitizeContext contextOf(DesensitizeField annotation) {
        DesensitizeStrategies strategy = DesensitizeStrategies.valueOf(annotation.strategy().toUpperCase(Locale.ROOT));
        int startKeep = annotation.startKeep() >= 0 ? annotation.startKeep() : strategy.defaultStartKeep();
        int endKeep = annotation.endKeep() >= 0 ? annotation.endKeep() : strategy.defaultEndKeep();
        String replacement = annotation.replacement().isEmpty() ? "*" : annotation.replacement();
        return new DesensitizeContext(startKeep, endKeep, replacement, annotation.skip());
    }

    private List<DesensitizeRule> rules() {
        List<DesensitizeRule> all = new ArrayList<>(extraRules);
        all.addAll(ExtensionLoader.getExtensionLoader(DesensitizeRule.class).getActivateExtensions());
        return all;
    }
}
