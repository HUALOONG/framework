package cn.jowen.framework.plugin.hotswap;

import org.jspecify.annotations.NullMarked;

/**
 * 热部署策略枚举。
 *
 * @author 王飞
 */
@NullMarked
public enum HotSwapStrategy {

    /**
     * 默认策略：停止旧插件，重新加载新 jar。
     */
    RESTART,

    /**
     * 仅重新加载类（需要 JVM HotSwap 支持）。
     */
    RELOAD_CLASSES,

    /**
     * 手动策略：等待人工干预。
     */
    MANUAL
}
