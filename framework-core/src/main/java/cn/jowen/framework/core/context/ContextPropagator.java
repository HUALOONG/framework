/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.core.context;

import cn.jowen.framework.core.spi.SPI;
import java.util.concurrent.Callable;
import org.jspecify.annotations.NullMarked;

/**
 * 上下文传播器 SPI：桥接异步执行（线程池/虚拟线程调度器），使任务提交时自动携带上下文快照。
 *
 * <p>实现示例（包装到线程池）：
 * <pre>{@code
 * @SPIImplementation
 * public class TracePropagator implements ContextPropagator {
 *     @Override
 *     public Runnable wrap(Runnable task) {
 *         ContextSnapshot snapshot = ContextSnapshot.capture();
 *         return () -> snapshot.replay(task);
 *     }
 * }
 * }</pre>
 *
 * <p>通过 {@link cn.jowen.framework.core.spi.ExtensionLoader} 发现与激活，业务方可自行注册
 * 全局传播器或使用 {@link ContextSnapshot} 手动传播。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@SPI
@FunctionalInterface
public interface ContextPropagator {

    /**
     * 包装任务，使其在其他线程执行时携带当前上下文。
     *
     * @param task 原始任务，不能为 {@code null}
     * @return 携带上下文的任务
     */
    Runnable wrap(Runnable task);

    /**
     * 包装可调用任务（默认实现委托快照回放，保留返回值与受检异常）。
     *
     * @param task 原始任务，不能为 {@code null}
     * @param <V>  返回值类型
     * @return 携带上下文的任务
     */
    default <V> Callable<V> wrap(Callable<V> task) {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        return () -> {
            Object[] holder = new Object[1];
            snapshot.replay(() -> {
                try {
                    holder[0] = task.call();
                } catch (Exception e) {
                    holder[0] = new WrappedExecutionException(e);
                }
            });
            if (holder[0] instanceof WrappedExecutionException we) {
                throw (Exception) we.getCause();
            }
            @SuppressWarnings("unchecked")
            V result = (V) holder[0];
            return result;
        };
    }

    /** 用于跨作用域传递受检异常的载体。 */
    final class WrappedExecutionException extends RuntimeException {
        WrappedExecutionException(Exception cause) {
            super(cause);
        }
    }
}
