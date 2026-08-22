package cn.jowen.framework.logger.facade;

import cn.jowen.framework.core.spi.ExtensionLoader;
import cn.jowen.framework.logger.adapter.LoggerAdapter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 日志工厂，按名/类获取 {@link Logger}，底层实现经 {@link LoggerAdapter} SPI 解析（默认 Logback）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LoggerFactory {

    private static volatile @Nullable LoggerAdapter adapter;

    private LoggerFactory() {
    }

    /**
     * 按类获取日志器。
     *
     * @param clazz 类，不可为 {@code null}
     * @return 日志器，不可为 {@code null}
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * 按名称获取日志器。
     *
     * @param name 名称，不可为 {@code null}
     * @return 日志器，不可为 {@code null}
     */
    public static Logger getLogger(String name) {
        return resolveAdapter().getLogger(name);
    }

    /**
     * 解析并缓存当前底层适配器（供启动期预热或显式探测）。
     *
     * @return 适配器，不可为 {@code null}
     */
    public static LoggerAdapter resolveAdapter() {
        LoggerAdapter current = adapter;
        if (current != null) {
            return current;
        }
        synchronized (LoggerFactory.class) {
            if (adapter != null) {
                return adapter;
            }
            List<LoggerAdapter> adapters = ExtensionLoader.getExtensionLoader(LoggerAdapter.class)
                    .getActivateExtensions();
            if (adapters.isEmpty()) {
                throw new IllegalStateException("未找到可用的 LoggerAdapter 实现，请引入 logback/log4j2 适配");
            }
            adapter = adapters.get(0);
            return adapter;
        }
    }

    /**
     * 显式设置适配器（用于测试或默认实现切换）。
     *
     * @param adapter 适配器，不可为 {@code null}
     */
    public static void setAdapter(LoggerAdapter adapter) {
        LoggerFactory.adapter = adapter;
    }
}
