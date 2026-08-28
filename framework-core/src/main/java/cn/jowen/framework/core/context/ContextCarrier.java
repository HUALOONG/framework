package cn.jowen.framework.core.context;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 上下文载体：统一 {@link java.lang.ScopedValue}（JDK 22+ 正式 API）与
 * {@link java.lang.ThreadLocal}（兼容模式）双模式的上下文读写与作用域执行。
 *
 * <p>模式说明：
 * <ul>
 *   <li>{@link Mode#SCOPED_VALUE}（默认）：基于结构化并发的值绑定，子任务不自动继承，
 *       仅通过 {@link #runWith} 显式绑定，避免上下文跨线程泄漏；
 *       在 ScopedValue 不可用的 JVM（如未启用预览的 JDK 21）上自动降级为 ThreadLocal；</li>
 *   <li>{@link Mode#THREAD_LOCAL}：兼容传统线程局部存储，适用于无法改造的存量调用链。</li>
 * </ul>
 *
 * <p>模式由启动层注入（如 autoconfigure 依据 {@code framework.context.mode} 配置调用
 * {@link #configure(Mode)}），本类不直接读取配置文件。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ContextCarrier {

    /**
     * ScopedValue 实例句柄（通过反射创建，不可用时为 null）。
     */
    private static final @Nullable Object SCOPED_VALUE_INSTANCE = ScopedValueBridge.newInstance();
    private static final ThreadLocal<@Nullable Map<ContextKey<?>, Object>> THREAD_LOCAL_VALUES = new ThreadLocal<>();
    private static volatile Mode mode = Mode.SCOPED_VALUE;

    private ContextCarrier() {
    }

    /**
     * 切换上下文模式（仅应在启动阶段调用一次）。
     *
     * @param newMode 目标模式，不能为 {@code null}
     */
    public static void configure(Mode newMode) {
        mode = Objects.requireNonNull(newMode, "mode must not be null");
    }

    /**
     * 当前配置的模式。
     *
     * @return 配置模式；注意 {@link Mode#SCOPED_VALUE} 在 ScopedValue 不可用环境会实际降级
     */
    public static Mode mode() {
        return mode;
    }

    /**
     * 读取上下文值。
     *
     * @param key 键，不能为 {@code null}
     * @param <T> 值类型
     * @return 当前作用域/线程中的值；未设置或类型不匹配时返回 {@code null}
     */
    public static <T> @Nullable T get(ContextKey<T> key) {
        Map<ContextKey<?>, Object> values = boundValues();
        if (values == null) {
            return null;
        }
        return key.cast(values.get(key));
    }

    /**
     * 写入上下文值（仅在作用域内有效；值传 null 表示移除该键）。
     *
     * @param key   键
     * @param value 值（null 表示移除）
     * @param <T>   值类型
     * @throws IllegalStateException ScopedValue 生效模式下在 {@link #runWith} 作用域之外调用
     */
    public static <T> void set(ContextKey<T> key, @Nullable T value) {
        Objects.requireNonNull(key, "key must not be null");
        if (useScopedValue()) {
            Map<ContextKey<?>, Object> values = boundValues();
            if (values == null) {
                throw new IllegalStateException("ScopedValue 模式下 set() 必须在 runWith()/replay() 作用域内调用");
            }
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
            return;
        }
        Map<ContextKey<?>, Object> values = THREAD_LOCAL_VALUES.get();
        if (values == null) {
            values = new HashMap<>();
            THREAD_LOCAL_VALUES.set(values);
        }
        if (value == null) {
            values.remove(key);
        } else {
            values.put(key, value);
        }
    }

    /**
     * 在绑定新值的作用域内执行任务（值传 null 表示在作用域内移除该键）。
     *
     * <p>ScopedValue 模式下任务结束即恢复外部值，子线程不自动继承；
     * ThreadLocal 模式下执行后还原旧值。
     *
     * @param key   键
     * @param value 绑定的值（null 表示移除）
     * @param task  待执行任务
     * @param <T>   值类型
     */
    public static <T> void runWith(ContextKey<T> key, @Nullable T value, Runnable task) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(task, "task must not be null");
        Map<ContextKey<?>, Object> next = nextValues(key, value);
        executeScoped(next, task);
    }

    /**
     * 捕获当前上下文快照（不可变，可跨线程回放）。
     *
     * @return 快照，永不为 {@code null}
     */
    public static ContextSnapshot snapshot() {
        Map<ContextKey<?>, Object> values = boundValues();
        return new ContextSnapshot(values == null ? Map.of() : Map.copyOf(values));
    }

    /**
     * 在给定快照的作用域内执行任务（语义同 {@link #runWith} 但一次性绑定全部条目）。
     *
     * @param snapshot 快照，不能为 {@code null}
     * @param task     待执行任务
     */
    public static void replay(ContextSnapshot snapshot, Runnable task) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(task, "task must not be null");
        executeScoped(new HashMap<>(snapshot.values()), task);
    }

    /**
     * 当前生效上下文条目（未绑定返回 null）。
     */
    static @Nullable Map<ContextKey<?>, Object> boundValues() {
        if (useScopedValue()) {
            if (SCOPED_VALUE_INSTANCE != null && !ScopedValueBridge.isBound(SCOPED_VALUE_INSTANCE)) {
                return null;
            }
            return (Map<ContextKey<?>, Object>) ScopedValueBridge.get(SCOPED_VALUE_INSTANCE);
        }
        return THREAD_LOCAL_VALUES.get();
    }

    private static boolean useScopedValue() {
        return mode == Mode.SCOPED_VALUE && ScopedValueBridge.available() && SCOPED_VALUE_INSTANCE != null;
    }

    private static <T> Map<ContextKey<?>, Object> nextValues(ContextKey<T> key, @Nullable T value) {
        Map<ContextKey<?>, Object> base = boundValues();
        Map<ContextKey<?>, Object> next = base == null ? new HashMap<>() : new HashMap<>(base);
        if (value == null) {
            next.remove(key);
        } else {
            next.put(key, value);
        }
        return next;
    }

    private static void executeScoped(Map<ContextKey<?>, Object> next, Runnable task) {
        if (SCOPED_VALUE_INSTANCE != null && useScopedValue()) {
            // 直接绑定可变 Map：作用域内 set() 可修改，作用域结束随 ScopedValue 一并丢弃
            ScopedValueBridge.run(SCOPED_VALUE_INSTANCE, next, task);
            return;
        }
        Map<ContextKey<?>, Object> previous = THREAD_LOCAL_VALUES.get();
        THREAD_LOCAL_VALUES.set(next);
        try {
            task.run();
        } finally {
            if (previous == null) {
                THREAD_LOCAL_VALUES.remove();
            } else {
                THREAD_LOCAL_VALUES.set(previous);
            }
        }
    }

    /**
     * 上下文模式。
     */
    public enum Mode {
        /**
         * 结构化并发（ScopedValue，JDK 22+ 正式），默认。
         */
        SCOPED_VALUE,
        /**
         * 传统线程局部存储，兼容模式。
         */
        THREAD_LOCAL
    }
}
