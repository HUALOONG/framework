package cn.jowen.framework.core.lifecycle;

import org.jspecify.annotations.NullMarked;

/**
 * 智能生命周期：在 {@link Lifecycle} 初始化/销毁语义之上，补充显式的启动/停止状态机与阶段排序。
 *
 * <p>设计要点：
 * <ul>
 *   <li>{@link #start()} / {@link #stop()}：运行时启停（区别于初始化 {@link #afterPropertiesSet()}）；</li>
 *   <li>{@link #getPhase()}：阶段值，由 {@link LifecycleProcessor} 按升序启动、降序停止（值小者先启动）；</li>
 *   <li>{@link #isAutoStartup()}：是否随容器自动启动（默认 true，可关闭以手动控制）；</li>
 *   <li>{@link #stop(Runnable)}：异步停止完成回调（默认同步执行 {@link #stop()} 后回调）。</li>
 * </ul>
 *
 * <p>兼容性说明：本接口继承 {@link Lifecycle}（组合 {@code InitializingBean}/{@code DisposableBean}），
 * 不破坏既有实现方的初始化/销毁契约。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface SmartLifecycle extends Lifecycle {

    /**
     * 默认阶段值：最先启动的组件通常希望尽早就绪。
     */
    int DEFAULT_PHASE = 0;

    /**
     * 启动组件（幂等：已在运行则直接返回）。
     */
    void start();

    /**
     * 停止组件（幂等：未在运行则直接返回）。
     */
    void stop();

    /**
     * 是否处于运行中。
     */
    boolean isRunning();

    /**
     * 阶段值，值越小越先启动、越后停止。
     *
     * @return 阶段值，默认 {@value #DEFAULT_PHASE}
     */
    default int getPhase() {
        return DEFAULT_PHASE;
    }

    /**
     * 是否随容器自动启动。
     *
     * @return 默认 {@code true}
     */
    default boolean isAutoStartup() {
        return true;
    }

    /**
     * 停止并异步通知完成（默认同步执行 {@link #stop()} 后立即回调）。
     *
     * @param callback 停止完成回调，不能为 {@code null}
     */
    default void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
