package cn.jowen.framework.core.desensitize;

import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.core.util.ReflectionUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 脱敏执行器，聚合内置策略、SPI 注册的自定义规则与 {@link DesensitizeField} 注解反射脱敏，
 * 对外提供统一的入口。
 *
 * <p>典型用法：
 * <pre>{@code
 * Desensitizer.getInstance().mask("13812345678");                    // 内置规则自动识别
 * Desensitizer.getInstance().mask("13812345678", "PHONE");           // 按策略名
 * Desensitizer.getInstance().maskObject(userVO);                     // 按 @DesensitizeField 注解
 * Desensitizer.getInstance().maskMap(map, "PHONE");                  // Map 值按策略脱敏
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Desensitizer {

    private static final Desensitizer INSTANCE = new Desensitizer();

    /**
     * 手动注册的自定义规则（SPI 自动发现的规则同样生效），线程安全。
     */
    private final List<DesensitizeRule> extraRules = Collections.synchronizedList(new ArrayList<>());

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

    private static DesensitizeContext contextOf(DesensitizeField annotation) {
        DesensitizeStrategies strategy = DesensitizeStrategies.valueOf(annotation.strategy().toUpperCase(Locale.ROOT));
        int startKeep = annotation.startKeep() >= 0 ? annotation.startKeep() : strategy.defaultStartKeep();
        int endKeep = annotation.endKeep() >= 0 ? annotation.endKeep() : strategy.defaultEndKeep();
        String replacement = annotation.replacement().isEmpty() ? "*" : annotation.replacement();
        return new DesensitizeContext(startKeep, endKeep, replacement, annotation.skip());
    }

    /**
     * 对文本执行脱敏：遍历所有自定义规则（SPI + 手动注册），按上下文执行。
     *
     * @param text 原文，可为 {@code null}
     * @return 脱敏后文本；{@code null} 原样返回
     */
    public @Nullable String mask(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (DesensitizeRule rule : rules()) {
            result = rule.apply(result, DesensitizeContext.DEFAULT);
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
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
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
     * Map 值脱敏：对 Map 中所有字符串值按给定策略脱敏，返回新 Map（原 Map 不变）。
     *
     * @param source   源 Map，可为 {@code null}
     * @param strategy 策略名，对应 {@link DesensitizeStrategies}
     * @return 脱敏后的新 Map；{@code source} 为 {@code null} 时返回 {@code null}
     * @throws DesensitizeException 策略不存在
     */
    public @Nullable Map<String, String> maskMap(@Nullable Map<String, String> source, String strategy) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        Map<String, String> result = new java.util.LinkedHashMap<>(source.size());
        for (Map.Entry<String, String> entry : source.entrySet()) {
            result.put(entry.getKey(), Objects.requireNonNull(mask(entry.getValue(), strategy)));
        }
        return result;
    }

    /**
     * 手动注册自定义规则（线程安全）。
     *
     * @param rule 规则，不能为 {@code null}
     */
    public void register(DesensitizeRule rule) {
        if (!extraRules.contains(rule)) {
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

    private List<DesensitizeRule> rules() {
        List<DesensitizeRule> all = new ArrayList<>(extraRules);
        all.addAll(ExtensionLoader.getExtensionLoader(DesensitizeRule.class).getActivateExtensions());
        return all;
    }
}
