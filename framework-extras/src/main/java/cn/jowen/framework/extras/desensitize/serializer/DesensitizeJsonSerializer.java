package cn.jowen.framework.extras.desensitize.serializer;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.desensitize.DesensitizeContext;
import cn.jowen.framework.core.desensitize.DesensitizeField;
import cn.jowen.framework.core.desensitize.DesensitizeStrategies;
import cn.jowen.framework.core.desensitize.Desensitizer;
import cn.jowen.framework.extras.desensitize.DesensitizeSkipContextKey;
import cn.jowen.framework.extras.desensitize.annotation.DesensitizeMeta;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.introspect.AnnotatedMember;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 脱敏 JSON 序列化器：在序列化阶段依据字段上的脱敏注解（core 的 {@link DesensitizeField}
 * 或本模块便捷注解元标注的 {@link DesensitizeMeta}）调用核心 {@link Desensitizer} 进行脱敏；
 * 无脱敏注解的字符串字段原样输出，保持透明。
 *
 * <p>跳过策略（优先级由高到低）：
 * <ol>
 *   <li>字段级 {@code skip} 为 {@code true}（如 {@code @PhoneDesensitize(skip = true)}）；</li>
 *   <li>全局开关 {@link DesensitizeSkipContextKey#DESENSITIZE_SKIP} 为 {@code true}（管理员免脱敏）。</li>
 * </ol>
 *
 * <p>与 Jackson 集成：由 {@link DesensitizeModule} 注册到所有 {@code String} 类型，
 * 字段标注脱敏注解时被 {@link #createContextual} 识别并绑定脱敏元数据。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class DesensitizeJsonSerializer extends ValueSerializer<String> {

    private final @Nullable Resolved resolved;

    /**
     * 透传构造（无字段注解时原样输出）。
     */
    public DesensitizeJsonSerializer() {
        this(null);
    }

    /**
     * 绑定字段脱敏元数据的构造。
     *
     * @param resolved 解析后的脱敏元数据；{@code null} 表示透传
     */
    public DesensitizeJsonSerializer(@Nullable Resolved resolved) {
        this.resolved = resolved;
    }

    /**
     * 从 {@link BeanProperty} 反向定位字段，解析脱敏注解元数据。
     *
     * <p>优先识别 core 的 {@link DesensitizeField}（直接标注）；
     * 否则遍历字段上的便捷注解，按其元标注 {@link DesensitizeMeta} 的常量 strategy 与
     * 实例级别成员（skip/startKeep/endKeep/replacement）构建元数据。
     *
     * @param property 当前属性
     * @return 解析结果；未标注时返回 {@code null}
     */
    private static @Nullable Resolved resolveFieldAnnotation(BeanProperty property) {
        AnnotatedMember annotatedMember = property.getMember();
        if (annotatedMember == null) {
            return null;
        }
        Member raw = annotatedMember.getMember();
        if (!(raw instanceof Field field)) {
            return null;
        }
        // 1) 字段直接标注 core 的 @DesensitizeField
        DesensitizeField direct = field.getAnnotation(DesensitizeField.class);
        if (direct != null) {
            return fromCore(direct);
        }
        // 2) 便捷注解（元标注 @DesensitizeMeta）：在类型层读取常量 strategy，在实例层读取其它成员
        for (Annotation present : field.getAnnotations()) {
            DesensitizeMeta metaType = present.annotationType().getAnnotation(DesensitizeMeta.class);
            if (metaType == null) {
                continue;
            }
            String strategy = metaType.strategy();
            boolean skip = readMember(present, "skip", metaType.skip(), Boolean.class);
            int startKeep = readMember(present, "startKeep", metaType.startKeep(), Integer.class);
            int endKeep = readMember(present, "endKeep", metaType.endKeep(), Integer.class);
            String replacement = readMember(present, "replacement", metaType.replacement(), String.class);
            return buildResolved(strategy, skip, startKeep, endKeep, replacement);
        }
        return null;
    }

    private static Resolved fromCore(DesensitizeField annotation) {
        DesensitizeStrategies strategyEnum = DesensitizeStrategies.valueOf(annotation.strategy().toUpperCase(Locale.ROOT));
        int startKeep = annotation.startKeep() >= 0 ? annotation.startKeep() : strategyEnum.defaultStartKeep();
        int endKeep = annotation.endKeep() >= 0 ? annotation.endKeep() : strategyEnum.defaultEndKeep();
        String replacement = annotation.replacement().isEmpty() ? "*" : annotation.replacement();
        DesensitizeContext ctx = new DesensitizeContext(startKeep, endKeep, replacement, annotation.skip());
        return new Resolved(annotation.strategy(), ctx);
    }

    private static Resolved buildResolved(String strategy, boolean skip,
                                          int startKeep, int endKeep, String replacement) {
        DesensitizeStrategies strategyEnum =
                DesensitizeStrategies.valueOf(strategy.toUpperCase(Locale.ROOT));
        int sk = startKeep >= 0 ? startKeep : strategyEnum.defaultStartKeep();
        int ek = endKeep >= 0 ? endKeep : strategyEnum.defaultEndKeep();
        String rep = (replacement == null || replacement.isEmpty()) ? "*" : replacement;
        DesensitizeContext ctx = new DesensitizeContext(sk, ek, rep, skip);
        return new Resolved(strategy, ctx);
    }

    /**
     * 反射读取便捷注解实例的成员值（成员不存在时回退到元注解默认值）。
     *
     * @param present  便捷注解实例
     * @param name     成员名
     * @param fallback 元注解默认值
     * @param type     成员类型（装箱类）
     * @param <T>      成员类型
     * @return 实例值或默认值
     */
    private static <T> T readMember(Annotation present, String name, T fallback, Class<T> type) {
        try {
            Method method = present.annotationType().getMethod(name);
            Object value = method.invoke(present);
            return type.cast(value);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return fallback;
        }
    }

    @Override
    public ValueSerializer<String> createContextual(SerializationContext ctxt, BeanProperty property) {
        if (property == null) {
            return this;
        }
        return new DesensitizeJsonSerializer(resolveFieldAnnotation(property));
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext ctxt) {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (resolved == null) {
            gen.writeString(value);
            return;
        }
        // 全局跳过（管理员免脱敏）
        Boolean globalSkip = ContextCarrier.get(DesensitizeSkipContextKey.DESENSITIZE_SKIP);
        if (Boolean.TRUE.equals(globalSkip) || resolved.ctx().skip()) {
            gen.writeString(value);
            return;
        }
        String masked = Desensitizer.getInstance().mask(value, resolved.strategy(), resolved.ctx());
        gen.writeString(masked);
    }

    /**
     * 已解析的字段脱敏元数据（strategy + 上下文）。
     */
    private record Resolved(String strategy, DesensitizeContext ctx) {
    }
}
