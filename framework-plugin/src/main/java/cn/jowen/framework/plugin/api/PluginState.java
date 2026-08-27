package cn.jowen.framework.plugin.api;

import org.jspecify.annotations.NullMarked;

/**
 * 插件生命周期状态枚举。
 *
 * <p>状态机：
 * <pre>
 * CREATED ──start──▶ STARTING ──▶ STARTED
 *                           │         │
 *                           ▼         ▼ stop
 *                        FAILED ◀── STOPPING ──▶ STOPPED
 *                           │                      │
 *                           └────retry─────────────┘
 * </pre>
 *
 * @author 王飞
 */
@NullMarked
public enum PluginState {
    /**
     * 插件已创建但尚未启动。
     */
    CREATED,

    /**
     * 插件正在启动中（异步）。
     */
    STARTING,

    /**
     * 插件已完全启动，可以对外提供服务。
     */
    STARTED,

    /**
     * 插件正在停止中（异步）。
     */
    STOPPING,

    /**
     * 插件已停止，资源已释放。
     */
    STOPPED,

    /**
     * 插件启动或停止过程中发生异常。
     */
    FAILED,

    /**
     * 插件已被手动禁用。
     */
    DISABLED;

    /**
     * 检查当前状态是否可以安全转换到 {@code nextState}。
     *
     * @param nextState 目标状态，不可为 {@code null}
     * @return {@code true} 当且仅当转换合法
     */
    public boolean canTransitTo(PluginState nextState) {
        if (nextState == null) return false;
        return switch (this) {
            case CREATED, STOPPED -> nextState == STARTING || nextState == DISABLED;
            case STARTING -> nextState == STARTED || nextState == FAILED || nextState == DISABLED;
            case STARTED -> nextState == STOPPING || nextState == DISABLED;
            case STOPPING -> nextState == STOPPED || nextState == FAILED || nextState == DISABLED;
            case FAILED, DISABLED -> nextState == CREATED;
        };
    }

    /**
     * 判断当前状态是否为运行中状态。
     *
     * @return {@code true} 表示插件正在运行
     */
    public boolean isRunning() {
        return this == STARTED;
    }

    /**
     * 判断当前状态是否为终态。
     *
     * @return {@code true} 表示处于不可恢复的终止状态
     */
    public boolean isTerminal() {
        return this == STOPPED || this == FAILED;
    }
}
