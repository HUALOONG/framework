package cn.jowen.framework.plugin.hotswap;

import org.jspecify.annotations.NullMarked;

/**
 * 插件热部署策略接口。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface HotSwapStrategy {

    /**
     * 当插件 jar 文件发生变化时回调。
     *
     * @param fileName 变化的 jar 文件名，不可为 {@code null}
     */
    void onPluginChange(String fileName);
}
