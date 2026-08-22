/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.core.context;

import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 上下文快照：捕获某一时刻的全部上下文条目（不可变），供跨线程/异步回放。
 *
 * <p>典型用法：
 * <pre>{@code
 * // 主线程捕获
 * ContextSnapshot snapshot = ContextCarrier.snapshot();
 *
 * // 提交到线程池（每任务捕获则每任务回放）
 * executor.execute(() -> snapshot.replay(() -> doAsync()));
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class ContextSnapshot {

    private final Map<ContextKey<?>, Object> values;

    /** 由 {@link ContextCarrier#snapshot()} 构造（包内可见），外部不可直接实例化。 */
    ContextSnapshot(Map<ContextKey<?>, Object> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * 捕获当前线程/作用域的上下文快照。
     *
     * @return 快照，永不为 {@code null}
     */
    public static ContextSnapshot capture() {
        return ContextCarrier.snapshot();
    }

    /**
     * 快照条目（不可变视图）。
     *
     * @return 条目集合，永不为 {@code null}
     */
    public Map<ContextKey<?>, Object> values() {
        return values;
    }

    /** 快照是否为空。 */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * 在快照作用域内执行任务（当前线程）。
     *
     * @param task 待执行任务，不能为 {@code null}
     */
    public void replay(Runnable task) {
        ContextCarrier.replay(this, task);
    }

    /**
     * 在快照基础上追加单个键值后执行任务。
     *
     * @param key   追加的键
     * @param value 追加的值（null 表示移除该键）
     * @param task  待执行任务
     * @param <T>   值类型
     */
    public <T> void scopedRun(ContextKey<T> key, @Nullable T value, Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        Map<ContextKey<?>, Object> merged = new java.util.HashMap<>(values);
        if (value == null) {
            merged.remove(key);
        } else {
            merged.put(key, value);
        }
        ContextCarrier.replay(new ContextSnapshot(merged), task);
    }
}
