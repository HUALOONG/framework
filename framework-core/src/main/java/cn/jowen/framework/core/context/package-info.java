/**
 * 上下文传播：统一 {@link java.lang.ScopedValue}/{@link java.lang.ThreadLocal} 双模式上下文载体，
 * 支持上下文快照捕获、回放与线程池桥接传播。
 *
 * <p>使用方式：
 * <pre>{@code
 * // 定义上下文键
 * static final ContextKey<String> TENANT = ContextKey.named("tenantId", String.class);
 *
 * // 写入并执行（默认 ScopedValue 模式）
 * ContextCarrier.runWith(TENANT, "t-1001", () -> doBiz());
 *
 * // 读取
 * String tenantId = ContextCarrier.get(TENANT);
 *
 * // 传播到线程池：快照捕获 + 回放
 * executor.execute(ContextSnapshot.capture()::replay);
 * }</pre>
 */
@NullMarked
package cn.jowen.framework.core.context;

import org.jspecify.annotations.NullMarked;
