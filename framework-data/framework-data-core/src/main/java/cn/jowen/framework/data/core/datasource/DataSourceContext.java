package cn.jowen.framework.data.core.datasource;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据源上下文，承载当前线程/虚拟线程的数据源名称。
 * 基于 {@link ContextCarrier}，虚拟线程切换时自动传播。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class DataSourceContext {

    static final ContextKey<String> KEY = ContextKey.named("framework.data.source", String.class);

    private DataSourceContext() {
    }

    /**
     * 获取当前数据源名称。
     *
     * @return 当前数据源名称；未设置时返回 {@code null}
     */
    public static @Nullable String getDataSourceKey() {
        return ContextCarrier.get(KEY);
    }

    /**
     * 设置当前数据源名称。
     *
     * @param key 数据源名称（不能为 {@code null}）
     */
    public static void setDataSourceKey(String key) {
        ContextCarrier.set(KEY, key);
    }

    /**
     * 在指定数据源的作用域内执行任务，任务结束后恢复原值。
     *
     * @param key  数据源名称
     * @param task 待执行任务
     */
    public static void runWith(String key, Runnable task) {
        ContextCarrier.runWith(KEY, key, task);
    }

    /**
     * 清除当前数据源名称。
     */
    public static void clear() {
        ContextCarrier.set(KEY, null);
    }
}