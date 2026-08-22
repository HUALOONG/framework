package cn.jowen.framework.i18n.reload;

import org.jspecify.annotations.NullMarked;

/**
 * 资源变更监听器。负责感知底层资源（文件/数据库/Redis）变更并触发重载。
 *
 * <p>典型生命周期：{@code start()} 后进入监听状态，{@code stop()} 释放资源并停止监听。
 * 各实现（文件 WatchService、DB 轮询、Redis 订阅）应保证 {@code start()/stop()} 幂等。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface ResourceWatcher {

    /** 启动监听（幂等：已在运行则直接返回）。 */
    void start();

    /** 停止监听并释放资源（幂等：未在运行则直接返回）。 */
    void stop();

    /** 是否处于监听状态。 */
    boolean isRunning();
}
